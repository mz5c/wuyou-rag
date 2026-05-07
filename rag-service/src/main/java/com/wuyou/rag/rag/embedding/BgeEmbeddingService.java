package com.wuyou.rag.rag.embedding;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BgeEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final KbConfigMapper kbConfigMapper;

    private String getApiUrl() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "embedding.api_url"));
        return config != null ? config.getConfigValue() : "http://localhost:5001/embed";
    }

    @Override
    public float[] embed(String text) {
        String url = getApiUrl();
        Map<String, Object> body = new HashMap<>();
        body.put("texts", Collections.singletonList(text));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            List<List<Float>> embeddings = (List<List<Float>>) response.getBody().get("embeddings");
            List<Float> floats = embeddings.get(0);
            float[] vector = new float[floats.size()];
            for (int i = 0; i < floats.size(); i++) {
                vector[i] = floats.get(i);
            }
            return vector;
        } catch (Exception e) {
            log.error("Embedding API call failed: url={}", url, e);
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service unavailable");
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        String url = getApiUrl();
        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            List<List<Float>> embeddings = (List<List<Float>>) response.getBody().get("embeddings");
            List<float[]> list =new ArrayList<>();
            for (List<Float> embedding : embeddings) {
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i);
                }
                list.add(vector);
            }
            return list;
        } catch (Exception e) {
            log.error("Embedding API call failed: url={}", url, e);
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service unavailable");
        }
    }

    public static void main(String[] args) {
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:5001/embed";

        Map<String, Object> body = new HashMap<>();
        body.put("texts", Arrays.asList("xxx", "yyy"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = rt.postForEntity(url, entity, Map.class);

        // FastAPI 返回的是 embeddings，不是 vector
        List<List<Float>> embeddings = (List<List<Float>>) response.getBody().get("embeddings");

        System.out.println(embeddings);
    }
}
