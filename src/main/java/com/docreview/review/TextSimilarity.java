package com.docreview.review;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class TextSimilarity {
    private TextSimilarity() {
    }

    public static double jaccard(String left, String right) {
        Set<String> first = tokens(left);
        Set<String> second = tokens(right);
        if (first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);
        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private static Set<String> tokens(String value) {
        Set<String> tokens = new HashSet<>();
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        for (String token : normalized.split("\\s+")) {
            if (token.codePointCount(0, token.length()) == 1) {
                tokens.add(token);
            } else if (token.matches(".*[\\u4e00-\\u9fff].*")) {
                for (int index = 0; index < token.length() - 1; index++) {
                    tokens.add(token.substring(index, index + 2));
                }
            } else {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
