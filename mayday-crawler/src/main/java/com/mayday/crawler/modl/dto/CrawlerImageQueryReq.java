package com.mayday.crawler.modl.dto;

import lombok.Data;

/**
 * 图片查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerImageQueryReq {

    private Long current = 1L;
    private Long pageSize = 10L;

    private Long id;
    private Long taskId;
    private Long articleId;
    private String downloadStatus;
    private String format;
}
