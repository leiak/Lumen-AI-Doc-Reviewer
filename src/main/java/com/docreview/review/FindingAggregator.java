package com.docreview.review;

import com.docreview.reviewer.FindingDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FindingAggregator {
    public List<FindingDraft> aggregate(List<FindingDraft> findings, double threshold) {
        List<FindingDraft> result = new ArrayList<>();
        for (FindingDraft finding : findings) {
            boolean duplicate = false;
            for (int index = 0; index < result.size(); index++) {
                FindingDraft existing = result.get(index);
                if (existing.segmentId().equals(finding.segmentId())
                        && similarity(existing.description(), finding.description()) >= threshold) {
                    if (severityRank(finding.severity()) > severityRank(existing.severity())) {
                        result.set(index, finding);
                    }
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(finding);
            }
        }
        return result.stream().sorted((left, right) -> {
            int severity = Integer.compare(severityRank(right.severity()), severityRank(left.severity()));
            return severity != 0 ? severity : Long.compare(left.segmentId(), right.segmentId());
        }).toList();
    }

    private double similarity(String left, String right) {
        return TextSimilarity.jaccard(left, right);
    }

    private int severityRank(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase(Locale.ROOT)) {
            case "critical" -> 4;
            case "major" -> 3;
            case "minor" -> 2;
            case "info" -> 1;
            default -> 0;
        };
    }
}
