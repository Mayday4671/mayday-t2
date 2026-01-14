package com.mayday.crawler.service.impl;

import com.mayday.common.enums.ErrorCode;
import com.mayday.common.exception.BusinessException;
import com.mayday.common.util.BeanConverterUtils;
import com.mayday.common.util.StringUtils;
import com.mayday.crawler.mapper.CrawlerProxyMapper;
import com.mayday.crawler.modl.dto.CrawlerProxyEditReq;
import com.mayday.crawler.modl.dto.CrawlerProxyQueryReq;
import com.mayday.crawler.modl.entity.CrawlerProxyEntity;
import com.mayday.crawler.service.ICrawlerProxyService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mayday.crawler.modl.entity.table.CrawlerProxyEntityTableDef.CRAWLER_PROXY_ENTITY;

/**
 * 全局代理服务实现
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Service
public class CrawlerProxyServiceImpl extends ServiceImpl<CrawlerProxyMapper, CrawlerProxyEntity> implements ICrawlerProxyService {

    @Override
    public Page<CrawlerProxyEntity> queryList(CrawlerProxyQueryReq req) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_PROXY_ENTITY.PROXY_NAME.like(req.getProxyName()).when(StringUtils.isNotEmpty(req.getProxyName())))
                .and(CRAWLER_PROXY_ENTITY.PROXY_TYPE.eq(req.getProxyType()).when(StringUtils.isNotEmpty(req.getProxyType())))
                .and(CRAWLER_PROXY_ENTITY.ENABLED.eq(req.getEnabled()).when(req.getEnabled() != null))
                .orderBy(CRAWLER_PROXY_ENTITY.SORT, true)
                .orderBy(CRAWLER_PROXY_ENTITY.ID, true);

        return page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateProxy(CrawlerProxyEditReq req) {
        CrawlerProxyEntity entity = BeanConverterUtils.convert(req, CrawlerProxyEntity.class);
        if (req.getId() == null) {
            if (entity.getSort() == null) {
                entity.setSort(0);
            }
            save(entity);
            return entity.getId();
        }

        CrawlerProxyEntity existing = getById(req.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代理不存在");
        }
        updateById(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeProxy(Long id) {
        return removeById(id);
    }

    @Override
    public List<CrawlerProxyEntity> listEnabled() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_PROXY_ENTITY.ENABLED.eq(1))
                .orderBy(CRAWLER_PROXY_ENTITY.SORT, true)
                .orderBy(CRAWLER_PROXY_ENTITY.ID, true);
        return list(wrapper);
    }
}
