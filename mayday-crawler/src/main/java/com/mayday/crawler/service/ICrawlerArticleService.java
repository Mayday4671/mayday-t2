package com.mayday.crawler.service;

import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.modl.dto.CrawlerArticleQueryReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 文章服务接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface ICrawlerArticleService extends IService<CrawlerArticleEntity> {

    /**
     * 分页查询文章列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerArticleEntity> queryList(CrawlerArticleQueryReq req);

    /**
     * 查询文章详情（包含图片处理）
     *
     * @param id 文章ID
     * @return 文章详情
     */
    CrawlerArticleEntity queryDetail(Long id);
}
