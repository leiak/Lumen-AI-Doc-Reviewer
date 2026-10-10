package com.docreview.report;

import com.docreview.persistence.entity.DocumentSegmentEntity;
import com.docreview.persistence.entity.DocumentSessionEntity;
import com.docreview.persistence.entity.DocFindingEntity;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReportRenderer {
    public String renderMarkdown(DocumentSessionEntity session,
                                 List<DocumentSegmentEntity> segments,
                                 List<DocFindingEntity> findings) {
        Map<Long, String> sectionPaths = segments.stream()
                .collect(Collectors.toMap(DocumentSegmentEntity::getId, DocumentSegmentEntity::getSectionPath));
        StringBuilder report = new StringBuilder();
        report.append("# 文档审查报告\n\n");
        report.append("- 文件：").append(session.getDocName()).append('\n');
        report.append("- 类型：").append(session.getDocType()).append('\n');
        report.append("- 版本：v").append(session.getVersion()).append('\n');
        report.append("- 状态：").append(session.getStatus()).append('\n');
        report.append("- 风险分：").append(session.getRiskScore()).append("/100\n\n");
        report.append("> AI 审查结果仅供参考，不构成法律意见。\n\n");
        if (session.isIncremental()) {
            report.append("> 增量审查：变更段 ").append(session.getChangedSegmentCount())
                    .append("，未变更段 ").append(session.getUnchangedSegmentCount()).append("。\n\n");
        }
        report.append("## 审查发现\n\n");
        if (findings.isEmpty()) {
            report.append("未发现明显问题。\n");
        } else {
            for (DocFindingEntity finding : findings) {
                report.append("### ").append(finding.getSeverity()).append(" / ")
                        .append(finding.getReviewerRole()).append('\n');
                report.append("- 位置：").append(sectionPaths.getOrDefault(finding.getSegmentId(), "未知")).append('\n');
                report.append("- 类型：").append(finding.getRiskType()).append('\n');
                report.append("- 描述：").append(finding.getDescription()).append('\n');
                report.append("- 建议：").append(finding.getSuggestedRevision()).append('\n');
                if (finding.getUserAction() != null) {
                    report.append("- 用户处理：").append(finding.getUserAction()).append('\n');
                }
                report.append('\n');
            }
        }
        return report.toString();
    }

    public String renderHtml(DocumentSessionEntity session,
                             List<DocumentSegmentEntity> segments,
                             List<DocFindingEntity> findings) {
        String markdown = renderMarkdown(session, segments, findings);
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>文档审查报告</title>
                  <style>
                    body { font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; line-height: 1.65; max-width: 980px; margin: 32px auto; padding: 0 20px; color: #172033; }
                    pre { white-space: pre-wrap; background: #f6f8fb; padding: 18px; border-radius: 12px; }
                  </style>
                </head>
                <body><pre>%s</pre></body></html>
                """.formatted(markdown);
    }
}
