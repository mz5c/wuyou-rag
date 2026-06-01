package com.wuyou.rag.document.chunk;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits parsed document text into overlapping chunks for embedding and indexing.
 * <p>
 * Configuration is read dynamically from the {@code kb_config} table:
 * <ul>
 *   <li>{@code chunk.max_size} -- maximum characters per chunk (default: 1024)</li>
 *   <li>{@code chunk.overlap}  -- overlapping character window between chunks (default: 128)</li>
 *   <li>{@code chunk.segmenter} -- splitting strategy: {@code hard} (character boundary) or
 *       {@code sentence} (prefer sentence boundaries, default)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextChunker {

    private static final String CONFIG_KEY_MAX_SIZE = "chunk.max_size";
    private static final String CONFIG_KEY_OVERLAP = "chunk.overlap";
    private static final String CONFIG_KEY_SEGMENTER = "chunk.segmenter";
    private static final int DEFAULT_MAX_SIZE = 1024;
    private static final int DEFAULT_OVERLAP = 128;

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "[。！？：；.!?:;\\n]");

    private final KbConfigMapper kbConfigMapper;

    /**
     * Chunk the given plain text into a list of {@link ChunkResult}.
     */
    public List<ChunkResult> chunk(String text) {
        int maxSize = getConfigValue(CONFIG_KEY_MAX_SIZE, DEFAULT_MAX_SIZE);
        int overlap = getConfigValue(CONFIG_KEY_OVERLAP, DEFAULT_OVERLAP);
        String segmenter = getConfigString(CONFIG_KEY_SEGMENTER, "sentence");

        log.debug("Chunking text (length={}) with maxSize={}, overlap={}, segmenter={}",
                text.length(), maxSize, overlap, segmenter);

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

        for (String para : paragraphs) {
            para = para.strip();
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

                // Split the oversized paragraph
                List<String> segments = splitOversized(para, maxSize, overlap, segmenter);
                for (int i = 0; i < segments.size(); i++) {
                    String segment = segments.get(i);
                    StringBuilder segmentBuilder = new StringBuilder();
                    if (!lastChunkOverlap.isEmpty()) {
                        segmentBuilder.append(lastChunkOverlap);
                    }
                    segmentBuilder.append(segment);
                    results.add(new ChunkResult(
                            segmentBuilder.toString(),
                            chunkIndex++,
                            segmentBuilder.length()));
                    lastChunkOverlap = extractOverlap(segment, overlap);
                }
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
     * Split an oversized paragraph into segments, respecting sentence boundaries
     * when configured.
     */
    private List<String> splitOversized(String text, int maxSize, int overlap, String segmenter) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            if (end < text.length() && "sentence".equals(segmenter)) {
                int sentenceEnd = findLastSentenceBoundary(text, start, end);
                if (sentenceEnd > start) {
                    end = sentenceEnd + 1;
                }
            }
            segments.add(text.substring(start, end));
            start = end;
        }
        return segments;
    }

    /**
     * Find the last sentence boundary position in {@code [rangeStart, rangeEnd)}.
     * Returns -1 if no boundary is found.
     */
    static int findLastSentenceBoundary(String text, int rangeStart, int rangeEnd) {
        int bound = rangeEnd - 1;
        while (bound >= rangeStart) {
            char c = text.charAt(bound);
            if (c == '。' || c == '！' || c == '？' || c == '：' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == '\n') {
                return bound;
            }
            bound--;
        }
        return -1;
    }

    /**
     * Build a {@link ChunkResult} by optionally prepending overlap text.
     */
    private ChunkResult buildChunk(StringBuilder content, String overlapText,
                                   int index, int overlapSize) {
        String chunkContent = content.toString();
        if (!overlapText.isEmpty() && index > 0) {
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

    private String getConfigString(String key, String defaultValue) {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            return config.getConfigValue().strip();
        }
        return defaultValue;
    }
}
