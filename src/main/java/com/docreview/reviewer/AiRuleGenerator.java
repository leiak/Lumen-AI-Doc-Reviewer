package com.docreview.reviewer;

import com.docreview.config.DocReviewProperties;
import com.docreview.config.ReviewerDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiRuleGenerator {
    private final DocReviewProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRuleGenerator(DocReviewProperties properties) {
        this.properties = properties;
    }

    public List<ReviewerDefinition> generate(String documentType, String businessContext, int ruleCount) {
        int count = Math.max(1, Math.min(8, ruleCount));
        RestClient client = RestClient.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAi().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        String systemPrompt = """
                You are a senior review-framework designer. Return only a valid JSON array.
                Each object must contain: role (short english_snake_case reviewer name) and focus (Chinese review instruction).
                Rules must be specific, measurable, non-overlapping, and actionable for the document type.
                Do not return markdown explanations.
                """;
        String userPrompt = "Document type: %s\nRule count: %d\nBusiness context:\n%s".formatted(
                documentType, count, businessContext == null || businessContext.isBlank()
                        ? "No extra context provided."
                        : businessContext);
        try {
            Map<String, Object> response = client.post()
                    .uri(URI.create(properties.getAi().getBaseUrl() + "/chat/completions"))
                    .body(Map.of(
                            "model", properties.getAi().getDefaultModel(),
                            "temperature", 0.2,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(Map.class);
            String content = objectMapper.valueToTree(response)
                    .at("/choices/0/message/content").asText("");
            return parseRules(content);
        } catch (Exception exception) {
            throw new IllegalStateException("AI rule generation failed", exception);
        }
    }

    List<ReviewerDefinition> parseRules(String content) throws Exception {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("AI did not return any review rules");
        }
        JsonNode root = objectMapper.readTree(content.substring(start, end + 1));
        List<ReviewerDefinition> reviewers = new ArrayList<>();
        for (JsonNode node : root) {
            ReviewerDefinition reviewer = new ReviewerDefinition();
            reviewer.setRole(node.path("role").asText("").trim());
            reviewer.setPrompt(node.path("focus").asText("").trim());
            if (!reviewer.getRole().isBlank() && !reviewer.getPrompt().isBlank()) {
                reviewers.add(reviewer);
            }
        }
        if (reviewers.isEmpty()) {
            throw new IllegalStateException("AI returned no valid review rules");
        }
        return reviewers;
    }
}
