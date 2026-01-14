package com.mayday.crawler.modl.dto;

import lombok.Data;

/**
 * 按文章聚合分页查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerImageArticleCoverQueryReq {

    private Long current = 1L;
    private Long pageSize = 10L;

    private Long taskId;
    private String title;
}
