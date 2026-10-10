package com.docreview.segment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DocumentSegmenter {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+.*");
    private static final Pattern CLAUSE = Pattern.compile(
            "^(第\\s*[0-9一二三四五六七八九十百千]+\\s*[条款章]|Article\\s+\\d+|SECTION\\s+\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBERED = Pattern.compile("^\\d+(?:\\.\\d+)*\\.?\\s+\\S.*");

    public List<Segment> segment(String text, String chunkBy, int maxChunkTokens) {
        List<String> blocks = switch (chunkBy == null ? "section" : chunkBy.toLowerCase()) {
            case "clause" -> splitByPattern(text, line -> CLAUSE.matcher(line).find());
            case "paragraph" -> splitByParagraph(text);
            case "token" -> splitByTokens(text, maxChunkTokens);
            default -> splitByPattern(text, line -> HEADING.matcher(line).matches() || CLAUSE.matcher(line).matches());
        };

        List<Segment> segments = new ArrayList<>();
        int order = 0;
        for (String block : blocks) {
            String content = block.trim();
            if (content.isEmpty()) {
                continue;
            }
            if (estimateTokens(content) > maxChunkTokens) {
                for (String part : splitByTokens(content, maxChunkTokens)) {
                    segments.add(new Segment(order++, sectionPathFor(part, segments.size() + 1), part, estimateTokens(part)));
                }
            } else {
                segments.add(new Segment(order++, sectionPathFor(content, segments.size() + 1), content, estimateTokens(content)));
            }
        }
        return segments;
    }

    private List<String> splitByPattern(String text, java.util.function.Predicate<String> newBlock) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.replace("\r\n", "\n").split("\\n", -1)) {
            if (newBlock.test(line.trim()) && !current.isEmpty()) {
                blocks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString());
        }
        return blocks;
    }

    private List<String> splitByParagraph(String text) {
        return List.of(text.replace("\r\n", "\n").split("\\n\\s*\\n"));
    }

    private List<String> splitByTokens(String text, int maxChunkTokens) {
        List<String> chunks = new ArrayList<>();
        for (String paragraph : text.replace("\r\n", "\n").split("\\n\\s*\\n")) {
            StringBuilder current = new StringBuilder();
            for (String line : paragraph.split("\\n", -1)) {
                if (!current.isEmpty() && estimateTokens(current + "\n" + line) > maxChunkTokens) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                current.append(line).append('\n');
            }
            if (!current.toString().trim().isEmpty()) {
                chunks.add(current.toString().trim());
            }
        }
        return chunks;
    }

    private String sectionPathFor(String content, int order) {
        String firstLine = content.split("\\n", 2)[0].trim();
        if (HEADING.matcher(firstLine).matches() || CLAUSE.matcher(firstLine).find() || NUMBERED.matcher(firstLine).matches()) {
            return firstLine.replaceAll("^#+\\s*", "");
        }
        return "Part " + order;
    }

    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 2.0));
    }
}
