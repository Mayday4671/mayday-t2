package com.mayday.crawler.service.impl;

import com.mayday.common.util.StringUtils;
import com.mayday.crawler.mapper.CrawlerLogMapper;
import com.mayday.crawler.modl.dto.CrawlerLogQueryReq;
import com.mayday.crawler.modl.entity.CrawlerLogEntity;
import com.mayday.crawler.service.ICrawlerLogService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import static com.mayday.crawler.modl.entity.table.CrawlerLogEntityTableDef.CRAWLER_LOG_ENTITY;

import com.mayday.crawler.util.CrawlerDataScopeUtil;

/**
 * 爬虫日志服务实现
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Service
public class CrawlerLogServiceImpl extends ServiceImpl<CrawlerLogMapper, CrawlerLogEntity> implements ICrawlerLogService {

    @Override
    public Page<CrawlerLogEntity> queryList(CrawlerLogQueryReq req) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_LOG_ENTITY.ID.eq(req.getId()).when(req.getId() != null))
                .and(CRAWLER_LOG_ENTITY.TASK_ID.eq(req.getTaskId()).when(req.getTaskId() != null))
                .and(CRAWLER_LOG_ENTITY.LOG_TYPE.eq(req.getLogType()).when(StringUtils.isNotEmpty(req.getLogType())))
                .and(CRAWLER_LOG_ENTITY.LEVEL.eq(req.getLevel()).when(StringUtils.isNotEmpty(req.getLevel())))
                .and(CRAWLER_LOG_ENTITY.CREATE_TIME.ge(req.getCreateTimeStart()).when(req.getCreateTimeStart() != null))
                .and(CRAWLER_LOG_ENTITY.CREATE_TIME.le(req.getCreateTimeEnd()).when(req.getCreateTimeEnd() != null))
                .orderBy(CRAWLER_LOG_ENTITY.CREATE_TIME, false);

        // 应用数据权限过滤
        CrawlerDataScopeUtil.applyDataScope(wrapper, CRAWLER_LOG_ENTITY.CREATE_BY, CRAWLER_LOG_ENTITY.DEPT_ID);

        return page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
    }
}
