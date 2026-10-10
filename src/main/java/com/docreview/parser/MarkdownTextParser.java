package com.docreview.parser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class MarkdownTextParser implements DocumentParser {
    @Override
    public boolean supports(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt");
    }

    @Override
    public String parse(InputStream input) throws Exception {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
