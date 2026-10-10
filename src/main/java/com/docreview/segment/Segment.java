package com.docreview.segment;

public record Segment(int orderIndex, String sectionPath, String content, int tokenCount) {
}
