package com.wuyou.rag.rag.embedding;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        body.put("text", text);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            List<Double> vectorList = (List<Double>) response.getBody().get("vector");
            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.error("Embedding API call failed: url={}", url, e);
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "Embedding service unavailable");
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }
}
