package com.mayday.ai.router.impl;

import com.mayday.ai.mapper.AiConfigMapper;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;


/**
 * AiRouter：负责从数据库拉取某个 (tenantId + sceneCode) 的候选配置列表
 *
 * 说明：
 * - 这里只负责“取候选并排序”，不做真正的权重挑选与回退执行
 * - 先查租户配置；若没有则查 tenant='*' 的全局配置
 */
@Service
@RequiredArgsConstructor
public class AiRouter {

    private final AiConfigMapper aiConfigMapper;

    public List<AiConfigEntity> route(String tenantId, String sceneCode) {
        // 1) 优先租户级配置
        List<AiConfigEntity> list = queryEnabled(tenantId, sceneCode);

        // 2) 若租户级为空，fallback 到全局默认
        if (list.isEmpty() && !"*".equals(tenantId)) {
            list = queryEnabled("*", sceneCode);
        }

        if (list.isEmpty()) {
            throw new IllegalStateException("AI config not found: tenant=" + tenantId + ", scene=" + sceneCode);
        }

        // 3) priority 越小越优先
        list.sort(Comparator.comparing(AiConfigEntity::getPriority));
        return list;
    }

    private List<AiConfigEntity> queryEnabled(String tenantId, String sceneCode) {
        return aiConfigMapper.selectListByQuery(
            QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("scene_code", sceneCode)
                .eq("enabled", 1)
        );
    }
}
