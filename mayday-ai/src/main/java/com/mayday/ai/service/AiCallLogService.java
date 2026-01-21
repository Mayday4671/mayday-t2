package com.mayday.ai.service;

import com.mayday.ai.model.dto.AiCallLogQuery;
import com.mayday.ai.model.entity.AiCallLogEntity;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * AI 调用日志服务：
 * - 统一封装日志落库（成功/失败都写）
 * - 严禁写入 prompt 原文、API Key 等敏感内容
 */
public interface AiCallLogService extends IService<AiCallLogEntity> {

    /**
     * 分页查询
     */
    Page<AiCallLogEntity> page(AiCallLogQuery query);
}
