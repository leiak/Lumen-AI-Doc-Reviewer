package com.docreview.config;

import java.util.ArrayList;
import java.util.List;

public class ReviewTemplate {
    private String name;
    private String version = "1.0";
    private String documentType;
    private String chunkBy = "section";
    private int maxChunkTokens = 30000;
    private List<ReviewerDefinition> reviewers = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getChunkBy() { return chunkBy; }
    public void setChunkBy(String chunkBy) { this.chunkBy = chunkBy; }
    public int getMaxChunkTokens() { return maxChunkTokens; }
    public void setMaxChunkTokens(int maxChunkTokens) { this.maxChunkTokens = maxChunkTokens; }
    public List<ReviewerDefinition> getReviewers() { return reviewers; }
    public void setReviewers(List<ReviewerDefinition> reviewers) { this.reviewers = reviewers; }
}
