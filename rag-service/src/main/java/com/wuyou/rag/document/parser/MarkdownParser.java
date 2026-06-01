package com.wuyou.rag.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * {@link DocumentParser} implementation for Markdown files.
 * <p>
 * Decodes the raw bytes as UTF-8, strips HTML tags and image references,
 * then normalizes whitespace.
 */
@Slf4j
@Component
public class MarkdownParser implements DocumentParser {

    private static final String SUPPORTED_TYPE = "md";

    @Override
    public String parse(byte[] data, String filename) {
        String text = new String(data, StandardCharsets.UTF_8);
        text = TextNormalizer.stripMarkdownImages(text);
        text = TextNormalizer.stripHtmlTags(text);
        return TextNormalizer.normalizeWhitespace(text);
    }

    @Override
    public String supportedType() {
        return SUPPORTED_TYPE;
    }
}
