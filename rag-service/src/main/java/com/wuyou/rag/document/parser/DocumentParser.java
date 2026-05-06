package com.wuyou.rag.document.parser;

/**
 * Strategy interface for parsing documents of a specific file type.
 * <p>
 * Each implementation handles one supported file type and returns
 * the plain-text content extracted from the raw bytes.
 */
public interface DocumentParser {

    /**
     * Parse the given raw file bytes into plain text.
     *
     * @param data     raw file bytes
     * @param filename original filename (may be used to determine further type hints)
     * @return extracted plain-text content
     */
    String parse(byte[] data, String filename);

    /**
     * Returns the file extension this parser handles (e.g. "pdf", "docx", "md").
     */
    String supportedType();
}
