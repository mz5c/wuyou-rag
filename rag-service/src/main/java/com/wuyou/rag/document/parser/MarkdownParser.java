package com.wuyou.rag.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * {@link DocumentParser} implementation for Markdown files.
 * <p>
 * Simply decodes the raw bytes as UTF-8 text. Markdown structure is preserved
 * for downstream chunking; no AST-level processing is performed here.
 */
@Slf4j
@Component
public class MarkdownParser implements DocumentParser {

    private static final String SUPPORTED_TYPE = "md";

    @Override
    public String parse(byte[] data, String filename) {
        return new String(data, StandardCharsets.UTF_8).strip();
    }

    @Override
    public String supportedType() {
        return SUPPORTED_TYPE;
    }
}
