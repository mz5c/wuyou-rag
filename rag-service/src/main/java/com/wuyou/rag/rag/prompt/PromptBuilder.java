package com.wuyou.rag.rag.prompt;

import com.wuyou.rag.entity.kb.KbChatHistory;
import com.wuyou.rag.rag.llm.LlmService.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = "你是一个专业的企业知识库问答助手。请基于以下提供的资料，准确、简洁地回答用户的问题。\n" +
            "如果资料不足以回答问题，请如实告知，不要编造信息。\n" +
            "请注明你的回答引用了哪些资料。\n";

    private static final int MAX_HISTORY_ROUNDS = 10;

    public List<Message> buildMessages(String question, List<String> contextChunks, List<KbChatHistory> history) {
        List<Message> messages = new ArrayList<>();

        // System message
        messages.add(new Message("system", SYSTEM_PROMPT));

        // History messages (alternating user/assistant, limited to last N rounds)
        if (history != null && !history.isEmpty()) {
            for (KbChatHistory h : history) {
                messages.add(new Message("user", h.getQuestion()));
                if (h.getAnswer() != null) {
                    messages.add(new Message("assistant", h.getAnswer()));
                }
            }
        }

        // Current user message with context
        StringBuilder userContent = new StringBuilder();
        userContent.append("相关资料：\n");
        userContent.append("---\n");
        IntStream.range(0, contextChunks.size()).forEach(i ->
                userContent.append("[").append(i + 1).append("] ").append(contextChunks.get(i)).append("\n"));
        userContent.append("---\n\n");
        userContent.append("问题：\n");
        userContent.append(question);

        messages.add(new Message("user", userContent.toString()));

        return messages;
    }

    /**
     * @deprecated 保留兼容，新代码请使用 {@link #buildMessages(String, List, List)}
     */
    @Deprecated
    public String buildPrompt(String question, List<String> contextChunks) {
        List<Message> messages = buildMessages(question, contextChunks, Collections.emptyList());
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append("[").append(msg.role()).append("] ").append(msg.content()).append("\n\n");
        }
        return sb.toString();
    }

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
