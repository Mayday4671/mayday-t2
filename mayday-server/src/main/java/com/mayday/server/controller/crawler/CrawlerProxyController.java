package com.mayday.server.controller.crawler;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerProxyEditReq;
import com.mayday.crawler.modl.dto.CrawlerProxyQueryReq;
import com.mayday.crawler.modl.entity.CrawlerProxyEntity;
import com.mayday.crawler.service.ICrawlerProxyService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 爬虫全局代理配置接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerProxy")
@Tag(name = "爬虫全局代理配置接口")
@RequiredArgsConstructor
public class CrawlerProxyController {

    private final ICrawlerProxyService proxyService;

    @PostMapping("/list")
    @Operation(summary = "代理列表")
    @PreAuthorize("hasAuthority('crawler:proxy:list')")
    public AjaxResult list(@RequestBody CrawlerProxyQueryReq req) {
        Page<CrawlerProxyEntity> page = proxyService.queryList(req);
        return AjaxResult.success(page);
    }

    @GetMapping("/enabled")
    @Operation(summary = "已启用代理列表")
    @PreAuthorize("hasAuthority('crawler:proxy:list')")
    public AjaxResult enabled() {
        List<CrawlerProxyEntity> list = proxyService.listEnabled();
        return AjaxResult.success(list);
    }

    @PostMapping
    @Operation(summary = "新增代理")
    @PreAuthorize("hasAuthority('crawler:proxy:add')")
    public AjaxResult add(@Valid @RequestBody CrawlerProxyEditReq req) {
        Long id = proxyService.saveOrUpdateProxy(req);
        return AjaxResult.success(id);
    }

    @PutMapping
    @Operation(summary = "编辑代理")
    @PreAuthorize("hasAuthority('crawler:proxy:edit')")
    public AjaxResult edit(@Valid @RequestBody CrawlerProxyEditReq req) {
        Long id = proxyService.saveOrUpdateProxy(req);
        return AjaxResult.success(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除代理")
    @PreAuthorize("hasAuthority('crawler:proxy:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return AjaxResult.success(proxyService.removeProxy(id));
    }
}
