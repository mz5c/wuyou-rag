package com.wuyou.rag.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuyou.rag.audit.AuditLogService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.config.SensitiveWordFilter;
import com.wuyou.rag.entity.kb.KbChatHistory;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.entity.kb.KbConversation;
import com.wuyou.rag.mapper.KbChatHistoryMapper;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.mapper.KbConversationMapper;
import com.wuyou.rag.rag.llm.LlmService;
import com.wuyou.rag.rag.llm.QwenLlmService;
import com.wuyou.rag.rag.prompt.PromptBuilder;
import com.wuyou.rag.rag.search.HybridSearchService;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingChatService {

    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final String CACHE_KEY_PREFIX = "rag:qa:cache:";
    private static final long CACHE_TTL_SECONDS = 3600;

    private static final String FALLBACK_ANSWER = "抱歉，AI 服务暂时不可用，请稍后再试。";
    private static final long TOKEN_DELAY_MS = 30;

    private final HybridSearchService hybridSearchService;
    private final PromptBuilder promptBuilder;
    private final QwenLlmService llmService;
    private final KbConfigMapper kbConfigMapper;
    private final KbChatHistoryMapper chatHistoryMapper;
    private final KbConversationMapper conversationMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final AuditLogService auditLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;

    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    public SseEmitter streamChat(Long userId, Long conversationId, String question) {
        long startTime = System.currentTimeMillis();

        // 1. Sensitive word check
        if (sensitiveWordFilter.containsSensitiveWord(question)) {
            log.warn("Sensitive word detected in streaming question: userId={}", userId);
            return sendErrorEvent("提问包含敏感词，请修改后重试");
        }

        // 2. Conversation management
        final Long resolvedConversationId = resolveConversation(userId, conversationId, question);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        streamingExecutor.execute(() -> {
            try {
                // 3-5. Hybrid search (Milvus + ES BM25 + RRF)
                Long kbId = null;
                if (resolvedConversationId != null) {
                    KbConversation conv = conversationMapper.selectById(resolvedConversationId);
                    if (conv != null) kbId = conv.getKbId();
                }
                log.debug("Hybrid searching for streaming, kbId={}", kbId);
                List<HybridSearchService.SearchResult> searchResults = hybridSearchService.search(question, kbId);

                List<Long> chunkIds = searchResults.stream().map(HybridSearchService.SearchResult::chunkId).collect(Collectors.toList());
                List<String> contextChunks = searchResults.stream().map(HybridSearchService.SearchResult::content).collect(Collectors.toList());
                List<ChatService.SourceDoc> sources = searchResults.stream()
                        .map(r -> new ChatService.SourceDoc(r.chunkId(), r.content(), r.docTitle(), r.docUrl()))
                        .collect(Collectors.toList());

                // 6. Send sources event FIRST, so frontend can render citations
                String sourcesJson = objectMapper.writeValueAsString(sources);
                emitter.send(SseEmitter.event()
                        .name("sources")
                        .data(sourcesJson));

                // 7. Get conversation history (last N rounds)
                List<KbChatHistory> chatHistory = chatHistoryMapper.selectList(
                        Wrappers.<KbChatHistory>lambdaQuery()
                                .eq(KbChatHistory::getConversationId, resolvedConversationId)
                                .orderByDesc(KbChatHistory::getCreateTime)
                                .last("LIMIT 20"));
                Collections.reverse(chatHistory);

                // 8. Build messages with history
                List<LlmService.Message> messages = promptBuilder.buildMessages(question, contextChunks, chatHistory);

                // 9. LLM call (non-streaming for now, simulate token streaming)
                LlmService.ChatResult result = llmService.chat(messages);
                String fullAnswer = result.answer();
                String reasoningContent = result.reasoningContent();

                // 10. Send reasoning content event
                if (reasoningContent != null) {
                    JSONObject reasoningEvent = new JSONObject();
                    reasoningEvent.put("content", reasoningContent);
                    emitter.send(SseEmitter.event()
                            .name("reasoning")
                            .data(reasoningEvent.toJSONString()));
                }

                // 11. Stream answer token by token
                for (char c : fullAnswer.toCharArray()) {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(String.valueOf(c)));
                    Thread.sleep(TOKEN_DELAY_MS);
                }

                // 12. Send done event with elapsed time
                long elapsed = System.currentTimeMillis() - startTime;
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"elapsedMs\": " + elapsed + "}"));

                // 13. Save chat history
                String usedChunkIds = chunkIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                KbChatHistory history = new KbChatHistory();
                history.setConversationId(resolvedConversationId);
                history.setUserId(userId);
                history.setQuestion(sensitiveWordFilter.filter(question));
                history.setAnswer(fullAnswer);
                history.setAnswerType("llm");
                history.setReasoningContent(reasoningContent);
                history.setUsedChunkIds(usedChunkIds);
                history.setSources(sourcesJson);
                history.setElapsedMs((int) elapsed);
                history.setCreateTime(LocalDateTime.now());
                chatHistoryMapper.insert(history);

                // Update conversation message count
                KbConversation conv = conversationMapper.selectById(resolvedConversationId);
                if (conv != null) {
                    conv.setMessageCount(conv.getMessageCount() + 1);
                    conv.setUpdateTime(LocalDateTime.now());
                    conversationMapper.updateById(conv);
                }

                // 14. Cache hot QA (skip fallback error messages)
                if (!fullAnswer.contains(FALLBACK_ANSWER)) {
                    long cacheTtl = getCacheTtl();
                    String cacheKey = CACHE_KEY_PREFIX + md5Hex(question);
                    redisTemplate.opsForValue().set(cacheKey, fullAnswer, cacheTtl, TimeUnit.SECONDS);
                    log.debug("Cached streaming QA response: key={}, ttl={}s", cacheKey, cacheTtl);
                }

                // 15. Audit log
                String ip = normalizeIp(httpServletRequest.getRemoteAddr());
                String userAgent = httpServletRequest.getHeader("User-Agent");
                String detail = "对话ID: " + resolvedConversationId + ", 问题长度: " + question.length();
                auditLogService.log(userId, "user", "CHAT", detail, ip, userAgent);

                emitter.complete();
                log.info("Streaming chat completed: conversationId={}", resolvedConversationId);

            } catch (Exception e) {
                log.error("Streaming chat failed: userId={}, conversationId={}", userId, resolvedConversationId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("抱歉，处理出错，请稍后再试"));
                } catch (IOException ex) {
                    log.warn("Failed to send error event to client", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Resolve conversation: auto-create if conversationId is null,
     * verify ownership if provided.
     */
    private Long resolveConversation(Long userId, Long conversationId, String question) {
        if (conversationId == null) {
            String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
            KbConversation conversation = new KbConversation();
            conversation.setUserId(userId);
            conversation.setTitle(title);
            conversation.setMessageCount(0);
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.insert(conversation);
            log.info("Auto-created conversation: id={}, userId={}", conversation.getId(), userId);
            return conversation.getId();
        }

        KbConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "对话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问此对话");
        }
        return conversationId;
    }

    /**
     * Create an SseEmitter that immediately sends an error event and completes.
     */
    private SseEmitter sendErrorEvent(String message) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(message));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private long getCacheTtl() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "cache.ttl_hot_qa"));
        if (config != null && config.getConfigValue() != null) {
            try {
                return Long.parseLong(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid cache.ttl_hot_qa config: {}", config.getConfigValue());
            }
        }
        return CACHE_TTL_SECONDS;
    }

    private String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String normalizeIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
