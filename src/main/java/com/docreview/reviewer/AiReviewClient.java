package com.docreview.reviewer;

import com.docreview.config.ReviewerDefinition;
import com.docreview.segment.Segment;
import java.util.List;

public interface AiReviewClient {
    List<FindingDraft> review(ReviewerDefinition reviewer, Segment segment, String documentType);
}
