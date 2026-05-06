package com.wuyou.rag.rag.llm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbConfigMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenLlmService implements LlmService {

    private final RestTemplate restTemplate;
    private final KbConfigMapper kbConfigMapper;

    private String getApiUrl() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "llm.api_url"));
        return config != null ? config.getConfigValue() : "http://localhost:8000/v1";
    }

    private String getModelName() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "llm.model_name"));
        return config != null ? config.getConfigValue() : "qwen";
    }

    private double getTemperature() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "llm.temperature"));
        return config != null ? Double.parseDouble(config.getConfigValue()) : 0.7;
    }

    @SuppressWarnings("unused")
    private int getTimeout() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "llm.timeout"));
        return config != null ? Integer.parseInt(config.getConfigValue()) : 30000;
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "chatFallback")
    @RateLimiter(name = "llmService")
    @Retry(name = "llmService")
    public String chat(String prompt) {
        String url = getApiUrl() + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", getModelName());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", getTemperature());

        log.info("Calling LLM: url={}, model={}", url, getModelName());

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
        Map body = response.getBody();

        if (body == null) {
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "LLM返回为空");
        }

        List<Map> choices = (List<Map>) body.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map message = (Map) choices.get(0).get("message");
            if (message != null) {
                String content = (String) message.get("content");
                if (content != null) {
                    return content;
                }
            }
        }

        throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "LLM返回为空");
    }

    @SuppressWarnings("unused")
    public String chatFallback(String prompt, Throwable t) {
        log.error("LLM call failed after retries: {}", t.getMessage());
        return "抱歉，AI 服务暂时不可用，请稍后再试。";
    }
}
