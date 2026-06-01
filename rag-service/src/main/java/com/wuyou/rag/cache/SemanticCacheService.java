package com.wuyou.rag.cache;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.rag.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Semantic cache that matches semantically similar questions to cached answers.
 * <p>
 * Uses Redis hash to store cache entries. Each entry contains the question text,
 * answer, and embedding vector. Lookup computes cosine similarity between the
 * input and all cached entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final String CACHE_INDEX_KEY = "rag:qa:semantic:index";
    private static final String CACHE_DATA_KEY = "rag:qa:semantic:data";
    private static final long CACHE_TTL_SECONDS = 3600;

    private static final String CONFIG_KEY_ENABLED = "cache.semantic.enabled";
    private static final String CONFIG_KEY_THRESHOLD = "cache.semantic.threshold";
    private static final double DEFAULT_THRESHOLD = 0.92;
    private static final int MAX_CACHE_ENTRIES = 500;

    private final EmbeddingService embeddingService;
    private final StringRedisTemplate redisTemplate;
    private final KbConfigMapper kbConfigMapper;

    public boolean isEnabled() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, CONFIG_KEY_ENABLED));
        return config != null && Boolean.parseBoolean(config.getConfigValue());
    }

    public double getThreshold() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, CONFIG_KEY_THRESHOLD));
        if (config != null && config.getConfigValue() != null) {
            try {
                return Double.parseDouble(config.getConfigValue().strip());
            } catch (NumberFormatException e) {
                log.warn("Invalid cache.semantic.threshold: {}", config.getConfigValue());
            }
        }
        return DEFAULT_THRESHOLD;
    }

    /**
     * Look up a semantically similar cached answer.
     *
     * @return cached answer, or {@code null} if no match found
     */
    public String lookup(String question) {
        if (!isEnabled()) {
            return null;
        }

        // Read all cached entries first — avoid embedding if cache is empty
        Map<Object, Object> indexEntries = redisTemplate.opsForHash().entries(CACHE_INDEX_KEY);
        if (indexEntries.isEmpty()) {
            return null;
        }

        float[] queryEmbedding = embeddingService.embed(question);
        double threshold = getThreshold();

        String bestMatchKey = null;
        double bestScore = 0;

        for (Map.Entry<Object, Object> entry : indexEntries.entrySet()) {
            String embeddingJson = (String) entry.getValue();
            float[] cachedEmbedding = parseEmbedding(embeddingJson);
            if (cachedEmbedding == null) continue;

            double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatchKey = (String) entry.getKey();
            }
        }

        if (bestMatchKey == null || bestScore < threshold) {
            return null;
        }

        // Fetch the cached answer
        String answer = (String) redisTemplate.opsForHash().get(CACHE_DATA_KEY, bestMatchKey);
        if (answer == null) {
            // Clean up stale index entry
            redisTemplate.opsForHash().delete(CACHE_INDEX_KEY, bestMatchKey);
            return null;
        }

        log.info("Semantic cache hit: key={}, similarity={}", bestMatchKey, String.format("%.4f", bestScore));
        return answer;
    }

    /**
     * Store a question-answer pair in the semantic cache.
     */
    public void store(String question, String answer) {
        if (!isEnabled()) {
            return;
        }

        try {
            float[] embedding = embeddingService.embed(question);
            String key = md5Hex(question);
            String embeddingJson = embeddingToJson(embedding);

            redisTemplate.opsForHash().put(CACHE_INDEX_KEY, key, embeddingJson);
            redisTemplate.opsForHash().put(CACHE_DATA_KEY, key, answer);

            // Set TTL via expire on the data key (entries cleaned on lookup if stale)
            redisTemplate.expire(CACHE_INDEX_KEY, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            redisTemplate.expire(CACHE_DATA_KEY, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

            // Evict oldest entries if over limit
            Long size = redisTemplate.opsForHash().size(CACHE_INDEX_KEY);
            if (size != null && size > MAX_CACHE_ENTRIES) {
                evictOldest();
            }
        } catch (Exception e) {
            log.warn("Failed to store semantic cache entry", e);
        }
    }

    private void evictOldest() {
        Set<Object> keys = redisTemplate.opsForHash().keys(CACHE_INDEX_KEY);
        int toRemove = keys.size() - MAX_CACHE_ENTRIES;
        if (toRemove <= 0) return;

        keys.stream().limit(toRemove).forEach(k -> {
            redisTemplate.opsForHash().delete(CACHE_INDEX_KEY, k);
            redisTemplate.opsForHash().delete(CACHE_DATA_KEY, k);
        });
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dotProduct / denom;
    }

    private static String embeddingToJson(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private static float[] parseEmbedding(String json) {
        try {
            String trimmed = json.trim();
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null;
            String[] parts = trimmed.substring(1, trimmed.length() - 1).split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].strip());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse cached embedding", e);
            return null;
        }
    }

    private static String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
