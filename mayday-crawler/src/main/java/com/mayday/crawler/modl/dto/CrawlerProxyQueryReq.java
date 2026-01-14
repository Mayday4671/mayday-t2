package com.mayday.crawler.modl.dto;

import lombok.Data;

/**
 * 代理查询请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerProxyQueryReq {

    private Long current = 1L;
    private Long pageSize = 10L;

    private String proxyName;
    private String proxyType;
    private Integer enabled;
}
