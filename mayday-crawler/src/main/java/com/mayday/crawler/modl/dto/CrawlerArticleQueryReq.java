package com.mayday.crawler.modl.dto;

import lombok.Data;

import java.util.Date;

/**
 * 文章查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerArticleQueryReq {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long pageSize = 10L;

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 文章标题（模糊查询）
     */
    private String title;

    /**
     * 作者（模糊查询）
     */
    private String author;

    /**
     * 来源站点（模糊查询）
     */
    private String sourceSite;

    /**
     * 发布时间开始
     */
    private Date publishTimeStart;

    /**
     * 发布时间结束
     */
    private Date publishTimeEnd;

    /**
     * 审核状态：0-待审核 1-已发布 2-已驳回
     */
    private Integer status;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 排序方式：new-最新 hot-最热 recommend-推荐
     */
    private String sortType;
}
