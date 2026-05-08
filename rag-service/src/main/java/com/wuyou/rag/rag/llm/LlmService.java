package com.wuyou.rag.rag.llm;

import java.util.List;

public interface LlmService {

    ChatResult chat(List<Message> messages);

    record Message(String role, String content) {}

    record ChatResult(String answer, String reasoningContent) {}
}
