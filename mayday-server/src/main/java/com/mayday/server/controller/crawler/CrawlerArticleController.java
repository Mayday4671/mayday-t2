package com.mayday.server.controller.crawler;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerArticleQueryReq;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文章管理接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerArticle")
@Tag(name = "文章管理接口")
@RequiredArgsConstructor
public class CrawlerArticleController {

    private final ICrawlerArticleService articleService;
    private final ICrawlerImageService imageService;

    @PostMapping("/list")
    @Operation(summary = "文章列表")
    @PreAuthorize("hasAuthority('crawler:article:list')")
    public AjaxResult list(@RequestBody CrawlerArticleQueryReq req) {
        Page<CrawlerArticleEntity> page = articleService.queryList(req);
        return AjaxResult.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "文章详情")
    @PreAuthorize("hasAuthority('crawler:article:detail')")
    public AjaxResult detail(@PathVariable("id") Long id) {
        CrawlerArticleEntity entity = articleService.getById(id);
        return AjaxResult.success(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    @PreAuthorize("hasAuthority('crawler:article:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        imageService.removeImagesWithFileByArticleId(id);
        return AjaxResult.success(articleService.removeById(id));
    }
}
