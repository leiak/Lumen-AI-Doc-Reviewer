package com.docreview.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_sessions")
public class DocumentSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String docName;

    @Column(nullable = false)
    private String docType;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String status = "pending";

    private String parentSessionId;

    private Integer version = 1;

    private Boolean incremental;

    private Integer changedSegmentCount = 0;

    private Integer unchangedSegmentCount = 0;

    private Integer riskScore;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocName() { return docName; }
    public void setDocName(String docName) { this.docName = docName; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(String parentSessionId) { this.parentSessionId = parentSessionId; }
    public int getVersion() { return version == null ? 1 : version; }
    public void setVersion(int version) { this.version = version; }
    public boolean isIncremental() { return Boolean.TRUE.equals(incremental); }
    public void setIncremental(boolean incremental) { this.incremental = incremental; }
    public int getChangedSegmentCount() { return changedSegmentCount == null ? 0 : changedSegmentCount; }
    public void setChangedSegmentCount(int changedSegmentCount) { this.changedSegmentCount = changedSegmentCount; }
    public int getUnchangedSegmentCount() { return unchangedSegmentCount == null ? 0 : unchangedSegmentCount; }
    public void setUnchangedSegmentCount(int unchangedSegmentCount) { this.unchangedSegmentCount = unchangedSegmentCount; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
