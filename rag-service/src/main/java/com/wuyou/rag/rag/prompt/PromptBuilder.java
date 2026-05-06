package com.wuyou.rag.rag.prompt;

import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = "你是一个专业的企业知识库问答助手。请基于以下提供的资料，准确、简洁地回答用户的问题。\n" +
            "如果资料不足以回答问题，请如实告知，不要编造信息。\n" +
            "请注明你的回答引用了哪些资料。\n";

    private final KbConfigMapper kbConfigMapper;

    public String buildPrompt(String question, List<String> contextChunks) {
        StringBuilder sb = new StringBuilder();

        sb.append(SYSTEM_PROMPT).append("\n\n");

        sb.append("相关资料：\n");
        sb.append("---\n");
        IntStream.range(0, contextChunks.size()).forEach(i -> {
            sb.append("[").append(i + 1).append("] ").append(contextChunks.get(i)).append("\n");
        });
        sb.append("---\n\n");

        sb.append("问题：\n");
        sb.append(question);

        return sb.toString();
    }

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
