package com.docreview.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentSegmenterTest {
    private final DocumentSegmenter segmenter = new DocumentSegmenter();

    @Test
    void segmentsMarkdownByHeadings() {
        List<Segment> segments = segmenter.segment("""
                # 合同
                导言内容。

                ## 责任条款
                责任内容。

                ## 保密条款
                保密内容。
                """, "section", 30000);

        assertEquals(3, segments.size());
        assertEquals("合同", segments.getFirst().sectionPath());
        assertEquals("责任条款", segments.get(1).sectionPath());
        assertTrue(segments.get(2).content().contains("保密内容。"));
    }

    @Test
    void segmentsContractClauses() {
        List<Segment> segments = segmenter.segment("""
                第一条 服务范围
                乙方提供服务。

                第二条 付款条件
                甲方付款。
                """, "clause", 30000);

        assertEquals(2, segments.size());
        assertEquals("第一条 服务范围", segments.getFirst().sectionPath());
        assertEquals("第二条 付款条件", segments.get(1).sectionPath());
    }

    @Test
    void splitsOversizedContentByTokenMode() {
        String first = "第一段落。".repeat(20);
        String second = "第二段落。".repeat(20);
        List<Segment> segments = segmenter.segment(first + "\n\n" + second, "token", 24);
        assertEquals(2, segments.size());
    }
}
