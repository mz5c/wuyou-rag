package com.wuyou.rag.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link DocumentParser} implementation for DOCX files using Apache POI 5.x.
 * <p>
 * Extracts both paragraph text and table content (cells separated by tabs,
 * rows separated by newlines).
 */
@Slf4j
@Component
public class DocxParser implements DocumentParser {

    private static final String SUPPORTED_TYPE = "docx";

    @Override
    public String parse(byte[] data, String filename) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
            StringBuilder result = new StringBuilder();

            // Extract paragraphs
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    result.append(text).append("\n");
                }
            }

            // Extract tables
            List<XWPFTable> tables = document.getTables();
            for (int t = 0; t < tables.size(); t++) {
                result.append("\n--- 表格 ").append(t + 1).append(" ---\n");
                for (var row : tables.get(t).getRows()) {
                    String rowText = row.getTableCells().stream()
                            .map(XWPFTableCell::getText)
                            .map(cell -> cell.strip().replace("\n", " "))
                            .collect(Collectors.joining("\t"));
                    if (!rowText.isBlank()) {
                        result.append(rowText).append("\n");
                    }
                }
            }

            return TextNormalizer.normalizeWhitespace(result.toString());
        } catch (IOException e) {
            log.error("Failed to parse DOCX file: {}", filename, e);
            throw new RuntimeException("DOCX parsing failed: " + filename, e);
        }
    }

    @Override
    public String supportedType() {
        return SUPPORTED_TYPE;
    }
}
