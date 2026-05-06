package com.wuyou.rag.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * {@link DocumentParser} implementation for PDF files using Apache PDFBox 3.x.
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    private static final String SUPPORTED_TYPE = "pdf";

    @Override
    public String parse(byte[] data, String filename) {
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return normalizeWhitespace(text);
        } catch (IOException e) {
            log.error("Failed to parse PDF file: {}", filename, e);
            throw new RuntimeException("PDF parsing failed: " + filename, e);
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
