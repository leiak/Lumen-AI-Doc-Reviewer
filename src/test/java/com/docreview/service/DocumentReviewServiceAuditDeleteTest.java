package com.docreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.docreview.DocReviewApplication;
import com.docreview.persistence.entity.AuditLogEntity;
import com.docreview.persistence.entity.DocumentSessionEntity;
import com.docreview.persistence.repository.AuditLogRepository;
import com.docreview.persistence.repository.DocumentSegmentRepository;
import com.docreview.persistence.repository.DocumentSessionRepository;
import com.docreview.persistence.repository.DocFindingRepository;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(classes = DocReviewApplication.class)
@DirtiesContext
class DocumentReviewServiceAuditDeleteTest {
    @TempDir
    static Path tempDir;

    @Autowired
    private DocumentReviewService service;

    @Autowired
    private DocumentSessionRepository sessionRepository;

    @Autowired
    private DocumentSegmentRepository segmentRepository;

    @Autowired
    private DocFindingRepository findingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempDir.resolve("review.db"));
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.community.dialect.SQLiteDialect");
        registry.add("doc-review.upload-dir", () -> tempDir.resolve("uploads").toString());
        registry.add("doc-review.ai.mock", () -> "true");
    }

    @Test
    void auditsLifecycleAndDeletesSessionWithData() throws Exception {
        Path document = tempDir.resolve("document.txt");
        Files.writeString(document, "# 合同条款\n甲方应在三十日内交付成果。");
        Map<String, Object> result = service.upload(new PathMultipartFile(document), "contract", null);
        String sessionId = (String) result.get("id");

        awaitReviewComplete(sessionId);
        Long findingId = service.findings(sessionId).getFirst().getId();
        service.feedback(sessionId, findingId, "accepted", "同意修改", null);
        service.report(sessionId, "markdown");

        assertThat(auditLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId))
                .extracting(AuditLogEntity::getAction)
                .containsExactly("report_access", "feedback", "review_done", "upload");

        service.delete(sessionId);

        assertThat(sessionRepository.existsById(sessionId)).isFalse();
        assertThat(segmentRepository.findBySessionIdOrderByOrderIndexAsc(sessionId)).isEmpty();
        assertThat(findingRepository.findBySessionIdOrderBySeverityAscIdAsc(sessionId)).isEmpty();
        assertThat(auditLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId))
                .extracting(AuditLogEntity::getAction)
                .contains("session_delete");
    }

    @Test
    void blocksDeletingSessionReferencedByLaterVersion() {
        DocumentSessionEntity parent = new DocumentSessionEntity();
        parent.setDocName("parent.txt");
        parent.setDocType("contract");
        parent.setFilePath(tempDir.resolve("missing.txt").toString());
        parent = sessionRepository.save(parent);

        DocumentSessionEntity child = new DocumentSessionEntity();
        child.setDocName("child.txt");
        child.setDocType("contract");
        child.setFilePath(tempDir.resolve("missing.txt").toString());
        child.setParentSessionId(parent.getId());
        child.setVersion(parent.getVersion() + 1);
        child.setIncremental(true);
        sessionRepository.save(child);
        String parentId = parent.getId();

        assertThatThrownBy(() -> service.delete(parentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("later version");
    }

    private void awaitReviewComplete(String sessionId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> status = service.status(sessionId);
            if (!"reviewing".equals(status.get("status"))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Review did not complete in time");
    }

    private record PathMultipartFile(Path path) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return path.getFileName().toString(); }
        @Override public String getContentType() { return "text/plain"; }
        @Override public boolean isEmpty() { return false; }
        @Override public long getSize() { return path.toFile().length(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
        @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
        @Override public void transferTo(File destination) throws IOException {
            Files.copy(path, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        @Override public void transferTo(Path destination) throws IOException {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
