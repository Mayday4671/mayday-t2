package com.mayday.ai.provider;

import com.mayday.ai.model.entity.AiConfigEntity;
import dev.langchain4j.model.chat.ChatLanguageModel;


/**
 * AiProvider：策略接口
 *
 * 作用：
 * - 将“不同供应商 SDK 构建方式”隔离在 provider 层
 * - 上层仅关心 providerCode/modelName/baseUrl/key/参数
 */
public interface AiProvider {

    /** 返回该策略的 provider code（如 google） */
    String provider();

    /**
     * 构建 LangChain4j ChatLanguageModel
     *
     * @param cfg 配置（模型名、baseUrl、参数等）
     * @param apiKeyPlain 解密后的 key（只在内存短暂使用）
     */
    ChatLanguageModel buildChatModel(AiConfigEntity cfg, String apiKeyPlain);
}