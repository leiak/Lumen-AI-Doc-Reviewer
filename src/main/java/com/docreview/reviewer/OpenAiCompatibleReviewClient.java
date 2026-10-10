package com.docreview.reviewer;

import com.docreview.config.DocReviewProperties;
import com.docreview.config.ReviewerDefinition;
import com.docreview.segment.Segment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiCompatibleReviewClient implements AiReviewClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleReviewClient.class);
    private final DocReviewProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleReviewClient(DocReviewProperties properties) {
        this.properties = properties;
    }

    private String resolveModel(ReviewerDefinition reviewer) {
        return reviewer.getModel() == null || reviewer.getModel().isBlank()
                ? properties.getAi().getDefaultModel()
                : reviewer.getModel();
    }

    @Override
    public List<FindingDraft> review(ReviewerDefinition reviewer, Segment segment, String documentType) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAi().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        String systemPrompt = """
                You are a precise document reviewer. Return only a valid JSON array. Each object must contain:
                severity (critical|major|minor|info), risk_type, description, suggested_revision.
                Do not invent clauses. If there is no issue, return [].
                """;
        String userPrompt = "%s\n\nDocument type: %s\nSection: %s\nContent:\n%s".formatted(
                reviewer.getPrompt(), documentType, segment.sectionPath(), segment.content());
        try {
            Map<String, Object> response = client.post()
                    .uri(URI.create(properties.getAi().getBaseUrl() + "/chat/completions"))
                    .body(Map.of(
                            "model", resolveModel(reviewer),
                            "temperature", 0.1,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(Map.class);
            String content = objectMapper.valueToTree(response)
                    .at("/choices/0/message/content").asText("");
            return parseFindings(content, segment, reviewer.getRole());
        } catch (Exception exception) {
            log.error("AI review request failed", exception);
            throw new IllegalStateException("AI review failed for role " + reviewer.getRole(), exception);
        }
    }

    List<FindingDraft> parseFindings(String content, Segment segment, String role) throws Exception {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(content.substring(start, end + 1));
        List<FindingDraft> findings = new ArrayList<>();
        for (JsonNode node : root) {
            findings.add(new FindingDraft(
                    (long) segment.orderIndex(),
                    role,
                    node.path("severity").asText("minor"),
                    node.path("risk_type").asText("risk"),
                    node.path("description").asText(""),
                    node.path("suggested_revision").asText("")
            ));
        }
        return findings;
    }
}
