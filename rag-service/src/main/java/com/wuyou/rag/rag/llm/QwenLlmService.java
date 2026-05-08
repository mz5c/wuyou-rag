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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenLlmService implements LlmService {

    private static final Pattern REASONING_PATTERN = Pattern.compile(
            "\\s*<reasoning>([\\s\\S]*?)</reasoning>\\s*|([\\s\\S]*?)</think>\\s*",
            Pattern.CASE_INSENSITIVE);

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
    public ChatResult chat(List<Message> messages) {
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
                    return parseReasoningContent(content);
                }
            }
        }

        throw new BizException(ErrorCode.LLM_CIRCUIT_BROKEN, "LLM返回为空");
    }

    ChatResult parseReasoningContent(String rawResponse) {
        Matcher matcher = REASONING_PATTERN.matcher(rawResponse);
        String reasoningContent = null;
        if (matcher.find()) {
            reasoningContent = matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
        }
        String cleanAnswer = REASONING_PATTERN.matcher(rawResponse).replaceAll("").trim();
        if (cleanAnswer.isEmpty()) {
            cleanAnswer = rawResponse;
        }
        return new ChatResult(cleanAnswer, reasoningContent);
    }

    String cleanAnswerForContext(String answer) {
        if (answer == null) return null;
        return REASONING_PATTERN.matcher(answer).replaceAll("").trim();
    }

    @SuppressWarnings("unused")
    public ChatResult chatFallback(List<Message> messages, Throwable t) {
        log.error("LLM call failed after retries: {}", t.getMessage());
        return new ChatResult("抱歉，AI 服务暂时不可用，请稍后再试。", null);
    }
}
