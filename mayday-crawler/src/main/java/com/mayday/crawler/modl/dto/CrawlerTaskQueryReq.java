package com.mayday.crawler.modl.dto;

import lombok.Data;

/**
 * 任务查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerTaskQueryReq {

    private Long current = 1L;
    private Long pageSize = 10L;

    private Long id;
    private String taskName;
    private String status;
    private String crawlType;
}
