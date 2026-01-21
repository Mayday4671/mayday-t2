package com.mayday.ai.router.impl;

import com.mayday.ai.mapper.AiConfigMapper;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.router.AiRouter;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 简化版路由实现：
 * 1) 先查 tenantId 精确匹配
 * 2) 查不到则 fallback 到 "*"
 *
 * 说明：后续再加 priority/weight/回退也在这里扩展
 */
@Service
@RequiredArgsConstructor
public class AiRouterImpl implements AiRouter
{
    private final AiConfigMapper aiConfigMapper;

    @Override
    public List<AiConfigEntity> route(String tenantId, String sceneCode) {
        List<AiConfigEntity> list = queryEnabledConfigs(tenantId, sceneCode);
        if (list != null && !list.isEmpty()) return list;

        return queryEnabledConfigs("*", sceneCode);
    }

    /**
     * 查询指定 tenant + scene 的可用配置列表
     */
    private List<AiConfigEntity> queryEnabledConfigs(String tenantId, String sceneCode) {
        QueryWrapper qw = QueryWrapper.create()
            .from(AiConfigEntity.class)
            .where(AiConfigEntity::getTenantId).eq(tenantId)
            .and(AiConfigEntity::getSceneCode).eq(sceneCode)
            .and(AiConfigEntity::getEnabled).eq(1)
            .orderBy(AiConfigEntity::getPriority, true) // true=ASC
            .orderBy(AiConfigEntity::getId, true);

        // ✅ MyBatis-Flex：按 QueryWrapper 查列表
        return aiConfigMapper.selectListByQuery(qw);
    }
}