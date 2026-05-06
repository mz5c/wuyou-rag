package com.wuyou.rag.chat;

import com.wuyou.rag.entity.kb.KbConversation;
import com.wuyou.rag.result.Result;

import java.util.List;

public interface ChatService {

    // Conversation management
    Result<KbConversation> createConversation(Long userId, String title, Long kbId);

    Result<List<KbConversation>> listConversations(Long userId);

    Result<Void> deleteConversation(Long conversationId, Long userId);

    // Chat
    Result<ChatResponse> chat(Long userId, Long conversationId, String question, String ip, String userAgent);

    // History
    Result<?> getMessages(Long conversationId, int page, int size);

    // Feedback
    Result<Void> feedback(Long historyId, Integer feedback, String comment);

    record ChatResponse(Long conversationId, Long historyId, String answer,
                        List<SourceDoc> sources, int elapsedMs) {}

    record SourceDoc(Long chunkId, String content, String docTitle, String docUrl) {}
}
