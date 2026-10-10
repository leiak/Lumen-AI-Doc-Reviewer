package com.docreview.web;

import com.docreview.persistence.entity.DocumentSegmentEntity;
import com.docreview.persistence.entity.DocFindingEntity;
import com.docreview.persistence.entity.AuditLogEntity;
import com.docreview.config.ReviewTemplate;
import com.docreview.service.DocumentReviewService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doc")
public class DocumentController {
    private final DocumentReviewService service;

    public DocumentController(DocumentReviewService service) {
        this.service = service;
    }

    @GetMapping("/templates")
    public List<String> templates() {
        return service.templates();
    }

    @GetMapping("/mode")
    public Map<String, String> mode() {
        return service.mode();
    }

    @GetMapping("/templates/{name}")
    public ReviewTemplate template(@PathVariable String name) {
        return service.template(name);
    }

    @PutMapping("/templates/{name}")
    public ReviewTemplate saveTemplate(@PathVariable String name, @RequestBody ReviewTemplate template) {
        return service.saveTemplate(name, template);
    }

    @DeleteMapping("/templates/{name}/override")
    public ReviewTemplate resetTemplate(@PathVariable String name) {
        return service.resetTemplate(name);
    }

    @PostMapping("/templates/{name}/generate-rules")
    public ReviewTemplate generateRules(@PathVariable String name,
                                        @RequestBody RuleGenerationRequest request) {
        return service.generateTemplateRules(name, request.businessContext(), request.ruleCount());
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("template") String template,
                                      @RequestParam(value = "previousSessionId", required = false) String previousSessionId) {
        return service.upload(file, template, previousSessionId);
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions() {
        return service.sessions();
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable String id) {
        return service.status(id);
    }

    @GetMapping("/{id}/segments")
    public List<DocumentSegmentEntity> segments(@PathVariable String id) {
        return service.segments(id);
    }

    @GetMapping("/{id}/findings")
    public List<DocFindingEntity> findings(@PathVariable String id) {
        return service.findings(id);
    }

    @GetMapping("/{id}/audit")
    public List<AuditLogEntity> audit(@PathVariable String id) {
        return service.audit(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        service.delete(id);
        return Map.of("id", id, "deleted", true);
    }

    @PostMapping("/{id}/feedback")
    public DocFindingEntity feedback(@PathVariable String id, @RequestBody FeedbackRequest request) {
        return service.feedback(id, request.findingId(), request.action(), request.description(),
                request.suggestedRevision());
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<String> report(@PathVariable String id, @RequestParam(defaultValue = "markdown") String format) {
        String report = service.report(id, format);
        if ("html".equalsIgnoreCase(format)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(report);
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(report);
    }

    public record FeedbackRequest(Long findingId, String action, String description, String suggestedRevision) {
    }

    public record RuleGenerationRequest(String businessContext, Integer ruleCount) {
    }
}
