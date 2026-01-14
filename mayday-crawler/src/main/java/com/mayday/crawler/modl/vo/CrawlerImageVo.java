package com.mayday.crawler.modl.vo;

import lombok.Data;

import java.util.Date;

/**
 * 图片VO（用于前端展示）
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class CrawlerImageVo {

    private Long id;
    private Long taskId;
    private Long articleId;
    private String url;
    private String urlHash;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String format;
    private String md5;
    private String downloadStatus;
    private String errorMsg;
    private Date createTime;
    private Date updateTime;

    /**
     * 前端展示URL（优先使用本地路径）
     */
    private String displayUrl;
}
