package com.mayday.ai.service.impl;

import com.mayday.ai.mapper.AiCallLogMapper;
import com.mayday.ai.model.dto.AiCallLogQuery;
import com.mayday.ai.model.entity.AiCallLogEntity;
import com.mayday.ai.service.AiCallLogService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.mayday.ai.model.entity.table.AiCallLogEntityTableDef.AI_CALL_LOG_ENTITY;

/**
 * AI 调用日志实现
 */
@Service
@RequiredArgsConstructor
public class AiCallLogServiceImpl extends ServiceImpl<AiCallLogMapper, AiCallLogEntity> implements AiCallLogService {

    @Override
    public Page<AiCallLogEntity> page(AiCallLogQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        
        // 只有非空时才添加条件
        if (query.getRequestId() != null && !query.getRequestId().isEmpty()) {
            qw.and(AI_CALL_LOG_ENTITY.REQUEST_ID.eq(query.getRequestId()));
        }
        if (query.getSceneCode() != null && !query.getSceneCode().isEmpty()) {
            qw.and(AI_CALL_LOG_ENTITY.SCENE_CODE.eq(query.getSceneCode()));
        }
        if (query.getProvider() != null && !query.getProvider().isEmpty()) {
            qw.and(AI_CALL_LOG_ENTITY.PROVIDER.eq(query.getProvider()));
        }
        if (query.getSuccess() != null) {
            qw.and(AI_CALL_LOG_ENTITY.SUCCESS.eq(query.getSuccess()));
        }
        qw.orderBy(AI_CALL_LOG_ENTITY.CREATE_TIME.desc());
        
        return page(new Page<>(query.getPageNum(), query.getPageSize()), qw);
    }
}