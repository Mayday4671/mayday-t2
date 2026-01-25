package com.mayday.server.controller.portal;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.entity.CmsCategoryEntity;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.modl.entity.SysPortalMenuEntity;
import com.mayday.crawler.service.ICmsCategoryService;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mayday.crawler.service.ISysPortalMenuService;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal/home")
@Tag(name = "门户首页聚合接口")
@RequiredArgsConstructor
public class PortalHomeController {

    private final ISysPortalMenuService menuService;
    private final ICmsCategoryService categoryService;
    private final ICrawlerArticleService articleService;

    @GetMapping("/init")
    @Operation(summary = "首页初始化数据(菜单、分类、热榜)")
    public AjaxResult init() {
        Map<String, Object> data = new HashMap<>();

        // 1. 顶部菜单 (只查启用的)
        List<SysPortalMenuEntity> menus = menuService.list(
                new QueryWrapper().eq("status", 1).orderBy("sort", true)
        );
        data.put("menus", menus);

        // 2. 左侧分类 (只查启用的)
        List<CmsCategoryEntity> categories = categoryService.list(
                new QueryWrapper().eq("status", 1).orderBy("sort", true)
        );
        data.put("categories", categories);

        // 3. 今日热榜 (已发布 status=1, 按 view_count 倒序, 取前10)
        // 简单逻辑：取前10
        List<CrawlerArticleEntity> hotList = articleService.list(
                new QueryWrapper()
                        .eq("status", 1)
                        .orderBy("view_count", false)
                        .limit(10)
        );
        data.put("hotArticles", hotList);

        return AjaxResult.success(data);
    }
}
