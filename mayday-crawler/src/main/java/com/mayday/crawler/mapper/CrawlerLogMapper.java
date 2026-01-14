package com.mayday.crawler.mapper;

import com.mayday.crawler.modl.entity.CrawlerLogEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 爬虫日志 Mapper
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Mapper
public interface CrawlerLogMapper extends BaseMapper<CrawlerLogEntity> {
}
