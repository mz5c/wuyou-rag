package com.wuyou.rag.rag.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbChatHistory;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.rag.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private static final String CONFIG_KEY_ENABLED = "query.rewrite.enabled";
    static final int MAX_HISTORY_ROUNDS = 3;

    private final LlmService llmService;
    private final KbConfigMapper kbConfigMapper;

    public boolean isEnabled() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, CONFIG_KEY_ENABLED));
        return config == null || Boolean.parseBoolean(config.getConfigValue());
    }

    public String rewrite(String question, List<KbChatHistory> history) {
        if (!isEnabled() || history == null || history.isEmpty()) {
            return question;
        }

        List<KbChatHistory> recent = history.size() > MAX_HISTORY_ROUNDS
                ? history.subList(history.size() - MAX_HISTORY_ROUNDS, history.size())
                : history;

        String historyText = recent.stream()
                .map(h -> "用户：" + h.getQuestion() + "\n助手：" + (h.getAnswer() != null ? h.getAnswer() : ""))
                .collect(Collectors.joining("\n"));

        String prompt = "请根据对话历史，将用户的最新问题重写为一个独立、自包含的问题。\n" +
                "保持原意，不要编造不存在的信息。\n\n" +
                "对话历史：\n" + historyText + "\n\n" +
                "用户最新问题：" + question + "\n\n" +
                "重写后的独立问题：";

        try {
            LlmService.ChatResult result = llmService.chat(List.of(
                    new LlmService.Message("user", prompt)));
            String rewritten = result.answer().strip();
            if (rewritten.isEmpty()) {
                return question;
            }
            log.info("Query rewritten: '{}' -> '{}'", question, rewritten);
            return rewritten;
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original question: {}", e.getMessage());
            return question;
        }
    }
}
