package com.docreview.reviewer;

import com.docreview.config.ReviewerDefinition;
import com.docreview.segment.Segment;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockReviewClient implements AiReviewClient {
    @Override
    public List<FindingDraft> review(ReviewerDefinition reviewer, Segment segment, String documentType) {
        String revision = "建议明确条款责任、期限和执行标准。";
        return List.of(new FindingDraft(
                (long) segment.orderIndex(),
                reviewer.getRole(),
                "major",
                "risk",
                "Mock 审查：「%s」中可能存在表述不够明确或保护不足的问题。".formatted(segment.sectionPath()),
                revision
        ));
    }
}
