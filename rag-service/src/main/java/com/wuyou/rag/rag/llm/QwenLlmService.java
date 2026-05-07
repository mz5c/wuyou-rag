package com.wuyou.rag.rag.llm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenLlmService implements LlmService {

    private final RestTemplate restTemplate;

    @Value("${llm.api-url}")
    private String llmApiUrl;

    @Value("${llm.model-name}")
    private String llmModelName;

    @Value("${llm.api-key}")
    private String llmApiKey;

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "chatFallback")
    @RateLimiter(name = "llmService")
    @Retry(name = "llmService")
    public String chat(List<Message> messages) {
        String url = llmApiUrl;

        JSONArray messagesArray = new JSONArray();
        for (Message msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.role());
            msgObj.put("content", msg.content());
            messagesArray.add(msgObj);
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", llmModelName);
        requestBody.put("messages", messagesArray);

        log.info("Calling LLM: url={}, model={}, messageCount={}", url, llmModelName, messages.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmApiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, entity, JSONObject.class);
        JSONObject body = response.getBody();

        if (body == null) {
            throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "LLM返回为空");
        }

        JSONArray choices = body.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            if (message != null) {
                String content = message.getString("content");
                if (content != null) {
                    return content;
                }
            }
        }

        throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "LLM返回为空");
    }

    @SuppressWarnings("unused")
    public String chatFallback(List<Message> messages, Throwable t) {
        log.error("LLM call failed after retries: {}", t.getMessage());
        return "抱歉，AI 服务暂时不可用，请稍后再试。";
    }
}
