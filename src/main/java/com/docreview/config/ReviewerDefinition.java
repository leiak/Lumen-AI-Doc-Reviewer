package com.docreview.config;

public class ReviewerDefinition {
    private String role;
    private String model;
    private String prompt;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
