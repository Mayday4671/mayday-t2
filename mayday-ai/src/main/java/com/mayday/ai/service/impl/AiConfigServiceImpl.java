package com.mayday.ai.service.impl;

import com.mayday.ai.mapper.AiConfigMapper;
import com.mayday.ai.model.dto.AiConfigQuery;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.service.AiConfigService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.mayday.ai.model.entity.table.AiConfigEntityTableDef.AI_CONFIG_ENTITY;

/**
 * AI 配置实现
 */
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfigEntity> implements AiConfigService
{

    private final AiConfigMapper aiConfigMapper;

    @Override
    public Page<AiConfigEntity> page(AiConfigQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        
        // 只有非空时才添加条件
        if (query.getSceneCode() != null && !query.getSceneCode().isEmpty()) {
            qw.and(AI_CONFIG_ENTITY.SCENE_CODE.eq(query.getSceneCode()));
        }
        if (query.getProvider() != null && !query.getProvider().isEmpty()) {
            qw.and(AI_CONFIG_ENTITY.PROVIDER.eq(query.getProvider()));
        }
        if (query.getModelName() != null && !query.getModelName().isEmpty()) {
            qw.and(AI_CONFIG_ENTITY.MODEL_NAME.like(query.getModelName()));
        }
        if (query.getEnabled() != null) {
            qw.and(AI_CONFIG_ENTITY.ENABLED.eq(query.getEnabled()));
        }
        qw.orderBy(AI_CONFIG_ENTITY.PRIORITY.asc(), AI_CONFIG_ENTITY.WEIGHT.desc());
        
        return page(new Page<>(query.getPageNum(), query.getPageSize()), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndBumpVersion(AiConfigEntity entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("ai_config id is required");
        }

        AiConfigEntity old = aiConfigMapper.selectOneById(entity.getId());
        if (old == null) {
            throw new IllegalArgumentException("ai_config not found: id=" + entity.getId());
        }

        // ✅ 任何变更都 version++，触发 factory 缓存失效
        int newVersion = (old.getVersion() == null ? 1 : old.getVersion()) + 1;
        entity.setVersion(newVersion);

        aiConfigMapper.update(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(Long id, boolean enabled) {
        AiConfigEntity old = aiConfigMapper.selectOneById(id);
        if (old == null) {
            throw new IllegalArgumentException("ai_config not found: id=" + id);
        }

        AiConfigEntity upd = new AiConfigEntity();
        upd.setId(id);
        upd.setEnabled(enabled ? 1 : 0);

        // ✅ 候选集合变化也应该 version++
        upd.setVersion((old.getVersion() == null ? 1 : old.getVersion()) + 1);

        aiConfigMapper.update(upd);
    }
}
