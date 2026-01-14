package com.mayday.crawler.service;

import com.mayday.crawler.modl.entity.CrawlerProxyEntity;
import com.mayday.crawler.modl.dto.CrawlerProxyEditReq;
import com.mayday.crawler.modl.dto.CrawlerProxyQueryReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 全局代理服务接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface ICrawlerProxyService extends IService<CrawlerProxyEntity> {

    /**
     * 分页查询代理列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerProxyEntity> queryList(CrawlerProxyQueryReq req);

    /**
     * 新增或更新代理
     *
     * @param req 请求参数
     * @return 代理ID
     */
    Long saveOrUpdateProxy(CrawlerProxyEditReq req);

    /**
     * 删除代理
     *
     * @param id 代理ID
     * @return 是否删除成功
     */
    boolean removeProxy(Long id);

    /**
     * 查询已启用的代理列表（按 sort、id 升序）
     *
     * @return 代理列表
     */
    List<CrawlerProxyEntity> listEnabled();
}
