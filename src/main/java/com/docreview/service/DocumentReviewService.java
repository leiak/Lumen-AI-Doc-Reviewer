package com.docreview.service;

import com.docreview.config.DocReviewProperties;
import com.docreview.config.ReviewTemplate;
import com.docreview.config.ReviewTemplateLoader;
import com.docreview.config.ReviewerDefinition;
import com.docreview.parser.DocumentParserFactory;
import com.docreview.persistence.entity.AuditLogEntity;
import com.docreview.persistence.entity.DocumentSegmentEntity;
import com.docreview.persistence.entity.DocumentSessionEntity;
import com.docreview.persistence.entity.DocFindingEntity;
import com.docreview.persistence.repository.DocumentSegmentRepository;
import com.docreview.persistence.repository.DocumentSessionRepository;
import com.docreview.persistence.repository.DocFindingRepository;
import com.docreview.report.ReportRenderer;
import com.docreview.reviewer.AiReviewClient;
import com.docreview.reviewer.FindingDraft;
import com.docreview.reviewer.MockReviewClient;
import com.docreview.reviewer.OpenAiCompatibleReviewClient;
import com.docreview.reviewer.AiRuleGenerator;
import com.docreview.review.FindingAggregator;
import com.docreview.review.DocumentDiffCalculator;
import com.docreview.review.SafetyScoreCalculator;
import com.docreview.segment.DocumentSegmenter;
import com.docreview.segment.Segment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentReviewService {
    private final DocumentSessionRepository sessionRepository;
    private final DocumentSegmentRepository segmentRepository;
    private final DocFindingRepository findingRepository;
    private final DocumentParserFactory parserFactory;
    private final DocumentSegmenter segmenter;
    private final ReviewTemplateLoader templateLoader;
    private final MockReviewClient mockReviewClient;
    private final OpenAiCompatibleReviewClient openAiReviewClient;
    private final AiRuleGenerator aiRuleGenerator;
    private final FindingAggregator findingAggregator;
    private final DocumentDiffCalculator diffCalculator;
    private final SafetyScoreCalculator scoreCalculator;
    private final AuditService auditService;
    private final DocReviewProperties properties;
    private final ExecutorService reviewExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public DocumentReviewService(DocumentSessionRepository sessionRepository,
                                 DocumentSegmentRepository segmentRepository,
                                 DocFindingRepository findingRepository,
                                 DocumentParserFactory parserFactory,
                                 DocumentSegmenter segmenter,
                                 ReviewTemplateLoader templateLoader,
                                 MockReviewClient mockReviewClient,
                                 OpenAiCompatibleReviewClient openAiReviewClient,
                                 AiRuleGenerator aiRuleGenerator,
                                 FindingAggregator findingAggregator,
                                 DocumentDiffCalculator diffCalculator,
                                 SafetyScoreCalculator scoreCalculator,
                                 AuditService auditService,
                                 DocReviewProperties properties) {
        this.sessionRepository = sessionRepository;
        this.segmentRepository = segmentRepository;
        this.findingRepository = findingRepository;
        this.parserFactory = parserFactory;
        this.segmenter = segmenter;
        this.templateLoader = templateLoader;
        this.mockReviewClient = mockReviewClient;
        this.openAiReviewClient = openAiReviewClient;
        this.aiRuleGenerator = aiRuleGenerator;
        this.findingAggregator = findingAggregator;
        this.diffCalculator = diffCalculator;
        this.scoreCalculator = scoreCalculator;
        this.auditService = auditService;
        this.properties = properties;
    }

    public List<String> templates() {
        return templateLoader.available();
    }

    public Map<String, String> mode() {
        return Map.of(
                "mode", properties.getAi().isMock() ? "mock" : "real",
                "model", properties.getAi().getDefaultModel()
        );
    }

    public ReviewTemplate template(String name) {
        return templateLoader.load(name);
    }

    public ReviewTemplate saveTemplate(String name, ReviewTemplate template) {
        templateLoader.save(name, template);
        return templateLoader.load(name);
    }

    public ReviewTemplate resetTemplate(String name) {
        templateLoader.reset(name);
        return templateLoader.load(name);
    }

    public ReviewTemplate generateTemplateRules(String name, String businessContext, Integer ruleCount) {
        ReviewTemplate template = templateLoader.load(name);
        List<ReviewerDefinition> reviewers = aiRuleGenerator.generate(
                template.getDocumentType(), businessContext, ruleCount == null ? 5 : ruleCount);
        ReviewTemplate generated = new ReviewTemplate();
        generated.setName(template.getName());
        generated.setVersion(template.getVersion());
        generated.setDocumentType(template.getDocumentType());
        generated.setChunkBy(template.getChunkBy());
        generated.setMaxChunkTokens(template.getMaxChunkTokens());
        generated.setReviewers(reviewers);
        return generated;
    }

    public Map<String, Object> upload(MultipartFile file, String templateName, String previousSessionId) {
        ReviewTemplate template = templateLoader.load(templateName);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        try {
            Path uploadDir = Path.of(properties.getUploadDir());
            Files.createDirectories(uploadDir);
            String safeName = UUID.randomUUID() + "-" + originalName.replaceAll("[^\\p{L}\\p{N}._-]+", "_");
            Path destination = uploadDir.resolve(safeName).normalize();
            if (!destination.startsWith(uploadDir.normalize())) {
                throw new IllegalArgumentException("Invalid upload path");
            }
            file.transferTo(destination);

            DocumentSessionEntity session = new DocumentSessionEntity();
            session.setDocName(originalName);
            session.setDocType(template.getDocumentType());
            session.setFilePath(destination.toString());
            session.setStatus("reviewing");
            if (previousSessionId != null && !previousSessionId.isBlank()) {
                DocumentSessionEntity previous = sessionRepository.findById(previousSessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Previous session not found: " + previousSessionId));
                session.setParentSessionId(previous.getId());
                session.setVersion(previous.getVersion() + 1);
                session.setIncremental(true);
            }
            session = sessionRepository.save(session);
            String sessionId = session.getId();
            auditService.record(sessionId, "upload", "Uploaded document: " + originalName);
            reviewExecutor.submit(() -> runReview(sessionId, templateName));
            return Map.of("id", sessionId, "status", "reviewing");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store uploaded document", exception);
        }
    }

    private void runReview(String sessionId, String templateName) {
        try {
            DocumentSessionEntity session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            ReviewTemplate template = templateLoader.load(templateName);
            String text = parserFactory.parse(session.getDocName(), Files.newInputStream(Path.of(session.getFilePath())));
            List<Segment> parsedSegments = segmenter.segment(text, template.getChunkBy(), template.getMaxChunkTokens());
            if (parsedSegments.isEmpty()) {
                throw new IllegalStateException("No readable text was extracted from the document");
            }

            List<DocumentSegmentEntity> segmentEntities = new ArrayList<>();
            List<FindingDraft> drafts = new ArrayList<>();
            List<DocumentDiffCalculator.SegmentDiff> segmentDiffs = session.isIncremental()
                    ? diffCalculator.diff(previousSegments(session.getParentSessionId()), parsedSegments, 0.9)
                    : parsedSegments.stream().map(segment -> new DocumentDiffCalculator.SegmentDiff(segment, true)).toList();
            for (DocumentDiffCalculator.SegmentDiff segmentDiff : segmentDiffs) {
                Segment parsedSegment = segmentDiff.segment();
                DocumentSegmentEntity entity = new DocumentSegmentEntity();
                entity.setSessionId(sessionId);
                entity.setSectionPath(parsedSegment.sectionPath());
                entity.setContent(parsedSegment.content());
                entity.setTokenCount(parsedSegment.tokenCount());
                entity.setOrderIndex(parsedSegment.orderIndex());
                entity.setChanged(segmentDiff.changed());
                entity = segmentRepository.save(entity);
                segmentEntities.add(entity);

                if (!entity.isChanged()) {
                    continue;
                }

                for (ReviewerDefinition reviewer : template.getReviewers()) {
                    AiReviewClient client = properties.getAi().isMock() ? mockReviewClient : openAiReviewClient;
                    if (!properties.getAi().isMock() && properties.getAi().getApiKey().isBlank()) {
                        throw new IllegalStateException("DOC_REVIEW_AI_API_KEY is required when mock mode is disabled");
                    }
                    Segment segmentWithEntityId = new Segment(parsedSegment.orderIndex(), parsedSegment.sectionPath(),
                            parsedSegment.content(), parsedSegment.tokenCount());
                    List<FindingDraft> reviewerDrafts = CompletableFuture.supplyAsync(
                            () -> client.review(reviewer, segmentWithEntityId, template.getDocumentType()),
                            reviewExecutor
                    ).join();
                    for (FindingDraft draft : reviewerDrafts) {
                        drafts.add(new FindingDraft(entity.getId(), draft.reviewerRole(), draft.severity(),
                                draft.riskType(), draft.description(), draft.suggestedRevision()));
                    }
                }
            }

            List<FindingDraft> aggregated = findingAggregator.aggregate(drafts, properties.getDuplicateThreshold());
            List<DocFindingEntity> findingEntities = new ArrayList<>();
            for (FindingDraft draft : aggregated) {
                DocFindingEntity finding = new DocFindingEntity();
                finding.setSessionId(sessionId);
                finding.setSegmentId(draft.segmentId());
                finding.setReviewerRole(draft.reviewerRole());
                finding.setSeverity(draft.severity());
                finding.setRiskType(draft.riskType());
                finding.setDescription(draft.description());
                finding.setSuggestedRevision(draft.suggestedRevision());
                findingEntities.add(findingRepository.save(finding));
            }
            session.setRiskScore(scoreCalculator.calculate(findingEntities));
            session.setStatus("done");
            session.setChangedSegmentCount((int) segmentEntities.stream().filter(DocumentSegmentEntity::isChanged).count());
            session.setUnchangedSegmentCount(segmentEntities.size() - session.getChangedSegmentCount());
            sessionRepository.save(session);
            auditService.record(sessionId, "review_done", "Review completed with " + aggregated.size() + " findings");
        } catch (Exception exception) {
            failSession(sessionId, exception);
        }
    }

    private void failSession(String sessionId, Exception exception) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus("failed");
            session.setErrorMessage(exception.getMessage());
            sessionRepository.save(session);
            auditService.record(sessionId, "review_failed", exception.getMessage());
        });
    }

    public Map<String, Object> status(String sessionId) {
        DocumentSessionEntity session = requireSession(sessionId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("id", session.getId());
        status.put("docName", session.getDocName());
        status.put("status", session.getStatus());
        status.put("riskScore", session.getRiskScore());
        status.put("segmentCount", segmentRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).size());
        status.put("findingCount", findingRepository.findBySessionIdOrderBySeverityAscIdAsc(sessionId).size());
        status.put("error", session.getErrorMessage());
        status.put("version", session.getVersion());
        status.put("incremental", session.isIncremental());
        status.put("changedSegmentCount", session.getChangedSegmentCount());
        status.put("unchangedSegmentCount", session.getUnchangedSegmentCount());
        return status;
    }

    public List<Map<String, Object>> sessions() {
        return sessionRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(session -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", session.getId());
                    item.put("docName", session.getDocName());
                    item.put("status", session.getStatus());
                    item.put("version", session.getVersion());
                    item.put("incremental", session.isIncremental());
                    item.put("riskScore", session.getRiskScore());
                    item.put("createdAt", session.getCreatedAt());
                    return item;
                })
                .toList();
    }

    private List<Segment> previousSegments(String sessionId) {
        return segmentRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(entity -> new Segment(entity.getOrderIndex(), entity.getSectionPath(), entity.getContent(),
                        entity.getTokenCount()))
                .toList();
    }

    public List<DocumentSegmentEntity> segments(String sessionId) {
        requireSession(sessionId);
        return segmentRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
    }

    public List<DocFindingEntity> findings(String sessionId) {
        requireSession(sessionId);
        return findingRepository.findBySessionIdOrderBySeverityAscIdAsc(sessionId);
    }

    public List<AuditLogEntity> audit(String sessionId) {
        return auditService.findBySession(sessionId);
    }

    public DocFindingEntity feedback(String sessionId, Long findingId, String action,
                                     String description, String suggestedRevision) {
        if (!List.of("accepted", "rejected", "modified").contains(action)) {
            throw new IllegalArgumentException("Action must be accepted, rejected or modified");
        }
        DocFindingEntity finding = findingRepository.findById(findingId)
                .filter(item -> item.getSessionId().equals(sessionId))
                .orElseThrow(() -> new IllegalArgumentException("Finding not found"));
        finding.setUserAction(action);
        if (description != null && !description.isBlank()) {
            finding.setDescription(description);
        }
        if (suggestedRevision != null && !suggestedRevision.isBlank()) {
            finding.setSuggestedRevision(suggestedRevision);
        }
        DocFindingEntity savedFinding = findingRepository.save(finding);
        auditService.record(sessionId, "feedback", "Finding " + findingId + " marked as " + action);
        return savedFinding;
    }

    @Transactional
    public void delete(String sessionId) {
        DocumentSessionEntity session = requireSession(sessionId);
        if (sessionRepository.existsByParentSessionId(sessionId)) {
            throw new IllegalStateException("Cannot delete this session because a later version references it");
        }

        segmentRepository.deleteBySessionId(sessionId);
        findingRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
        try {
            Files.deleteIfExists(Path.of(session.getFilePath()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete uploaded document", exception);
        }
        auditService.record(sessionId, "session_delete", "Deleted session: " + session.getDocName());
    }

    public String report(String sessionId, String format) {
        DocumentSessionEntity session = requireSession(sessionId);
        List<DocumentSegmentEntity> segments = segmentRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
        List<DocFindingEntity> findings = findingRepository.findBySessionIdOrderBySeverityAscIdAsc(sessionId);
        String requestedFormat = format == null ? "markdown" : format.toLowerCase();
        auditService.record(sessionId, "report_access", "Requested report format: " + requestedFormat);
        return switch (requestedFormat) {
            case "markdown", "md" -> renderMarkdown(session, segments, findings);
            case "html" -> renderHtml(session, segments, findings);
            default -> throw new IllegalArgumentException("Format must be markdown or html");
        };
    }

    private DocumentSessionEntity requireSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private String renderMarkdown(DocumentSessionEntity session,
                                  List<DocumentSegmentEntity> segments,
                                  List<DocFindingEntity> findings) {
        ReportRenderer renderer = new ReportRenderer();
        return renderer.renderMarkdown(session, segments, findings);
    }

    private String renderHtml(DocumentSessionEntity session,
                              List<DocumentSegmentEntity> segments,
                              List<DocFindingEntity> findings) {
        ReportRenderer renderer = new ReportRenderer();
        return renderer.renderHtml(session, segments, findings);
    }
}
