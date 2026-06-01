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
import com.wuyou.rag.rag.query.QueryRewriter;
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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final HybridSearchService hybridSearchService;
    private final PromptBuilder promptBuilder;
    private final QwenLlmService llmService;
    private final QueryRewriter queryRewriter;
    private final KbConfigMapper kbConfigMapper;
    private final KbChatHistoryMapper chatHistoryMapper;
    private final KbConversationMapper conversationMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final AuditLogService auditLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;

    private final ExecutorService streamingExecutor = Executors.newFixedThreadPool(20);

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
                Long kbId = null;
                if (resolvedConversationId != null) {
                    KbConversation conv = conversationMapper.selectById(resolvedConversationId);
                    if (conv != null) kbId = conv.getKbId();
                }

                // 3. Get conversation history
                List<KbChatHistory> chatHistory = chatHistoryMapper.selectList(
                        Wrappers.<KbChatHistory>lambdaQuery()
                                .eq(KbChatHistory::getConversationId, resolvedConversationId)
                                .orderByDesc(KbChatHistory::getCreateTime)
                                .last("LIMIT 20"));
                Collections.reverse(chatHistory);

                // 4. Query rewriting
                String searchQuestion = question;
                if (!chatHistory.isEmpty()) {
                    searchQuestion = queryRewriter.rewrite(question, chatHistory);
                }

                // 5. Hybrid search (Milvus + ES BM25 + RRF + Reranker)
                log.debug("Hybrid searching for streaming, kbId={}, rewritten={}",
                        kbId, !searchQuestion.equals(question));
                List<HybridSearchService.SearchResult> searchResults =
                        hybridSearchService.search(searchQuestion, kbId);

                List<Long> chunkIds = searchResults.stream()
                        .map(HybridSearchService.SearchResult::chunkId).collect(Collectors.toList());
                List<String> contextChunks = searchResults.stream()
                        .map(HybridSearchService.SearchResult::content).collect(Collectors.toList());
                List<ChatService.SourceDoc> sources = searchResults.stream()
                        .map(r -> new ChatService.SourceDoc(r.chunkId(), r.content(), r.docTitle(), r.docUrl()))
                        .collect(Collectors.toList());

                // 6. Send sources event FIRST
                String sourcesJson = objectMapper.writeValueAsString(sources);
                emitter.send(SseEmitter.event()
                        .name("sources")
                        .data(sourcesJson));

                // 7. Build messages with history
                List<LlmService.Message> messages = promptBuilder.buildMessages(question, contextChunks, chatHistory);

                // 8. Real SSE streaming LLM call
                StringBuilder reasoningContent = new StringBuilder();
                final boolean[] reasoningStarted = {false};
                final AtomicBoolean sseFailed = new AtomicBoolean(false);

                llmService.streamChat(messages,
                    // onToken — called for each token from LLM
                    token -> {

                        // Detect reasoning content (content wrapped in think/reasoning tags)
                        if (token.contains("</reasoning>") || token.contains("</think>")) {
                            reasoningStarted[0] = false;
                            return;
                        }
                        if (reasoningStarted[0]) {
                            reasoningContent.append(token);
                            return;
                        }
                        if (token.contains("<reasoning>") || token.contains("<think>")) {
                            reasoningStarted[0] = true;
                            return;
                        }

                        // Skip if SSE connection lost
                        if (sseFailed.get()) {
                            return;
                        }

                        // Send token to frontend
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                        } catch (IOException e) {
                            sseFailed.set(true);
                            log.warn("SSE connection lost, stopping stream");
                        }
                    },
                    // onComplete — called when LLM finishes
                    result -> {
                        try {
                            String finalAnswer = result.answer();
                            long elapsed = System.currentTimeMillis() - startTime;

                            // Send done event
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"elapsedMs\": " + elapsed + "}"));

                            // Save chat history
                            String usedChunkIds = chunkIds.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","));

                            KbChatHistory history = new KbChatHistory();
                            history.setConversationId(resolvedConversationId);
                            history.setUserId(userId);
                            history.setQuestion(sensitiveWordFilter.filter(question));
                            history.setAnswer(finalAnswer);
                            history.setAnswerType("llm");
                            history.setReasoningContent(result.reasoningContent());
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

                            // Cache hot QA
                            if (!finalAnswer.contains(FALLBACK_ANSWER)) {
                                long cacheTtl = getCacheTtl();
                                String cacheKey = CACHE_KEY_PREFIX + md5Hex(question);
                                redisTemplate.opsForValue().set(cacheKey, finalAnswer, cacheTtl, TimeUnit.SECONDS);
                            }

                            // Audit log
                            String ip = normalizeIp(httpServletRequest.getRemoteAddr());
                            String userAgent = httpServletRequest.getHeader("User-Agent");
                            String detail = "对话ID: " + resolvedConversationId + ", 问题长度: " + question.length();
                            auditLogService.log(userId, "user", "CHAT", detail, ip, userAgent);

                            emitter.complete();
                            log.info("Streaming chat completed: conversationId={}", resolvedConversationId);

                        } catch (IOException e) {
                            log.error("Failed to complete streaming chat", e);
                            emitter.completeWithError(e);
                        }
                    },
                    // onError
                    error -> {
                        log.error("Streaming chat failed: userId={}, conversationId={}",
                                userId, resolvedConversationId, error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("抱歉，处理出错，请稍后再试"));
                        } catch (IOException ex) {
                            log.warn("Failed to send error event", ex);
                        }
                        emitter.completeWithError(error);
                    });

            } catch (Exception e) {
                log.error("Streaming chat failed: userId={}, conversationId={}",
                        userId, resolvedConversationId, e);
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
