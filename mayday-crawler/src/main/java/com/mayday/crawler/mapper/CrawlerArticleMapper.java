package com.mayday.crawler.mapper;

import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 Mapper
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Mapper
public interface CrawlerArticleMapper extends BaseMapper<CrawlerArticleEntity> {
}
