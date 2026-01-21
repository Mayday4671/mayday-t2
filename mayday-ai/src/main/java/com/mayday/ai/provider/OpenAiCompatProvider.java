package com.mayday.ai.provider;

import com.mayday.ai.model.entity.AiConfigEntity;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容 Provider：
 * - 大多数国产模型提供 OpenAI-Compatible API
 * - 通过 baseUrl + modelName 直接切换，不改代码
 *
 * provider 兼容范围建议：
 * - openai / deepseek / kimi / qwen / ollama(若提供openai接口) / 你自己的代理
 */
@Component
public class OpenAiCompatProvider implements AiProvider {

    @Override
    public String provider() {
        // 注意：这里不是 providerCode，而是这个“策略”的名字
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
     * @param cfg 配置（模型名、baseUrl、参数等）
     * @param apiKeyPlain 解密后的 key（只在内存短暂使用）
     * @return
     */
    @Override
    public ChatLanguageModel buildChatModel(AiConfigEntity cfg, String apiKeyPlain) {
        double temp = cfg.getTemperature() == null ? 0.7 : cfg.getTemperature().doubleValue();

        var b = OpenAiChatModel.builder()
            .apiKey(apiKeyPlain)
            .modelName(cfg.getModelName())
            .temperature(temp);
        // baseUrl：用于代理、第三方OpenAI兼容、或本地模型（如 vLLM / ollama-openai）
        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
            b.baseUrl(cfg.getBaseUrl());
        }

        return b.build();
    }
}