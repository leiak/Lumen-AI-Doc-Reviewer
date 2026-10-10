package com.docreview.review;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.docreview.segment.Segment;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentDiffCalculatorTest {
    private final DocumentDiffCalculator calculator = new DocumentDiffCalculator();

    @Test
    void marksSameContentAsUnchanged() {
        List<DocumentDiffCalculator.SegmentDiff> result = calculator.diff(
                List.of(new Segment(0, "第一条", "乙方提供服务。", 5)),
                List.of(new Segment(0, "第一条", "乙方提供服务。", 5)),
                0.9
        );
        assertFalse(result.getFirst().changed());
    }

    @Test
    void marksNewContentAsChanged() {
        List<DocumentDiffCalculator.SegmentDiff> result = calculator.diff(
                List.of(new Segment(0, "第一条", "乙方提供服务。", 5)),
                List.of(new Segment(0, "第一条", "甲方应当及时支付全部款项。", 10)),
                0.9
        );
        assertTrue(result.getFirst().changed());
    }
}
