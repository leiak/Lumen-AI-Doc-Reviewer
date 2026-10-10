package com.docreview.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "doc_findings")
public class DocFindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Long segmentId;

    @Column(nullable = false)
    private String reviewerRole;

    @Column(nullable = false)
    private String severity;

    private String riskType;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String suggestedRevision;

    private String userAction;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }
    public String getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRiskType() { return riskType; }
    public void setRiskType(String riskType) { this.riskType = riskType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestedRevision() { return suggestedRevision; }
    public void setSuggestedRevision(String suggestedRevision) { this.suggestedRevision = suggestedRevision; }
    public String getUserAction() { return userAction; }
    public void setUserAction(String userAction) { this.userAction = userAction; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
