package com.mayday.ai.factory.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mayday.ai.factory.AiModelFactory;
import com.mayday.ai.mapper.AiKeyMapper;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mayday.ai.security.KeyCipher;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 模型工厂实现：
 * - 从 ai_key 取密文 key -> 解密成明文
 * - 根据 provider 构建 LangChain4j 模型
 * - 使用 Caffeine 缓存，避免每次请求 new
 */
@Service
@RequiredArgsConstructor
public class AiModelFactoryImpl implements AiModelFactory {

    private final AiKeyMapper aiKeyMapper;
    private final KeyCipher keyCipher;

    /** 缓存：configId:version -> model */
    private final Cache<String, ChatLanguageModel> cache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

    @Override
    public ChatLanguageModel getOrCreate(AiConfigEntity cfg) {
        String cacheKey = cfg.getId() + ":" + (cfg.getVersion() == null ? 1 : cfg.getVersion());
        return cache.get(cacheKey, k -> build(cfg));
    }

    private ChatLanguageModel build(AiConfigEntity cfg) {
        // 1) 取 key（密文）并解密
        AiKeyEntity key = aiKeyMapper.selectOneById(cfg.getKeyId());
        if (key == null) {
            throw new IllegalStateException("ai_key not found: keyId=" + cfg.getKeyId());
        }
        String apiKeyPlain = keyCipher.decrypt(key.getKeyCipher());

        // 2) 构建模型
        String provider = cfg.getProvider() == null ? "" : cfg.getProvider().trim().toLowerCase();

        // 温度：默认 0.7
        double temperature = (cfg.getTemperature() == null) ? 0.7 : cfg.getTemperature().doubleValue();

        if ("google".equals(provider) || "gemini".equals(provider)) {
            return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKeyPlain)
                .modelName(cfg.getModelName())
                .temperature(temperature)
                .build();
        }

        // openai / deepseek / 任何 openai-compatible 都走这里（靠 baseUrl 区分）
        if ("openai".equals(provider) || "deepseek".equals(provider) || "kimi".equals(provider) || "qwen".equals(provider)) {
            var b = OpenAiChatModel.builder()
                .apiKey(apiKeyPlain)
                .modelName(cfg.getModelName())
                .temperature(temperature);

            // baseUrl 可选：用于代理或国产模型 OpenAI 兼容地址
            if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
                b.baseUrl(cfg.getBaseUrl().trim());
            }
            return b.build();
        }

        throw new UnsupportedOperationException("Unsupported provider: " + cfg.getProvider());
    }
}