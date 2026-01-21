package com.mayday.ai.service;

import com.mayday.ai.model.dto.AiConfigQuery;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * AI 配置服务（后台管理端）
 */
public interface AiConfigService extends IService<AiConfigEntity> {

    /**
     * 分页查询
     */
    Page<AiConfigEntity> page(AiConfigQuery query);

    /**
     * 更新配置并自动 version++
     */
    void updateAndBumpVersion(AiConfigEntity entity);

    /**
     * 启用/停用（建议也 bump version）
     */
    void setEnabled(Long id, boolean enabled);
}