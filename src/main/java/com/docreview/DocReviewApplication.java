package com.docreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableAsync
    @ConfigurationPropertiesScan
    public class DocReviewApplication {
    public static void main(String[] args) {
        try {
            Files.createDirectories(Path.of("./data"));
            Files.createDirectories(Path.of("./data/uploads"));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize local data directories", exception);
        }
        SpringApplication.run(DocReviewApplication.class, args);
    }
}
