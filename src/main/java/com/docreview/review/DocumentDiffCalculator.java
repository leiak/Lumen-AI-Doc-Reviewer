package com.docreview.review;

import com.docreview.segment.Segment;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentDiffCalculator {
    public record SegmentDiff(Segment segment, boolean changed) {
    }

    public List<SegmentDiff> diff(List<Segment> previousSegments,
                                  List<Segment> nextSegments,
                                  double unchangedThreshold) {
        List<SegmentDiff> result = new ArrayList<>();
        for (Segment next : nextSegments) {
            boolean changed = true;
            for (Segment previous : previousSegments) {
                if (TextSimilarity.jaccard(previous.content(), next.content()) >= unchangedThreshold) {
                    changed = false;
                    break;
                }
            }
            result.add(new SegmentDiff(next, changed));
        }
        return result;
    }
}
