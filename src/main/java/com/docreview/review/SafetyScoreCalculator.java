package com.docreview.review;

import com.docreview.persistence.entity.DocFindingEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SafetyScoreCalculator {
    public int calculate(List<DocFindingEntity> findings) {
        int score = 100;
        for (DocFindingEntity finding : findings) {
            score -= switch (finding.getSeverity()) {
                case "critical" -> 15;
                case "major" -> 8;
                case "minor" -> 3;
                default -> 0;
            };
        }
        return Math.max(0, score);
    }
}
