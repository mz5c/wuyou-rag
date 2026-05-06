package com.wuyou.rag.document.chunk;

/**
 * Result of a single chunk produced by {@link TextChunker}.
 *
 * @param content the chunk text
 * @param index   zero-based chunk index in the overall sequence
 * @param size    character count of this chunk
 */
public record ChunkResult(String content, int index, int size) {
}
