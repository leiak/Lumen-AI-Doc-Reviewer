package com.docreview.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

@Component
public class DocxParser implements DocumentParser {
    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase().endsWith(".docx");
    }

    @Override
    public String parse(InputStream input) throws Exception {
        try (XWPFDocument document = new XWPFDocument(input)) {
            List<String> paragraphs = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    paragraphs.add(text);
                }
            }
            return String.join("\n\n", paragraphs);
        }
    }
}
