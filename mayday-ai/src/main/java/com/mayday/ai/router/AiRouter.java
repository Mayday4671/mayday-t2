package com.mayday.ai.router;

import com.mayday.ai.model.entity.AiConfigEntity;

import java.util.List;


/**
 * AI 路由：从数据库选出当前场景的候选配置
 */
public interface AiRouter {

    /**
     * 按 tenant + scene 选出可用配置（优先 tenant 精确匹配，再 fallback 到 "*"）
     */
    List<AiConfigEntity> route(String tenantId, String sceneCode);
}