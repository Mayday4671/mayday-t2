package com.mayday.crawler.modl.dto;

import lombok.Data;

/**
 * 代理编辑请求
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerProxyEditReq {

    private Long id;
    private String proxyName;
    private String proxyType;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Integer enabled;
    private Integer sort;
}
