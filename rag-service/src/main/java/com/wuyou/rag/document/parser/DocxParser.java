package com.wuyou.rag.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link DocumentParser} implementation for DOCX files using Apache POI 5.x.
 */
@Slf4j
@Component
public class DocxParser implements DocumentParser {

    private static final String SUPPORTED_TYPE = "docx";

    @Override
    public String parse(byte[] data, String filename) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            String text = paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .filter(line -> line != null)
                    .collect(Collectors.joining("\n"));
            return normalizeWhitespace(text);
        } catch (IOException e) {
            log.error("Failed to parse DOCX file: {}", filename, e);
            throw new RuntimeException("DOCX parsing failed: " + filename, e);
        }
    }

    @Override
    public String supportedType() {
        return SUPPORTED_TYPE;
    }

    /**
     * Collapse multiple blank lines and trim leading/trailing whitespace.
     */
    private String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[\\t\\r]+", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}
