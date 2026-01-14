package com.mayday.server.controller.crawler;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverDTO;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverQueryReq;
import com.mayday.crawler.modl.dto.CrawlerImageQueryReq;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.vo.CrawlerImageVo;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mayday.crawler.service.impl.CrawlerImageServiceImpl;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片管理接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerImage")
@Tag(name = "图片管理接口")
@RequiredArgsConstructor
public class CrawlerImageController {

    private final ICrawlerImageService imageService;
    private final CrawlerImageServiceImpl imageServiceImpl;

    @PostMapping("/list")
    @Operation(summary = "图片列表")
    @PreAuthorize("hasAuthority('crawler:image:list')")
    public AjaxResult list(@RequestBody CrawlerImageQueryReq req) {
        Page<CrawlerImageEntity> page = imageService.queryList(req);
        // 转换为VO，设置displayUrl
        Page<CrawlerImageVo> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(page.getRecords().stream()
                .map(imageServiceImpl::convertToVo)
                .collect(Collectors.toList()));
        return AjaxResult.success(voPage);
    }

    @PostMapping("/articleCover/list")
    @Operation(summary = "图片按文章聚合列表（封面模式）")
    @PreAuthorize("hasAuthority('crawler:image:list')")
    public AjaxResult articleCoverList(@RequestBody CrawlerImageArticleCoverQueryReq req) {
        Page<CrawlerImageArticleCoverDTO> page = imageService.queryArticleCoverPage(req);
        return AjaxResult.success(page);
    }

    @GetMapping("/byArticle/{articleId}")
    @Operation(summary = "查询文章下的图片列表（用于轮播）")
    @PreAuthorize("hasAuthority('crawler:image:view')")
    public AjaxResult listByArticle(@PathVariable("articleId") Long articleId) {
        List<CrawlerImageEntity> list = imageService.listByArticleId(articleId);
        // 转换为VO，设置displayUrl
        List<CrawlerImageVo> voList = imageServiceImpl.convertToVoList(list);
        return AjaxResult.success(voList);
    }

    @DeleteMapping("/byArticle/{articleId}")
    @Operation(summary = "按文章删除图片（批量）")
    @PreAuthorize("hasAuthority('crawler:image:remove')")
    public AjaxResult deleteByArticle(@PathVariable("articleId") Long articleId) {
        boolean removed = imageServiceImpl.removeImagesWithFileByArticleId(articleId);
        return AjaxResult.success(removed);
    }

    @GetMapping("/{id}")
    @Operation(summary = "图片详情")
    @PreAuthorize("hasAuthority('crawler:image:view')")
    public AjaxResult detail(@PathVariable("id") Long id) {
        CrawlerImageEntity entity = imageService.getById(id);
        // 转换为VO，设置displayUrl
        CrawlerImageVo vo = imageServiceImpl.convertToVo(entity);
        return AjaxResult.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片")
    @PreAuthorize("hasAuthority('crawler:image:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return AjaxResult.success(imageServiceImpl.removeImageWithFileById(id));
    }
}
