package com.wuyou.rag.controller;

import com.wuyou.rag.chat.ChatService;
import com.wuyou.rag.chat.StreamingChatService;
import com.wuyou.rag.entity.kb.KbConversation;
import com.wuyou.rag.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "问答服务")
public class ChatController {

    private final ChatService chatService;
    private final StreamingChatService streamingChatService;
    private final HttpServletRequest httpServletRequest;

    @PostMapping
    public Result<ChatService.ChatResponse> chat(@RequestBody @Valid ChatRequest request,
                                                  @AuthenticationPrincipal Long userId) {
        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return chatService.chat(userId, request.getConversationId(), request.getQuestion(), ip, userAgent);
    }

    @PostMapping("/conversation")
    public Result<KbConversation> createConversation(@RequestBody CreateConversationRequest request,
                                                      @AuthenticationPrincipal Long userId) {
        return chatService.createConversation(userId, request.getTitle(), request.getKbId());
    }

    @GetMapping("/conversation")
    public Result<List<KbConversation>> listConversations(@AuthenticationPrincipal Long userId) {
        return chatService.listConversations(userId);
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public Result<?> getMessages(@PathVariable Long conversationId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "100") int size) {
        return chatService.getMessages(conversationId, page, size);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId,
                                            @AuthenticationPrincipal Long userId) {
        return chatService.deleteConversation(conversationId, userId);
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(@RequestBody @Valid FeedbackRequest request) {
        return chatService.feedback(request.getHistoryId(), request.getFeedback(), request.getComment());
    }

    // ---- SSE Streaming ----

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) Long conversationId,
                             @RequestParam String question,
                             @AuthenticationPrincipal Long userId) {
        return streamingChatService.streamChat(userId, conversationId, question);
    }

    // ---- DTOs ----

    @Data
    public static class ChatRequest {
        private Long conversationId;

        @NotBlank(message = "问题不能为空")
        private String question;
    }

    @Data
    public static class CreateConversationRequest {
        private String title;
        private Long kbId;
    }

    @Data
    public static class FeedbackRequest {
        @NotNull(message = "消息ID不能为空")
        private Long historyId;

        @NotNull(message = "反馈类型不能为空")
        private Integer feedback;

        private String comment;
    }
}
