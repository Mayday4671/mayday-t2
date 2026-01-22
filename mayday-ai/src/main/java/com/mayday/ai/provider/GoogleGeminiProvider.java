package com.mayday.ai.provider;

import com.mayday.ai.config.AiNetProperties;
import com.mayday.ai.factory.AiHttpClientFactory;
import com.mayday.ai.model.entity.AiConfigEntity;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.Proxy;
import java.time.Duration;

/**
 * Google Gemini Provider
 * - 使用 LangChain4j 的 OpenAI 兼容模式访问 Gemini
 * - 支持超时配置和代理
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class GoogleGeminiProvider implements AiProvider {

    private final AiNetProperties netProps;
    private final AiHttpClientFactory httpFactory;

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    @Override
    public String provider() {
        return "google";
    }

    @Override
    public ChatLanguageModel buildChatModel(AiConfigEntity cfg, String apiKeyPlain) {
        double temp = (cfg.getTemperature() == null) ? 0.7 : cfg.getTemperature().doubleValue();

        // 计算超时时间
        Duration timeout = resolveTimeout(cfg);

        // 获取代理
        Proxy proxy = httpFactory.buildProxyOrNull();

        var builder = OpenAiChatModel.builder()
            .apiKey(apiKeyPlain)
            .baseUrl(cfg.getBaseUrl())       // DB 配置的 Gemini OpenAI 兼容端点
            .modelName(cfg.getModelName())
            .temperature(temp)
            .timeout(timeout);  // ✅ 添加超时配置

        // 代理配置
        if (proxy != null) {
            builder.proxy(proxy);
        }

        return builder.build();
    }

    /**
     * 计算超时时间
     * 优先级：DB 单独配置(timeoutMs) > 全局配置(timeoutSeconds) > 默认值
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
}

