package com.mayday.ai.factory.impl;

import com.mayday.ai.mapper.AiKeyMapper;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mayday.ai.provider.AiProvider;
import com.mayday.ai.provider.OpenAiCompatProvider;
import com.mayday.ai.security.KeyCipher;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AiModelFactory：构建并缓存 ChatLanguageModel（避免每次请求 new 模型对象）
 *
 * 缓存 key 设计：
 * - configId + ":" + version
 *   只要你更新 ai_config 并让 version+1，就会自动走新缓存（旧缓存自然过期）
 *
 * 缓存实现：
 * - 这里用最简单的 ConcurrentHashMap + TTL 也行
 * - 你也可以替换成 Caffeine（更专业）
 */
@Service
@RequiredArgsConstructor
public class AiModelFactory {

    private final AiKeyMapper aiKeyMapper;
    private final List<AiProvider> providers;
    private final OpenAiCompatProvider openAiCompatProvider;
    private final KeyCipher keyCipher;

    /** 简单缓存：key=configId:version -> model */
    private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();

    /** TTL：模型对象缓存时间，避免长期持有（可按需要调整） */
    private final Duration ttl = Duration.ofMinutes(10);

    public ChatLanguageModel getOrCreate(AiConfigEntity cfg) {
        String cacheKey = cfg.getId() + ":" + cfg.getVersion();

        // 1) 命中缓存且未过期，直接返回
        CacheItem item = cache.get(cacheKey);
        if (item != null && !item.expired()) {
            return item.model;
        }

        // 2) 缓存未命中或已过期：重建
        ChatLanguageModel model = build(cfg);
        cache.put(cacheKey, new CacheItem(model, System.currentTimeMillis() + ttl.toMillis()));
        return model;
    }

    private ChatLanguageModel build(AiConfigEntity cfg) {
        // 1) 读取可用 key
        AiKeyEntity keyEntity = aiKeyMapper.selectOneByQuery(
            QueryWrapper.create()
                .eq("id", cfg.getKeyId())
                .eq("status", "ACTIVE")
        );
        if (keyEntity == null) {
            throw new IllegalStateException("AI key not found/disabled: keyId=" + cfg.getKeyId());
        }

        // 2) 解密 key（只在内存里短暂存在）
        String apiKeyPlain = keyCipher.decrypt(keyEntity.getKeyCipher());

        // 3) 根据 providerCode 选择对应策略构建模型
        String providerCode = cfg.getProvider() == null ? "" : cfg.getProvider().toLowerCase();

        // 3.1 OpenAI 兼容走统一策略（deepseek/kimi/qwen/...）
        if (openAiCompatProvider.supports(providerCode)) {
            return openAiCompatProvider.buildChatModel(cfg, apiKeyPlain);
        }

        // 3.2 其它 provider 走精确匹配
        AiProvider provider = providers.stream()
            .filter(p -> Objects.equals(p.provider(), providerCode))
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException("Unsupported provider: " + cfg.getProvider()));

        return provider.buildChatModel(cfg, apiKeyPlain);
    }

    /** 缓存项结构 */
    private static class CacheItem {
        final ChatLanguageModel model;
        final long expireAt;

        CacheItem(ChatLanguageModel model, long expireAt) {
            this.model = model;
            this.expireAt = expireAt;
        }

        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}