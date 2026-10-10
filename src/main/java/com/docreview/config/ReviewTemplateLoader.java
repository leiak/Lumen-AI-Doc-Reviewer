package com.docreview.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class ReviewTemplateLoader {
    private final ObjectMapper yamlMapper = new YAMLMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path customTemplateDir;

    public ReviewTemplateLoader(
            @Value("${doc-review.custom-template-dir:./data/templates/review}") Path customTemplateDir) {
        this.customTemplateDir = customTemplateDir;
    }

    public ReviewTemplate load(String name) {
        try {
            Path override = customPath(name);
            if (Files.exists(override)) {
                return yamlMapper.readValue(override.toFile(), ReviewTemplate.class);
            }
            String location = "classpath:/templates/review/%s.yaml".formatted(name);
            Resource resource = new PathMatchingResourcePatternResolver().getResource(location);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Review template not found: " + name);
            }
            return yamlMapper.readValue(resource.getInputStream(), ReviewTemplate.class);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load review template: " + name, exception);
        }
    }

    public List<String> available() {
        try {
            Set<String> names = new LinkedHashSet<>(List.of(new PathMatchingResourcePatternResolver()
                    .getResources("classpath:/templates/review/*.yaml"))
                    .stream()
                    .map(Resource::getFilename)
                    .map(name -> name.substring(0, name.lastIndexOf('.')))
                    .toList());
            if (Files.isDirectory(customTemplateDir)) {
                try (var files = Files.list(customTemplateDir)) {
                    files.filter(path -> path.getFileName().toString().endsWith(".yaml"))
                            .map(path -> path.getFileName().toString())
                            .map(name -> name.substring(0, name.lastIndexOf('.')))
                            .forEach(names::add);
                }
            }
            return names.stream().sorted().toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to list review templates", exception);
        }
    }

    public void save(String name, ReviewTemplate template) {
        validateName(name);
        if (template == null || template.getReviewers() == null || template.getReviewers().isEmpty()) {
            throw new IllegalArgumentException("Template must contain at least one reviewer");
        }
        for (int index = 0; index < template.getReviewers().size(); index++) {
            ReviewerDefinition reviewer = template.getReviewers().get(index);
            if (reviewer == null || reviewer.getRole() == null || reviewer.getRole().isBlank()
                    || reviewer.getPrompt() == null || reviewer.getPrompt().isBlank()) {
                throw new IllegalArgumentException("Reviewer " + (index + 1) + " requires role and prompt");
            }
            reviewer.setRole(reviewer.getRole().trim());
            reviewer.setPrompt(reviewer.getPrompt().trim());
        }
        try {
            Files.createDirectories(customTemplateDir);
            template.setName(name);
            yamlMapper.writeValue(customPath(name).toFile(), template);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save review template: " + name, exception);
        }
    }

    public void reset(String name) {
        validateName(name);
        try {
            Files.deleteIfExists(customPath(name));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to reset review template: " + name, exception);
        }
    }

    private Path customPath(String name) {
        validateName(name);
        return customTemplateDir.resolve(name + ".yaml").normalize();
    }

    private void validateName(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid template name: " + name);
        }
    }
}
