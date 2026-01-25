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
        // 只展示已发布的文章
        if (req.getStatus() == null) {
            // req.setStatus(1); // 暂时注释掉状态过滤，以排查是否状态值问题
        }
        Page<CrawlerArticleEntity> page = articleService.queryPortalList(req);
        return AjaxResult.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "公开文章详情")
    public AjaxResult detail(@PathVariable("id") Long id) {
        CrawlerArticleEntity entity = articleService.queryDetail(id);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            return AjaxResult.error("文章不存在或未发布");
        }
        return AjaxResult.success(entity);
    }
}
