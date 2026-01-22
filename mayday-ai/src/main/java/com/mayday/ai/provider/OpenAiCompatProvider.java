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
 * OpenAI 兼容 Provider：
 * - 大多数国产模型提供 OpenAI-Compatible API
 * - 通过 baseUrl + modelName 直接切换，不改代码
 * - 支持超时配置
 *
 * provider 兼容范围建议：
 * - openai / deepseek / kimi / qwen / ollama(若提供openai接口) / 你自己的代理
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatProvider implements AiProvider {

    private final AiNetProperties netProps;
    private final AiHttpClientFactory httpClientFactory;

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    @Override
    public String provider() {
        // 注意：这里不是 providerCode，而是这个"策略"的名字
        // 真正匹配逻辑在 Factory 里用 supports(providerCode)
        return "openai-compat";
    }

    /** 判断某个 providerCode 是否走 OpenAI 兼容策略 */
    public boolean supports(String providerCode) {
        if (providerCode == null) return false;
        return switch (providerCode.toLowerCase()) {
            case "openai", "deepseek", "kimi", "qwen", "ollama", "alibaba", "google" -> true;
            default -> false;
        };
    }

    /**
     * 构建chat模型
     *
     * @param cfg 配置（模型名、baseUrl、参数等）
     * @param apiKeyPlain 解密后的 key（只在内存短暂使用）
     * @return 构建好的模型
     */
    @Override
    public ChatLanguageModel buildChatModel(AiConfigEntity cfg, String apiKeyPlain) {
        double temp = cfg.getTemperature() == null ? 0.7 : cfg.getTemperature().doubleValue();

        // 计算超时时间
        Duration timeout = resolveTimeout(cfg);

        // 获取代理
        Proxy proxy = httpClientFactory.buildProxyOrNull();

        var builder = OpenAiChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temp)
            .timeout(timeout);  // ✅ 添加超时配置

        // baseUrl：用于代理、第三方OpenAI兼容、或本地模型（如 vLLM / ollama-openai）
        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
            builder.baseUrl(cfg.getBaseUrl());
        }

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
