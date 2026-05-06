package com.wuyou.rag.document.chunk;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits parsed document text into overlapping chunks for embedding and indexing.
 * <p>
 * Configuration is read dynamically from the {@code kb_config} table:
 * <ul>
 *   <li>{@code chunk.max_size} -- maximum characters per chunk (default: 1024)</li>
 *   <li>{@code chunk.overlap}  -- overlapping character window between chunks (default: 128)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextChunker {

    private static final String CONFIG_KEY_MAX_SIZE = "chunk.max_size";
    private static final String CONFIG_KEY_OVERLAP = "chunk.overlap";
    private static final int DEFAULT_MAX_SIZE = 1024;
    private static final int DEFAULT_OVERLAP = 128;

    private final KbConfigMapper kbConfigMapper;

    /**
     * Chunk the given plain text into a list of {@link ChunkResult}.
     */
    public List<ChunkResult> chunk(String text) {
        int maxSize = getConfigValue(CONFIG_KEY_MAX_SIZE, DEFAULT_MAX_SIZE);
        int overlap = getConfigValue(CONFIG_KEY_OVERLAP, DEFAULT_OVERLAP);

        log.debug("Chunking text (length={}) with maxSize={}, overlap={}", text.length(), maxSize, overlap);

        // Normalise line separators
        String normalized = text.replace("\r\n", "\n");

        if (normalized.isEmpty()) {
            return List.of();
        }

        if (normalized.length() <= maxSize) {
            return List.of(new ChunkResult(normalized, 0, normalized.length()));
        }

        // Split into paragraphs by double newline
        String[] paragraphs = normalized.split("\n\n", -1);

        List<ChunkResult> results = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String lastChunkOverlap = "";
        int chunkIndex = 0;

        for (int i = 0; i < paragraphs.length; i++) {
            String para = paragraphs[i].strip();
            if (para.isEmpty()) {
                continue;
            }

            // If this paragraph alone exceeds maxSize, hard-split it
            if (para.length() > maxSize) {
                // Flush current accumulated paragraphs first
                if (!currentChunk.isEmpty()) {
                    results.add(buildChunk(currentChunk, lastChunkOverlap, chunkIndex++, overlap));
                    lastChunkOverlap = extractOverlap(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder();
                }

                // Hard-split the oversized paragraph
                int start = 0;
                while (start < para.length()) {
                    int end = Math.min(start + maxSize, para.length());
                    String segment = para.substring(start, end);

                    // Build chunk with overlap from previous segment
                    StringBuilder segmentBuilder = new StringBuilder();
                    if (!lastChunkOverlap.isEmpty()) {
                        segmentBuilder.append(lastChunkOverlap);
                    }
                    segmentBuilder.append(segment);
                    ChunkResult cr = new ChunkResult(
                            segmentBuilder.toString(),
                            chunkIndex++,
                            segmentBuilder.length());
                    results.add(cr);

                    lastChunkOverlap = extractOverlap(para.substring(
                            Math.max(0, end - overlap), end), overlap);
                    start = end;
                }
                lastChunkOverlap = extractOverlap(para.substring(
                        Math.max(0, para.length() - overlap)), overlap);
                continue;
            }

            // Check if adding this paragraph would exceed maxSize
            int projectedLength = currentChunk.length()
                    + (currentChunk.isEmpty() ? 0 : 2) // separator "\n\n"
                    + para.length()
                    + (currentChunk.isEmpty() && !lastChunkOverlap.isEmpty() ? lastChunkOverlap.length() : 0);

            if (projectedLength > maxSize && !currentChunk.isEmpty()) {
                // Flush current chunk
                results.add(buildChunk(currentChunk, lastChunkOverlap, chunkIndex++, overlap));
                lastChunkOverlap = extractOverlap(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder();
            }

            // Append paragraph to current chunk
            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(para);
        }

        // Flush the last chunk
        if (!currentChunk.isEmpty()) {
            results.add(buildChunk(currentChunk, lastChunkOverlap, chunkIndex, overlap));
        }

        log.debug("Chunking complete: {} chunks produced", results.size());
        return results;
    }

    /**
     * Build a {@link ChunkResult} by optionally prepending overlap text.
     */
    private ChunkResult buildChunk(StringBuilder content, String overlapText,
                                   int index, int overlapSize) {
        String chunkContent = content.toString();
        if (!overlapText.isEmpty() && index > 0) {
            // For chunks after the first, prepend the overlap from previous chunk
            // But avoid duplicating if the content already starts with the overlap
            if (!chunkContent.startsWith(overlapText)) {
                chunkContent = overlapText + chunkContent;
            }
        }
        return new ChunkResult(chunkContent, index, chunkContent.length());
    }

    /**
     * Extract the last {@code overlap} characters from a string.
     */
    private String extractOverlap(String text, int overlap) {
        if (text == null || text.isEmpty() || overlap <= 0) {
            return "";
        }
        int len = text.length();
        if (len <= overlap) {
            return text;
        }
        return text.substring(len - overlap);
    }

    /**
     * Read an integer configuration value from the {@code kb_config} table.
     */
    private int getConfigValue(String key, int defaultValue) {
        try {
            KbConfig config = kbConfigMapper.selectOne(
                    Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, key));
            if (config != null && config.getConfigValue() != null) {
                return Integer.parseInt(config.getConfigValue().strip());
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid config value for key={}, falling back to default={}", key, defaultValue, e);
        }
        return defaultValue;
    }
}
