package com.mayday.ai.controller;

import com.mayday.ai.model.dto.AiCallLogQuery;
import com.mayday.ai.model.entity.AiCallLogEntity;
import com.mayday.ai.service.AiCallLogService;
import com.mayday.common.web.AjaxResult;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 调用日志控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/ai/log")
@RequiredArgsConstructor
public class AiCallLogController {

    private final AiCallLogService aiCallLogService;

    /**
     * 分页查询
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ai:log:list')")
    public AjaxResult list(AiCallLogQuery query) {
        Page<AiCallLogEntity> page = aiCallLogService.page(query);
        return AjaxResult.success(page);
    }

    /**
     * 获取详细信息
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ai:log:query')")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(aiCallLogService.getById(id));
    }
    
    // 日志通常不提供修改和删除（或只允许批量清理），这里暂且保留查询
}
