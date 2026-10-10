package com.docreview.review;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.docreview.reviewer.FindingDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingAggregatorTest {
    private final FindingAggregator aggregator = new FindingAggregator();

    @Test
    void keepsHighestSeverityForSimilarFindingsInSameSegment() {
        List<FindingDraft> findings = List.of(
                new FindingDraft(1L, "legal", "minor", "risk", "This clause creates unequal liability.", "Fix it."),
                new FindingDraft(1L, "compliance", "critical", "risk", "This clause creates unequal liability terms.", "Fix it now.")
        );

        List<FindingDraft> result = aggregator.aggregate(findings, 0.8);

        assertEquals(1, result.size());
        assertEquals("critical", result.getFirst().severity());
    }

    @Test
    void doesNotMergeSimilarFindingsAcrossSegments() {
        List<FindingDraft> findings = List.of(
                new FindingDraft(1L, "legal", "minor", "risk", "The payment terms are unclear.", "Clarify."),
                new FindingDraft(2L, "legal", "minor", "risk", "The payment terms are unclear.", "Clarify.")
        );

        assertEquals(2, aggregator.aggregate(findings, 0.8).size());
    }
}
