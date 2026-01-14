package com.mayday.crawler.modl.dto;

import lombok.Data;

import java.util.Date;

/**
 * 按文章聚合分页结果DTO
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerImageArticleCoverDTO {

    private Long articleId;
    private Long taskId;
    private String articleTitle;
    private String articleUrl;
    private String sourceSite;
    private Date publishTime;
    private Date articleCreateTime;
    private String coverUrl;
    
    // 封面图片本地信息
    private String coverFilePath;
    private String coverFileName;
    private String coverDownloadStatus;

    private Long imageCount;
}
