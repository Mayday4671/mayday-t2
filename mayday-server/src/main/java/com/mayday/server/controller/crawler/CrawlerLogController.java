package com.mayday.server.controller.crawler;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerLogQueryReq;
import com.mayday.crawler.modl.entity.CrawlerLogEntity;
import com.mayday.crawler.service.ICrawlerLogService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 爬虫日志管理接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerLog")
@Tag(name = "爬虫日志管理接口")
@RequiredArgsConstructor
public class CrawlerLogController {

    private final ICrawlerLogService logService;

    @PostMapping("/list")
    @Operation(summary = "日志列表")
    @PreAuthorize("hasAuthority('crawler:log:list')")
    public AjaxResult list(@RequestBody CrawlerLogQueryReq req) {
        Page<CrawlerLogEntity> page = logService.queryList(req);
        return AjaxResult.success(page);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日志")
    @PreAuthorize("hasAuthority('crawler:log:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return AjaxResult.success(logService.removeById(id));
    }
}
