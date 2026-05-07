package com.wuyou.rag.rag.embedding;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
        JSONObject body = new JSONObject();
        body.put("texts", Collections.singletonList(text));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
            ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, entity, JSONObject.class);
            JSONObject respBody = response.getBody();
            if (respBody == null) {
                throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service returned empty body");
            }
            JSONArray embeddings = respBody.getJSONArray("embeddings");
            if (embeddings == null || embeddings.isEmpty()) {
                throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service returned empty embeddings");
            }
            JSONArray floats = embeddings.getJSONArray(0);
            float[] vector = new float[floats.size()];
            for (int i = 0; i < floats.size(); i++) {
                vector[i] = floats.getFloatValue(i);
            }
            return vector;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding API call failed: url={}", url, e);
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service unavailable");
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        String url = getApiUrl();
        JSONObject body = new JSONObject();
        body.put("texts", texts);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
            ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, entity, JSONObject.class);
            JSONObject respBody = response.getBody();
            if (respBody == null) {
                throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service returned empty body");
            }
            JSONArray embeddings = respBody.getJSONArray("embeddings");
            if (embeddings == null || embeddings.isEmpty()) {
                throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service returned empty embeddings");
            }
            List<float[]> list = new ArrayList<>();
            for (int i = 0; i < embeddings.size(); i++) {
                JSONArray embedding = embeddings.getJSONArray(i);
                float[] vector = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    vector[j] = embedding.getFloatValue(j);
                }
                list.add(vector);
            }
            return list;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding API call failed: url={}", url, e);
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service unavailable");
        }
    }
}
