package com.wuyou.rag.document.parser;

/**
 * Shared text normalization utilities for document parsers.
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /**
     * Collapse tabs, multiple spaces, and excessive blank lines.
     */
    public static String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[\\t\\r]+", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    /**
     * Remove markdown image references: ![alt](url) or ![](url)
     */
    public static String stripMarkdownImages(String text) {
        return text.replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "");
    }

    /**
     * Remove common HTML tags, keeping inner text.
     */
    public static String stripHtmlTags(String text) {
        return text.replaceAll("<[^>]+>", "");
    }
}
