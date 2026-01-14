package com.mayday.server.controller.crawler;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerTaskEditReq;
import com.mayday.crawler.modl.dto.CrawlerTaskQueryReq;
import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import com.mayday.crawler.service.ICrawlerTaskService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 爬虫任务管理接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerTask")
@Tag(name = "爬虫任务管理接口")
@RequiredArgsConstructor
public class CrawlerTaskController {

    private final ICrawlerTaskService taskService;

    @PostMapping("/list")
    @Operation(summary = "任务列表")
    @PreAuthorize("hasAuthority('crawler:task:list')")
    public AjaxResult list(@RequestBody CrawlerTaskQueryReq req) {
        Page<CrawlerTaskEntity> page = taskService.queryList(req);
        return AjaxResult.success(page);
    }

    @PostMapping
    @Operation(summary = "新增任务")
    @PreAuthorize("hasAuthority('crawler:task:add')")
    public AjaxResult add(@Valid @RequestBody CrawlerTaskEditReq req) {
        Long id = taskService.saveOrUpdateTask(req);
        return AjaxResult.success(id);
    }

    @PutMapping
    @Operation(summary = "编辑任务")
    @PreAuthorize("hasAuthority('crawler:task:edit')")
    public AjaxResult edit(@Valid @RequestBody CrawlerTaskEditReq req) {
        Long id = taskService.saveOrUpdateTask(req);
        return AjaxResult.success(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    @PreAuthorize("hasAuthority('crawler:task:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.removeTask(id));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动任务")
    @PreAuthorize("hasAuthority('crawler:task:start')")
    public AjaxResult start(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.startTask(id));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停任务")
    @PreAuthorize("hasAuthority('crawler:task:pause')")
    public AjaxResult pause(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.pauseTask(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复任务")
    @PreAuthorize("hasAuthority('crawler:task:resume')")
    public AjaxResult resume(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.resumeTask(id));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "停止任务")
    @PreAuthorize("hasAuthority('crawler:task:stop')")
    public AjaxResult stop(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.stopTask(id));
    }

    @PostMapping("/{id}/rerun")
    @Operation(summary = "重跑任务")
    @PreAuthorize("hasAuthority('crawler:task:rerun')")
    public AjaxResult rerun(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.rerunTask(id));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "查询任务运行状态")
    @PreAuthorize("hasAuthority('crawler:task:list')")
    public AjaxResult getStatus(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.getTaskStatus(id));
    }

    @GetMapping("/running")
    @Operation(summary = "查询所有运行中的任务")
    @PreAuthorize("hasAuthority('crawler:task:list')")
    public AjaxResult getRunningTasks() {
        return AjaxResult.success(taskService.getRunningTasks());
    }
}
