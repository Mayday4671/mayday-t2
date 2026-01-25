package com.mayday.crawler.service.impl;

import com.mayday.common.util.StringUtils;
import com.mayday.crawler.mapper.CrawlerArticleMapper;
import com.mayday.crawler.modl.dto.CrawlerArticleQueryReq;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import static com.mayday.crawler.modl.entity.table.CrawlerArticleEntityTableDef.CRAWLER_ARTICLE_ENTITY;

import com.mayday.crawler.util.CrawlerDataScopeUtil;

/**
 * 文章服务实现
 *
 * @author Antigravity
 * @since 1.0.0
 */
import com.mayday.crawler.mapper.CrawlerImageMapper;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.mayday.crawler.modl.entity.table.CrawlerImageEntityTableDef.CRAWLER_IMAGE_ENTITY;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerArticleServiceImpl extends ServiceImpl<CrawlerArticleMapper, CrawlerArticleEntity> implements ICrawlerArticleService {

    private final CrawlerImageMapper imageMapper;

    @Value("${crawler.image.base-path:${crawler.image-base-path:./data/crawler-images}}")
    private String imageBasePath;

    @Override
    public Page<CrawlerArticleEntity> queryList(CrawlerArticleQueryReq req) {
        QueryWrapper wrapper = buildQueryWrapper(req);
        // Apply data scope
        CrawlerDataScopeUtil.applyDataScope(wrapper, CRAWLER_ARTICLE_ENTITY.CREATE_BY, CRAWLER_ARTICLE_ENTITY.DEPT_ID);
        return doQueryPage(req, wrapper);
    }

    @Override
    public Page<CrawlerArticleEntity> queryPortalList(CrawlerArticleQueryReq req) {
        QueryWrapper wrapper = buildQueryWrapper(req);
        // Portal: No data scope applied
        return doQueryPage(req, wrapper);
    }

    private QueryWrapper buildQueryWrapper(CrawlerArticleQueryReq req) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_ARTICLE_ENTITY.ID.eq(req.getId()).when(req.getId() != null))
                .and(CRAWLER_ARTICLE_ENTITY.TASK_ID.eq(req.getTaskId()).when(req.getTaskId() != null))
                .and(CRAWLER_ARTICLE_ENTITY.TITLE.like(req.getTitle()).when(StringUtils.isNotEmpty(req.getTitle())))
                .and(CRAWLER_ARTICLE_ENTITY.AUTHOR.like(req.getAuthor()).when(StringUtils.isNotEmpty(req.getAuthor())))
                .and(CRAWLER_ARTICLE_ENTITY.SOURCE_SITE.like(req.getSourceSite()).when(StringUtils.isNotEmpty(req.getSourceSite())))
                .and(CRAWLER_ARTICLE_ENTITY.PUBLISH_TIME.ge(req.getPublishTimeStart()).when(req.getPublishTimeStart() != null))
                .and(CRAWLER_ARTICLE_ENTITY.PUBLISH_TIME.le(req.getPublishTimeEnd()).when(req.getPublishTimeEnd() != null))
                .and(CRAWLER_ARTICLE_ENTITY.STATUS.eq(req.getStatus()).when(req.getStatus() != null))
                .and(CRAWLER_ARTICLE_ENTITY.CATEGORY_ID.eq(req.getCategoryId()).when(req.getCategoryId() != null));
        
        // Handle Sort
        if ("hot".equals(req.getSortType())) {
            wrapper.orderBy(CRAWLER_ARTICLE_ENTITY.VIEW_COUNT.desc());
        } else {
            wrapper.orderBy(CRAWLER_ARTICLE_ENTITY.PUBLISH_TIME.desc());
        }
        
        return wrapper;
    }

    private Page<CrawlerArticleEntity> doQueryPage(CrawlerArticleQueryReq req, QueryWrapper wrapper) {
        Page<CrawlerArticleEntity> page = page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
        
        // Populate cover images from crawler_image table
        if (page.hasRecords()) {
            List<Long> articleIds = page.getRecords().stream()
                    .map(CrawlerArticleEntity::getId)
                    .collect(Collectors.toList());
            
            if (!articleIds.isEmpty()) {
                // Fetch valid images
                List<CrawlerImageEntity> images = imageMapper.selectListByQuery(
                    QueryWrapper.create()
                        .where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.in(articleIds))
                );

                // Map articleId -> computed display URL
                Map<Long, String> coverMap = images.stream()
                    .collect(Collectors.toMap(
                        CrawlerImageEntity::getArticleId,
                        this::computeDisplayUrl,
                        (existing, replacement) -> existing // Keep the first found
                    ));

                for (CrawlerArticleEntity entity : page.getRecords()) {
                    entity.setCoverImage(coverMap.get(entity.getId()));
                }
            }
        }
        return page;
    }

    @Override
    public CrawlerArticleEntity queryDetail(Long id) {
        CrawlerArticleEntity entity = getById(id);
        if (entity == null) {
            return null;
        }

        // Fetch associated images
        List<CrawlerImageEntity> images = imageMapper.selectListByQuery(
            QueryWrapper.create().where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.eq(id))
        );

        if (images != null && !images.isEmpty()) {
            // 1. Set cover image
            String coverUrl = computeDisplayUrl(images.get(0));
            entity.setCoverImage(coverUrl);

            // 2. Set image list (all images)
            java.util.List<String> imageList = images.stream()
                .map(this::computeDisplayUrl)
                .collect(java.util.stream.Collectors.toList());
            entity.setImageList(imageList);

            // 3. Replace content images
            String content = entity.getContent();
            if (StringUtils.isNotEmpty(content)) {
                for (CrawlerImageEntity img : images) {
                    if (StringUtils.isNotEmpty(img.getUrl())) {
                        String localUrl = computeDisplayUrl(img);
                        // Replace original URL with local URL in content
                        // Note: This is a simple replacement. 
                        // It might be safer to match src="..." but simple replace often works for unique URLs.
                        content = content.replace(img.getUrl(), localUrl);
                    }
                }
                entity.setContent(content);
            }
        }
        return entity;
    }

    private String computeDisplayUrl(CrawlerImageEntity image) {
        if ("SUCCESS".equals(image.getDownloadStatus()) && StringUtils.isNotEmpty(image.getFilePath()) && StringUtils.isNotEmpty(image.getFileName())) {
            try {
                Path basePath = Paths.get(imageBasePath).toAbsolutePath().normalize();
                Path dirPath = Paths.get(image.getFilePath()).toAbsolutePath().normalize();
                Path fullFilePath = dirPath.resolve(image.getFileName()).normalize();

                if (fullFilePath.startsWith(basePath)) {
                    String relativePath = basePath.relativize(fullFilePath).toString().replace("\\", "/");
                    return "/crawler-images/" + relativePath;
                } else {
                    return "/crawler-images/" + image.getFileName();
                }
            } catch (Exception e) {
                log.warn("Error computing display URL for image {}: {}", image.getId(), e.getMessage());
            }
        }
        return image.getUrl();
    }
}
