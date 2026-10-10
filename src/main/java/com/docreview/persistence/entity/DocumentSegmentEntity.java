package com.docreview.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_segments")
public class DocumentSegmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String sectionPath;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Integer tokenCount;

    @Column(nullable = false)
    private Integer orderIndex;

    private Boolean changed = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSectionPath() { return sectionPath; }
    public void setSectionPath(String sectionPath) { this.sectionPath = sectionPath; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public boolean isChanged() { return !Boolean.FALSE.equals(changed); }
    public void setChanged(boolean changed) { this.changed = changed; }
}
