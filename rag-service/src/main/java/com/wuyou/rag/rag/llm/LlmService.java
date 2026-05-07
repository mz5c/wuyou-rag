package com.wuyou.rag.rag.llm;

import java.util.List;

public interface LlmService {

    String chat(List<Message> messages);

    record Message(String role, String content) {}
}
