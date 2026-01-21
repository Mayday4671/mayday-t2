package com.mayday.crawler.modl.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 文章实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_article")
@Schema(description = "文章实体")
public class CrawlerArticleEntity {

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "文章URL")
    private String url;

    @Schema(description = "URL哈希值")
    private String urlHash;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章正文")
    private String content;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "发布时间")
    private Date publishTime;

    @Schema(description = "来源站点")
    private String sourceSite;

    @Schema(description = "内容哈希值（用于增量判断）")
    private String contentHash;

    @Schema(description = "是否已更新：0-否 1-是")
    private Integer isUpdated;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "审核状态：0-待审核 1-已发布 2-已驳回")
    private Integer status;

    @Schema(description = "来源类型：CRAWLER-爬虫采集 AI-AI生成 MANUAL-手动录入")
    private String sourceType;

    @Schema(description = "封面图URL")
    @com.mybatisflex.annotation.Column(ignore = true)
    private String coverImage;

    @Schema(description = "文章相关图片列表")
    @com.mybatisflex.annotation.Column(ignore = true)
    private java.util.List<String> imageList;
}
