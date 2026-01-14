package com.mayday.crawler.modl.dto;

import lombok.Data;

import java.util.Date;

/**
 * 日志查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerLogQueryReq {

    private Long current = 1L;
    private Long pageSize = 10L;

    private Long id;
    private Long taskId;
    private String logType;
    private String level;
    private Date createTimeStart;
    private Date createTimeEnd;
}
