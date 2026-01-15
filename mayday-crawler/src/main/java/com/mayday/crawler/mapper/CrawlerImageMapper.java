package com.mayday.crawler.mapper;

import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图片 Mapper
 * 
 * 使用 MyBatis-Flex 的 BaseMapper 提供的标准 CRUD 方法，
 * 复杂查询通过 Service 层使用 QueryWrapper 实现。
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Mapper
public interface CrawlerImageMapper extends BaseMapper<CrawlerImageEntity> {
    // 所有查询逻辑通过 Service 层使用 QueryWrapper 实现
    // 保持 Mapper 简洁，符合 MyBatis-Flex 的设计理念
}
