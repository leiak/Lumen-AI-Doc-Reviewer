package com.docreview.parser;

import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfParser implements DocumentParser {
    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String parse(InputStream input) throws Exception {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(input))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).trim();
        }
    }
}
