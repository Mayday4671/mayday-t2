package com.mayday.crawler.service;

import com.mayday.crawler.modl.entity.CrawlerLogEntity;
import com.mayday.crawler.modl.dto.CrawlerLogQueryReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 爬虫日志服务接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface ICrawlerLogService extends IService<CrawlerLogEntity> {

    /**
     * 分页查询日志列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerLogEntity> queryList(CrawlerLogQueryReq req);
}
