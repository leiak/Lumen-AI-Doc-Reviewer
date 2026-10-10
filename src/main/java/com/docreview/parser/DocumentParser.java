package com.docreview.parser;

import java.io.InputStream;

public interface DocumentParser {
    boolean supports(String fileName);
    String parse(InputStream input) throws Exception;
}
