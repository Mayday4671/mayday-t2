package com.mayday.crawler.modl.dto;

import lombok.Data;

import java.util.List;

/**
 * 任务编辑请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerTaskEditReq {

    private Long id;
    private String taskName;
    private String taskDesc;
    private List<String> startUrls;
    private String crawlType;
    private Integer maxDepth;
    private String scopeType;
    private Integer requestInterval;
    private Integer requestTimeout;
    private Integer maxRetries;
    private String userAgent;
    private String headers;
    private String cookies;
    private String referer;
    private Integer useProxy;
    private String proxyList;
    private Integer randomInterval;
    private Integer rotateUserAgent;
    private Integer listMaxPages;
    private Integer downloadImages;
    private String contentSelector;
    private String imageSelector;
    private String excludeSelector;
}
