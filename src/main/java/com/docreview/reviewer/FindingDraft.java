package com.docreview.reviewer;

public record FindingDraft(
        Long segmentId,
        String reviewerRole,
        String severity,
        String riskType,
        String description,
        String suggestedRevision
) {
}
