package com.wuyou.rag.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuyou.rag.audit.AuditLogService;
import com.wuyou.rag.chat.ChatService;
import com.wuyou.rag.config.SensitiveWordFilter;
import com.wuyou.rag.entity.kb.KbChatHistory;
import com.wuyou.rag.entity.kb.KbChunk;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.entity.kb.KbConversation;
import com.wuyou.rag.entity.kb.KbDocument;
import com.wuyou.rag.entity.sys.SysUser;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbChatHistoryMapper;
import com.wuyou.rag.mapper.KbChunkMapper;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.mapper.KbConversationMapper;
import com.wuyou.rag.mapper.KbDocumentMapper;
import com.wuyou.rag.mapper.SysUserMapper;
import com.wuyou.rag.rag.embedding.BgeEmbeddingService;
import com.wuyou.rag.rag.llm.LlmService;
import com.wuyou.rag.rag.llm.QwenLlmService;
import com.wuyou.rag.rag.prompt.PromptBuilder;
import com.wuyou.rag.rag.vector.MilvusVectorService;
import com.wuyou.rag.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String FALLBACK_ANSWER = "抱歉，AI 服务暂时不可用，请稍后再试。";
    private static final String CACHE_KEY_PREFIX = "rag:qa:cache:";
    private static final long CACHE_TTL_SECONDS = 3600;
    private static final int DEFAULT_TOP_K = 5;

    private final KbConversationMapper conversationMapper;
    private final KbChatHistoryMapper chatHistoryMapper;
    private final KbChunkMapper kbChunkMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbConfigMapper kbConfigMapper;
    private final SysUserMapper sysUserMapper;
    private final BgeEmbeddingService embeddingService;
    private final MilvusVectorService vectorService;
    private final QwenLlmService llmService;
    private final PromptBuilder promptBuilder;
    private final AuditLogService auditLogService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ---- Conversation Management ----

    @Override
    public Result<KbConversation> createConversation(Long userId, String title, Long kbId) {
        KbConversation conversation = new KbConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setKbId(kbId);
        conversation.setMessageCount(0);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        log.info("Conversation created: id={}, userId={}", conversation.getId(), userId);
        return Result.success(conversation);
    }

    @Override
    public Result<List<KbConversation>> listConversations(Long userId) {
        LambdaQueryWrapper<KbConversation> wrapper = Wrappers.<KbConversation>lambdaQuery()
                .eq(KbConversation::getUserId, userId)
                .orderByDesc(KbConversation::getUpdateTime);
        List<KbConversation> list = conversationMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteConversation(Long conversationId, Long userId) {
        KbConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "对话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            return Result.fail(ErrorCode.FORBIDDEN.getCode(), "无权删除此对话");
        }
        // Hard delete conversation
        conversationMapper.deleteById(conversationId);
        // Hard delete all related chat history
        chatHistoryMapper.delete(
                Wrappers.<KbChatHistory>lambdaQuery().eq(KbChatHistory::getConversationId, conversationId));
        log.info("Conversation deleted: id={}, userId={}", conversationId, userId);
        return Result.success(null);
    }

    // ---- Chat ----

    @Override
    public Result<ChatResponse> chat(Long userId, Long conversationId, String question, String ip, String userAgent) {
        long startTime = System.currentTimeMillis();

        // 1. DFA sensitive word check
        if (sensitiveWordFilter.containsSensitiveWord(question)) {
            log.warn("Sensitive word detected in question: userId={}", userId);
            return Result.fail(ErrorCode.SENSITIVE_WORD.getCode(), ErrorCode.SENSITIVE_WORD.getMessage());
        }

        // 2. Redis cache check
        String cacheKey = CACHE_KEY_PREFIX + md5Hex(question);
        String cachedAnswer = redisTemplate.opsForValue().get(cacheKey);
        if (cachedAnswer != null) {
            log.info("Cache hit for question: md5={}", md5Hex(question));
            long elapsed = System.currentTimeMillis() - startTime;
            ChatResponse cachedResponse = new ChatResponse(conversationId, null,
                    cachedAnswer, null, new ArrayList<>(), (int) elapsed);
            return Result.success(cachedResponse);
        }

        // Auto-create conversation if conversationId is null
        if (conversationId == null) {
            String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
            Result<KbConversation> convResult = createConversation(userId, title, null);
            KbConversation conversation = convResult.getData();
            if (conversation == null) {
                return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "创建对话失败");
            }
            conversationId = conversation.getId();
        } else {
            // Verify conversation exists and belongs to user
            KbConversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                return Result.fail(ErrorCode.NOT_FOUND.getCode(), "对话不存在");
            }
            if (!conversation.getUserId().equals(userId)) {
                return Result.fail(ErrorCode.FORBIDDEN.getCode(), "无权访问此对话");
            }
        }

        try {
            // 3. Embedding
            log.debug("Generating embedding for question");
            float[] queryVector = embeddingService.embed(question);

            // 4. Milvus search
            int topK = getTopK();
            log.debug("Searching Milvus with topK={}", topK);
            List<Long> chunkIds = vectorService.search(queryVector, topK);

            // 5. Get chunk content and document info
            List<SourceDoc> sources = new ArrayList<>();
            List<String> contextChunks = new ArrayList<>();
            for (Long chunkId : chunkIds) {
                KbChunk chunk = kbChunkMapper.selectById(chunkId);
                if (chunk == null) {
                    continue;
                }
                contextChunks.add(chunk.getChunkContent());

                String docTitle = null;
                String docUrl = null;
                if (chunk.getDocId() != null) {
                    KbDocument doc = kbDocumentMapper.selectById(chunk.getDocId());
                    if (doc != null) {
                        docTitle = doc.getTitle();
                        docUrl = doc.getFileUrl();
                    }
                }
                sources.add(new SourceDoc(chunkId, chunk.getChunkContent(), docTitle, docUrl));
            }

            // 6. Get conversation history (last N rounds)
            List<KbChatHistory> chatHistory = chatHistoryMapper.selectList(
                    Wrappers.<KbChatHistory>lambdaQuery()
                            .eq(KbChatHistory::getConversationId, conversationId)
                            .orderByDesc(KbChatHistory::getCreateTime)
                            .last("LIMIT 20"));
            Collections.reverse(chatHistory);

            // 7. Build messages with history
            List<LlmService.Message> messages = promptBuilder.buildMessages(question, contextChunks, chatHistory);

            // 8. LLM call
            LlmService.ChatResult result = llmService.chat(messages);
            String answer = result.answer();
            String reasoningContent = result.reasoningContent();

            // 9. Save chat history and update conversation
            long elapsed = System.currentTimeMillis() - startTime;

            String usedChunkIds = chunkIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String sourcesJson;
            try {
                sourcesJson = objectMapper.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources JSON", e);
                sourcesJson = "[]";
            }

            KbChatHistory history = new KbChatHistory();
            history.setConversationId(conversationId);
            history.setUserId(userId);
            history.setQuestion(sensitiveWordFilter.filter(question));
            history.setAnswer(answer);
            history.setAnswerType("llm");
            history.setUsedChunkIds(usedChunkIds);
            history.setSources(sourcesJson);
            history.setElapsedMs((int) elapsed);
            history.setCreateTime(LocalDateTime.now());
            chatHistoryMapper.insert(history);

            // Update conversation message count
            KbConversation conversation = conversationMapper.selectById(conversationId);
            if (conversation != null) {
                conversation.setMessageCount(conversation.getMessageCount() + 1);
                conversation.setUpdateTime(LocalDateTime.now());
                conversationMapper.updateById(conversation);
            }

            // 10. Cache hot QA (skip fallback error messages)
            if (!answer.contains(FALLBACK_ANSWER)) {
                long cacheTtl = getCacheTtl();
                redisTemplate.opsForValue().set(cacheKey, answer, cacheTtl, TimeUnit.SECONDS);
                log.debug("Cached QA response: key={}, ttl={}s", cacheKey, cacheTtl);
            }

            // 11. Audit log
            String username = getUsername(userId);
            String detail = "对话ID: " + conversationId + ", 问题长度: " + question.length();
            auditLogService.log(userId, username, "CHAT", detail, ip, userAgent);

            // 12. Return response
            ChatResponse response = new ChatResponse(
                    conversationId, history.getId(), answer, reasoningContent, sources, (int) elapsed);
            return Result.success(response);

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chat processing failed: userId={}, conversationId={}", userId, conversationId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            ChatResponse response = new ChatResponse(conversationId, null, FALLBACK_ANSWER,
                    null, new ArrayList<>(), (int) elapsed);
            return Result.success(response);
        }
    }

    // ---- History ----

    @Override
    public Result<?> getMessages(Long conversationId, int page, int size) {
        Page<KbChatHistory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KbChatHistory> wrapper = Wrappers.<KbChatHistory>lambdaQuery()
                .eq(KbChatHistory::getConversationId, conversationId)
                .orderByAsc(KbChatHistory::getCreateTime);
        Page<KbChatHistory> result = chatHistoryMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    // ---- Feedback ----

    @Override
    public Result<Void> feedback(Long historyId, Integer feedback, String comment) {
        KbChatHistory history = chatHistoryMapper.selectById(historyId);
        if (history == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "消息记录不存在");
        }
        history.setFeedback(feedback);
        history.setFeedbackComment(comment);
        chatHistoryMapper.updateById(history);
        log.info("Feedback recorded: historyId={}, feedback={}", historyId, feedback);
        return Result.success(null);
    }

    // ---- Private Helpers ----

    private int getTopK() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "search.top_k"));
        if (config != null && config.getConfigValue() != null) {
            try {
                return Integer.parseInt(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid search.top_k config: {}", config.getConfigValue());
            }
        }
        return DEFAULT_TOP_K;
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

    private String getUsername(Long userId) {
        if (userId == null) {
            return "unknown";
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getUsername() : "unknown";
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
}
