package com.wuyou.rag.rag.rerank;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.rag.search.HybridSearchService.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RerankerService {

    private static final String CONFIG_KEY_ENABLED = "search.rerank.enabled";
    private static final String CONFIG_KEY_API_URL = "search.rerank.api_url";
    private static final String DEFAULT_API_URL = "http://localhost:5002/rerank";

    private final RestTemplate restTemplate;
    private final KbConfigMapper kbConfigMapper;

    public boolean isEnabled() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, CONFIG_KEY_ENABLED));
        return config != null && Boolean.parseBoolean(config.getConfigValue());
    }

    /**
     * Re-rank search results using a cross-encoder reranker API.
     * Falls back to original order if reranker is unavailable.
     */
    public List<SearchResult> rerank(String question, List<SearchResult> results) {
        if (!isEnabled() || results == null || results.size() <= 1) {
            return results;
        }

        String apiUrl = getApiUrl();
        List<String> texts = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.toList());

        try {
            JSONObject body = new JSONObject();
            body.put("query", question);
            body.put("texts", texts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

            JSONObject resp = restTemplate.postForEntity(apiUrl, entity, JSONObject.class).getBody();
            if (resp == null) {
                log.warn("Reranker returned empty response");
                return results;
            }

            // Parse scores — supports both { "scores": [...] } and { "results": [{ "index": i, "score": s }] }
            double[] scores = parseScores(resp, texts.size());
            if (scores == null) {
                return results;
            }

            // Sort by score descending, preserving original index
            List<Integer> indices = IntStream.range(0, results.size())
                    .boxed()
                    .sorted(Comparator.comparingDouble((Integer i) -> scores[i]).reversed())
                    .collect(Collectors.toList());

            List<SearchResult> reranked = new ArrayList<>(results.size());
            for (int idx : indices) {
                reranked.add(results.get(idx));
            }

            log.info("Reranked {} results, top score={}", results.size(), scores[indices.get(0)]);
            return reranked;

        } catch (Exception e) {
            log.warn("Reranker call failed, using original order: {}", e.getMessage());
            return results;
        }
    }

    private double[] parseScores(JSONObject resp, int expectedSize) {
        JSONArray scoresArr = resp.getJSONArray("scores");
        if (scoresArr != null && scoresArr.size() == expectedSize) {
            double[] scores = new double[expectedSize];
            for (int i = 0; i < expectedSize; i++) {
                scores[i] = scoresArr.getDoubleValue(i);
            }
            return scores;
        }

        JSONArray resultsArr = resp.getJSONArray("results");
        if (resultsArr != null && resultsArr.size() == expectedSize) {
            double[] scores = new double[expectedSize];
            for (int i = 0; i < resultsArr.size(); i++) {
                JSONObject item = resultsArr.getJSONObject(i);
                int index = item.getIntValue("index");
                double score = item.getDoubleValue("score");
                if (index >= 0 && index < expectedSize) {
                    scores[index] = score;
                }
            }
            return scores;
        }

        log.warn("Reranker response format not recognized: {}", resp);
        return null;
    }

    private String getApiUrl() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, CONFIG_KEY_API_URL));
        return config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()
                ? config.getConfigValue().strip()
                : DEFAULT_API_URL;
    }
}
