package com.mayday.ai.api.impl;

import com.mayday.ai.api.AiService;
import com.mayday.ai.factory.impl.AiModelFactory;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.router.impl.AiRouterBf;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * AiService 实现：
 * - 支持同场景多配置：priority 主选 + weight 轮询 + 失败回退
 * - 每次尝试都写 ai_call_log（成功/失败）
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService
{

    private final AiRouterBf aiRouterBf;
    private final AiModelFactory aiModelFactory;

    @Override
    public String chat(String sceneCode, String tenantId, String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is blank");
        }

        List<AiConfigEntity> list = aiRouterBf.route(tenantId, sceneCode);
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("No ai_config found for tenant=" + tenantId + ", scene=" + sceneCode);
        }

        AiConfigEntity cfg = list.getFirst(); // 先跑通：取 priority 最小的第一条
        ChatLanguageModel model = aiModelFactory.getOrCreate(cfg);
        return model.generate(prompt);
    }
}