package com.docreview.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.docreview.persistence.entity.DocumentSegmentEntity;
import com.docreview.persistence.entity.DocumentSessionEntity;
import com.docreview.persistence.entity.DocFindingEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportRendererTest {
    private final ReportRenderer renderer = new ReportRenderer();

    @Test
    void markdownReportContainsScoreFindingAndDisclaimer() {
        DocumentSessionEntity session = new DocumentSessionEntity();
        session.setDocName("contract.md");
        session.setDocType("contract");
        session.setStatus("done");
        session.setRiskScore(85);
        session.setIncremental(true);
        session.setChangedSegmentCount(2);
        session.setUnchangedSegmentCount(3);

        DocumentSegmentEntity segment = new DocumentSegmentEntity();
        segment.setId(10L);
        segment.setSectionPath("第一条");

        DocFindingEntity finding = new DocFindingEntity();
        finding.setSegmentId(10L);
        finding.setReviewerRole("legal");
        finding.setSeverity("major");
        finding.setRiskType("risk");
        finding.setDescription("责任不明确");
        finding.setSuggestedRevision("补充责任边界");

        String report = renderer.renderMarkdown(session, List.of(segment), List.of(finding));

        assertTrue(report.contains("风险分：85/100"));
        assertTrue(report.contains("版本：v1"));
        assertTrue(report.contains("增量审查：变更段 2，未变更段 3。"));
        assertTrue(report.contains("责任不明确"));
        assertTrue(report.contains("不构成法律意见"));
    }
}
