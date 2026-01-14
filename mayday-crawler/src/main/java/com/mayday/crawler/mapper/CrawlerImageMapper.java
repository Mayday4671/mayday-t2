package com.mayday.crawler.mapper;

import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverDTO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 图片 Mapper
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Mapper
public interface CrawlerImageMapper extends BaseMapper<CrawlerImageEntity> {

    @Select("""
        SELECT
            a.id AS articleId,
            a.task_id AS taskId,
            a.title AS articleTitle,
            a.url AS articleUrl,
            a.source_site AS sourceSite,
            a.publish_time AS publishTime,
            a.create_time AS articleCreateTime,
            (SELECT i.url FROM crawler_image i WHERE i.article_id = a.id ORDER BY i.id ASC LIMIT 1) AS coverUrl,
            (SELECT i.file_path FROM crawler_image i WHERE i.article_id = a.id ORDER BY i.id ASC LIMIT 1) AS coverFilePath,
            (SELECT i.file_name FROM crawler_image i WHERE i.article_id = a.id ORDER BY i.id ASC LIMIT 1) AS coverFileName,
            (SELECT i.download_status FROM crawler_image i WHERE i.article_id = a.id ORDER BY i.id ASC LIMIT 1) AS coverDownloadStatus,
            (SELECT COUNT(1) FROM crawler_image i2 WHERE i2.article_id = a.id) AS imageCount
        FROM crawler_article a
        WHERE (#{taskId} IS NULL OR a.task_id = #{taskId})
          AND (#{title} IS NULL OR #{title} = '' OR a.title LIKE CONCAT('%', #{title}, '%'))
          AND EXISTS (SELECT 1 FROM crawler_image ix WHERE ix.article_id = a.id)
          AND (#{createBy} IS NULL OR a.create_by = #{createBy})
          AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
        ORDER BY a.create_time DESC
        LIMIT #{pageSize} OFFSET #{offset}
        """)
    List<CrawlerImageArticleCoverDTO> selectArticleCoverPage(@Param("offset") long offset,
                                                             @Param("pageSize") long pageSize,
                                                             @Param("taskId") Long taskId,
                                                             @Param("title") String title,
                                                             @Param("createBy") Long createBy,
                                                             @Param("deptId") Long deptId);

    @Select("""
        SELECT COUNT(1)
        FROM crawler_article a
        WHERE (#{taskId} IS NULL OR a.task_id = #{taskId})
          AND (#{title} IS NULL OR #{title} = '' OR a.title LIKE CONCAT('%', #{title}, '%'))
          AND EXISTS (SELECT 1 FROM crawler_image ix WHERE ix.article_id = a.id)
          AND (#{createBy} IS NULL OR a.create_by = #{createBy})
          AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
        """)
    Long countArticleCover(@Param("taskId") Long taskId, 
                           @Param("title") String title,
                           @Param("createBy") Long createBy,
                           @Param("deptId") Long deptId);

    @Select("""
        SELECT *
        FROM crawler_image
        WHERE article_id = #{articleId}
        ORDER BY id ASC
        """)
    List<CrawlerImageEntity> selectByArticleId(@Param("articleId") Long articleId);
}
