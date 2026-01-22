package com.mayday.ai.api;

import java.util.function.Consumer;

/**
 * 流式 AI 服务接口
 * 支持边生成边推送，实现"打字机"效果
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface StreamingAiService {

    /**
     * 流式对话/生成
     *
     * @param sceneCode  业务场景编码（对应 ai_config.scene_code）
     * @param tenantId   租户ID（单租户可固定 "*"）
     * @param prompt     用户提示词
     * @param onToken    每个 token 的回调（用于推送给前端）
     * @param onComplete 完成回调
     * @param onError    错误回调
     */
    void streamChat(String sceneCode, String tenantId, String prompt,
                    Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
}
