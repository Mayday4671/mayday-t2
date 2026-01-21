package com.mayday.ai.service;

/**
 * AiService：业务统一入口
 *
 * 业务层只需要关心：
 * - sceneCode：业务场景
 * - tenantId：租户（或*）
 * - prompt：提示词
 *
 * 底层模型供应商切换、回退、权重轮询等都由 mayday-ai 模块内部处理。
 */
public interface AiService {


    /**
     * 非流式对话/生成（MVP）
     *
     * @param sceneCode 业务场景编码（对应 ai_config.scene_code）
     * @param tenantId  租户ID（单租户可固定 "*"）
     * @param prompt    用户提示词（注意：不要把敏感信息写日志/落库）
     * @return 模型输出文本
     */
    String chat(String sceneCode, String tenantId, String prompt);
}