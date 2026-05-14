package com.wuyou.rag.rag.search;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.entity.kb.KbChunk;
import com.wuyou.rag.entity.kb.KbDocument;
import com.wuyou.rag.mapper.KbChunkMapper;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.mapper.KbDocumentMapper;
import com.wuyou.rag.rag.embedding.EmbeddingService;
import com.wuyou.rag.rag.vector.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int DEFAULT_MILVUS_TOP_K = 20;
    private static final int DEFAULT_ES_TOP_K = 20;
    private static final int DEFAULT_FINAL_TOP_K = 5;
    private static final int DEFAULT_RRF_K = 60;

    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final EsSearchService esSearchService;
    private final KbConfigMapper kbConfigMapper;
    private final KbChunkMapper kbChunkMapper;
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * Hybrid search: vector search + BM25 search + RRF fusion.
     * Falls back to pure vector search if ES is unavailable.
     */
    public List<SearchResult> search(String question, Long kbId) {
        boolean hybridEnabled = getConfigBoolean("search.hybrid.enabled", true);

        if (!hybridEnabled || !esSearchService.isAvailable()) {
            // Fallback to pure vector search
            return pureVectorSearch(question);
        }

        int milvusTopK = getConfigInt("search.hybrid.milvus_top_k", DEFAULT_MILVUS_TOP_K);
        int esTopK = getConfigInt("search.hybrid.es_top_k", DEFAULT_ES_TOP_K);
        int finalTopK = getConfigInt("search.hybrid.final_top_k", DEFAULT_FINAL_TOP_K);
        int rrfK = getConfigInt("search.hybrid.rrf_k", DEFAULT_RRF_K);

        try {
            // 1. Generate embedding
            float[] queryVector = embeddingService.embed(question);

            // 2. Parallel search
            List<Long> milvusChunkIds = vectorService.search(queryVector, milvusTopK);
            List<EsSearchService.EsSearchHit> esHits = esSearchService.search(question, kbId, esTopK);

            // 3. RRF fusion
            return rrfFuse(milvusChunkIds, esHits, rrfK, finalTopK);

        } catch (Exception e) {
            log.warn("Hybrid search failed, falling back to pure vector search", e);
            return pureVectorSearch(question);
        }
    }

    private List<SearchResult> pureVectorSearch(String question) {
        float[] queryVector = embeddingService.embed(question);
        int topK = getConfigInt("search.hybrid.final_top_k", DEFAULT_FINAL_TOP_K);
        List<Long> chunkIds;
        try {
            chunkIds = vectorService.search(queryVector, topK);
        } catch (Exception e) {
            log.error("Pure vector search also failed", e);
            return new ArrayList<>();
        }
        return enrichResults(chunkIds);
    }

    private List<SearchResult> rrfFuse(List<Long> milvusChunkIds, List<EsSearchService.EsSearchHit> esHits,
                                       int rrfK, int finalTopK) {
        // Build rank maps: chunkId -> rank (1-based)
        Map<Long, Integer> milvusRanks = new HashMap<>();
        for (int i = 0; i < milvusChunkIds.size(); i++) {
            milvusRanks.put(milvusChunkIds.get(i), i + 1);
        }

        Map<Long, Integer> esRanks = new HashMap<>();
        for (int i = 0; i < esHits.size(); i++) {
            esRanks.put(esHits.get(i).chunkId(), i + 1);
        }

        // Collect all unique chunk IDs
        Set<Long> allChunkIds = new HashSet<>();
        allChunkIds.addAll(milvusChunkIds);
        allChunkIds.addAll(esHits.stream().map(EsSearchService.EsSearchHit::chunkId).collect(Collectors.toList()));

        // Compute RRF scores
        List<Map.Entry<Long, Double>> scored = new ArrayList<>();
        for (Long chunkId : allChunkIds) {
            double score = 0;
            Integer mr = milvusRanks.get(chunkId);
            if (mr != null) {
                score += 1.0 / (rrfK + mr);
            }
            Integer er = esRanks.get(chunkId);
            if (er != null) {
                score += 1.0 / (rrfK + er);
            }
            scored.add(new AbstractMap.SimpleEntry<>(chunkId, score));
        }

        // Sort by score descending, take topK
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Long> topChunkIds = scored.stream()
                .limit(finalTopK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return enrichResults(topChunkIds);
    }

    private List<SearchResult> enrichResults(List<Long> chunkIds) {
        List<SearchResult> results = new ArrayList<>();
        for (Long chunkId : chunkIds) {
            KbChunk chunk = kbChunkMapper.selectById(chunkId);
            if (chunk == null) continue;

            String docTitle = null;
            String docUrl = null;
            if (chunk.getDocId() != null) {
                KbDocument doc = kbDocumentMapper.selectById(chunk.getDocId());
                if (doc != null) {
                    docTitle = doc.getTitle();
                    docUrl = doc.getFileUrl();
                }
            }
            results.add(new SearchResult(chunkId, chunk.getChunkContent(), docTitle, docUrl));
        }
        return results;
    }

    private int getConfigInt(String key, int defaultValue) {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null) {
            try { return Integer.parseInt(config.getConfigValue()); }
            catch (NumberFormatException e) { log.warn("Invalid config {}: {}", key, config.getConfigValue()); }
        }
        return defaultValue;
    }

    private boolean getConfigBoolean(String key, boolean defaultValue) {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null) {
            return Boolean.parseBoolean(config.getConfigValue());
        }
        return defaultValue;
    }

    public record SearchResult(Long chunkId, String content, String docTitle, String docUrl) {}
}
