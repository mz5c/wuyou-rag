package com.wuyou.rag.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensitiveWordFilter {

    private final KbConfigMapper kbConfigMapper;

    private String getSensitiveWordsConfig() {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "sensitive_words"));
        return config != null ? config.getConfigValue() : "";
    }

    public String filter(String text) {
        String sensitiveWords = getSensitiveWordsConfig();
        if (sensitiveWords.isEmpty()) {
            return text;
        }
        String[] words = sensitiveWords.split(",");
        String result = text;
        for (String word : words) {
            String trimmed = word.trim();
            if (!trimmed.isEmpty() && result.contains(trimmed)) {
                result = result.replace(trimmed, "***");
            }
        }
        return result;
    }

    public boolean containsSensitiveWord(String text) {
        String sensitiveWords = getSensitiveWordsConfig();
        if (sensitiveWords.isEmpty()) {
            return false;
        }
        String[] words = sensitiveWords.split(",");
        for (String word : words) {
            if (!word.trim().isEmpty() && text.contains(word.trim())) {
                return true;
            }
        }
        return false;
    }
}
