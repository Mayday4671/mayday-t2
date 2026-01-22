package com.mayday.ai.factory.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mayday.ai.config.AiNetProperties;
import com.mayday.ai.factory.AiHttpClientFactory;
import com.mayday.ai.factory.AiModelFactory;
import com.mayday.ai.mapper.AiKeyMapper;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mayday.ai.security.KeyCipher;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.Proxy;
import java.time.Duration;

/**
 * 模型工厂实现：
 * - 从 ai_key 取密文 key -> 解密成明文
 * - 根据 provider 构建 LangChain4j 模型（同步和流式）
 * - 使用 Caffeine 缓存，避免每次请求 new
 * - 支持超时配置：优先 DB 配置 > 全局配置
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelFactoryImpl implements AiModelFactory {

    private final AiKeyMapper aiKeyMapper;
    private final KeyCipher keyCipher;
    private final AiNetProperties netProps;
    private final AiHttpClientFactory httpClientFactory;

    /** 默认超时时间（秒），当配置都未设置时使用 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /** 同步模型缓存：configId:version -> model */
    private final Cache<String, ChatLanguageModel> cache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

    /** 流式模型缓存：configId:version:streaming -> model */
    private final Cache<String, StreamingChatLanguageModel> streamingCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

    @Override
    public ChatLanguageModel getOrCreate(AiConfigEntity cfg) {
        String cacheKey = cfg.getId() + ":" + (cfg.getVersion() == null ? 1 : cfg.getVersion());
        return cache.get(cacheKey, k -> build(cfg));
    }

    /**
     * 获取或创建流式模型
     *
     * @param cfg AI 配置实体
     * @return 流式聊天模型
     */
    public StreamingChatLanguageModel getOrCreateStreaming(AiConfigEntity cfg) {
        String cacheKey = cfg.getId() + ":" + (cfg.getVersion() == null ? 1 : cfg.getVersion()) + ":streaming";
        return streamingCache.get(cacheKey, k -> buildStreaming(cfg));
    }

    /**
     * 构建同步 ChatLanguageModel
     */
    private ChatLanguageModel build(AiConfigEntity cfg) {
        AiKeyEntity key = aiKeyMapper.selectOneById(cfg.getKeyId());
        if (key == null) {
            throw new IllegalStateException("ai_key not found: keyId=" + cfg.getKeyId());
        }
        String apiKeyPlain = keyCipher.decrypt(key.getKeyCipher());

        Duration timeout = resolveTimeout(cfg);
        log.debug("构建同步 AI 模型 [provider={}, model={}], timeout={}ms",
            cfg.getProvider(), cfg.getModelName(), timeout.toMillis());

        String provider = cfg.getProvider() == null ? "" : cfg.getProvider().trim().toLowerCase();
        double temperature = (cfg.getTemperature() == null) ? 0.7 : cfg.getTemperature().doubleValue();
        Proxy proxy = httpClientFactory.buildProxyOrNull();

        if ("google".equals(provider) || "gemini".equals(provider)) {
            return buildGoogleModel(cfg, apiKeyPlain, temperature, timeout, proxy);
        }

        if ("openai".equals(provider) || "deepseek".equals(provider) || "kimi".equals(provider) || "qwen".equals(provider)) {
            return buildOpenAiCompatModel(cfg, apiKeyPlain, temperature, timeout, proxy);
        }

        throw new UnsupportedOperationException("Unsupported provider: " + cfg.getProvider());
    }

    /**
     * 构建流式 StreamingChatLanguageModel
     */
    private StreamingChatLanguageModel buildStreaming(AiConfigEntity cfg) {
        AiKeyEntity key = aiKeyMapper.selectOneById(cfg.getKeyId());
        if (key == null) {
            throw new IllegalStateException("ai_key not found: keyId=" + cfg.getKeyId());
        }
        String apiKeyPlain = keyCipher.decrypt(key.getKeyCipher());

        Duration timeout = resolveTimeout(cfg);
        log.debug("构建流式 AI 模型 [provider={}, model={}], timeout={}ms",
            cfg.getProvider(), cfg.getModelName(), timeout.toMillis());

        String provider = cfg.getProvider() == null ? "" : cfg.getProvider().trim().toLowerCase();
        double temperature = (cfg.getTemperature() == null) ? 0.7 : cfg.getTemperature().doubleValue();
        Proxy proxy = httpClientFactory.buildProxyOrNull();

        if ("google".equals(provider) || "gemini".equals(provider)) {
            return buildGoogleStreamingModel(cfg, apiKeyPlain, temperature, timeout);
        }

        if ("openai".equals(provider) || "deepseek".equals(provider) || "kimi".equals(provider) || "qwen".equals(provider)
            || "alibaba".equals(provider) || "dashscope".equals(provider)) {
            return buildOpenAiStreamingModel(cfg, apiKeyPlain, temperature, timeout, proxy);
        }

        throw new UnsupportedOperationException("Unsupported streaming provider: " + cfg.getProvider());
    }

    /**
     * 计算超时时间
     */
    private Duration resolveTimeout(AiConfigEntity cfg) {
        if (cfg.getTimeoutMs() != null && cfg.getTimeoutMs() > 0) {
            return Duration.ofMillis(cfg.getTimeoutMs());
        }
        if (netProps.getTimeoutSeconds() != null && netProps.getTimeoutSeconds() > 0) {
            return Duration.ofSeconds(netProps.getTimeoutSeconds());
        }
        return Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
    }

    // ==================== 同步模型构建 ====================

    private ChatLanguageModel buildGoogleModel(AiConfigEntity cfg, String apiKeyPlain,
                                                double temperature, Duration timeout, Proxy proxy) {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temperature)
            .timeout(timeout)
            .build();
    }

    private ChatLanguageModel buildOpenAiCompatModel(AiConfigEntity cfg, String apiKeyPlain,
                                                      double temperature, Duration timeout, Proxy proxy) {
        var builder = OpenAiChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temperature)
            .timeout(timeout);

        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
            builder.baseUrl(cfg.getBaseUrl().trim());
        }
        if (proxy != null) {
            builder.proxy(proxy);
        }

        return builder.build();
    }

    // ==================== 流式模型构建 ====================

    private StreamingChatLanguageModel buildGoogleStreamingModel(AiConfigEntity cfg, String apiKeyPlain,
                                                                  double temperature, Duration timeout) {
        return GoogleAiGeminiStreamingChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temperature)
            .timeout(timeout)
            .build();
    }

    private StreamingChatLanguageModel buildOpenAiStreamingModel(AiConfigEntity cfg, String apiKeyPlain,
                                                                  double temperature, Duration timeout, Proxy proxy) {
        var builder = OpenAiStreamingChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temperature)
            .timeout(timeout);

        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
            builder.baseUrl(cfg.getBaseUrl().trim());
        }
        if (proxy != null) {
            builder.proxy(proxy);
        }

        return builder.build();
    }
}
