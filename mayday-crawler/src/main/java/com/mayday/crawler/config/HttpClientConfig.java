package com.mayday.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP客户端配置
 * 用于爬虫请求的各项参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "crawler.http")
public class HttpClientConfig
{
    /**
     * 默认连接超时（毫秒）
     */
    private int connectTimeout = 15000;
    
    /**
     * 默认读取超时（毫秒）
     */
    private int readTimeout = 30000;
    
    /**
     * 最大并发连接数
     */
    private int maxConcurrency = 30;
    
    /**
     * 图片下载最大并发数
     */
    private int imageDownloadConcurrency = 50;
    
    /**
     * 是否启用连接池
     */
    private boolean useConnectionPool = true;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    /**
     * 重试延迟基数（毫秒）
     */
    private long retryDelayBase = 1000;
    
    /**
     * 是否启用智能重试（根据错误类型调整）
     */
    private boolean smartRetry = true;
    
    /**
     * User-Agent轮换间隔（请求数）
     */
    private int userAgentRotateInterval = 5;
}















