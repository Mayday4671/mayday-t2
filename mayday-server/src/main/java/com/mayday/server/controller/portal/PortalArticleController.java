package com.mayday.server.controller.portal;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerArticleQueryReq;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 门户前台-文章接口 (无需登录)
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/portal/article")
@Tag(name = "门户文章接口")
@RequiredArgsConstructor
public class PortalArticleController {

    private final ICrawlerArticleService articleService;

    @GetMapping("/list")
    @Operation(summary = "公开文章列表")
    public AjaxResult list(CrawlerArticleQueryReq req) {
        // Force status = 1 (Published) if you had a status field, logic here.
        // For now, just reusing existing query logic.
        Page<CrawlerArticleEntity> page = articleService.queryList(req);
        return AjaxResult.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "公开文章详情")
    public AjaxResult detail(@PathVariable("id") Long id) {
        CrawlerArticleEntity entity = articleService.queryDetail(id);
        return AjaxResult.success(entity);
    }
}
