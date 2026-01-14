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
@Service
public class CrawlerArticleServiceImpl extends ServiceImpl<CrawlerArticleMapper, CrawlerArticleEntity> implements ICrawlerArticleService {

    @Override
    public Page<CrawlerArticleEntity> queryList(CrawlerArticleQueryReq req) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_ARTICLE_ENTITY.ID.eq(req.getId()).when(req.getId() != null))
                .and(CRAWLER_ARTICLE_ENTITY.TASK_ID.eq(req.getTaskId()).when(req.getTaskId() != null))
                .and(CRAWLER_ARTICLE_ENTITY.TITLE.like(req.getTitle()).when(StringUtils.isNotEmpty(req.getTitle())))
                .and(CRAWLER_ARTICLE_ENTITY.AUTHOR.like(req.getAuthor()).when(StringUtils.isNotEmpty(req.getAuthor())))
                .and(CRAWLER_ARTICLE_ENTITY.SOURCE_SITE.like(req.getSourceSite()).when(StringUtils.isNotEmpty(req.getSourceSite())))
                .and(CRAWLER_ARTICLE_ENTITY.PUBLISH_TIME.ge(req.getPublishTimeStart()).when(req.getPublishTimeStart() != null))
                .and(CRAWLER_ARTICLE_ENTITY.PUBLISH_TIME.le(req.getPublishTimeEnd()).when(req.getPublishTimeEnd() != null))
                .orderBy(CRAWLER_ARTICLE_ENTITY.CREATE_TIME, false);

        // 应用数据权限过滤
        CrawlerDataScopeUtil.applyDataScope(wrapper, CRAWLER_ARTICLE_ENTITY.CREATE_BY, CRAWLER_ARTICLE_ENTITY.DEPT_ID);

        return page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
    }
}
