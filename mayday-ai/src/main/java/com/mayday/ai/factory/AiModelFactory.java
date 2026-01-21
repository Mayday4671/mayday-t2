package com.mayday.ai.factory;

import com.mayday.ai.model.entity.AiConfigEntity;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * 模型工厂：根据 ai_config 动态构建 ChatLanguageModel
 * - 建议缓存（cacheKey = configId:version）
 */
public interface AiModelFactory {

    ChatLanguageModel getOrCreate(AiConfigEntity cfg);
}