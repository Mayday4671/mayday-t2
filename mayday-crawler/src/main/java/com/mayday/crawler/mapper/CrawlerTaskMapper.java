package com.mayday.crawler.mapper;

import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 爬虫任务 Mapper
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Mapper
public interface CrawlerTaskMapper extends BaseMapper<CrawlerTaskEntity> {
}
