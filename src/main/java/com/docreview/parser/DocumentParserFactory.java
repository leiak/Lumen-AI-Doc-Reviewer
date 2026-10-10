package com.docreview.parser;

import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentParserFactory {
    private final List<DocumentParser> parsers;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public String parse(String fileName, InputStream input) throws Exception {
        return parsers.stream()
                .filter(parser -> parser.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported document type: " + fileName))
                .parse(input);
    }
}
