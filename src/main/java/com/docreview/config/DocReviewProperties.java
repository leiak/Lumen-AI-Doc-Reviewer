package com.docreview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "doc-review")
public class DocReviewProperties {
    private String uploadDir = "./data/uploads";
    private double duplicateThreshold = 0.72;
    private final Ai ai = new Ai();

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public double getDuplicateThreshold() { return duplicateThreshold; }
    public void setDuplicateThreshold(double duplicateThreshold) { this.duplicateThreshold = duplicateThreshold; }
    public Ai getAi() { return ai; }

    public static class Ai {
        private boolean mock = true;
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String defaultModel = "gpt-4o-mini";
        private int timeoutSeconds = 120;

        public boolean isMock() { return mock; }
        public void setMock(boolean mock) { this.mock = mock; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
