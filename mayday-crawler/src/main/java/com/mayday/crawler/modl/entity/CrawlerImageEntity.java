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
 * 图片实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_image")
@Schema(description = "图片实体")
public class CrawlerImageEntity {

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "关联文章ID")
    private Long articleId;

    @Schema(description = "图片URL")
    private String url;

    @Schema(description = "URL哈希值")
    private String urlHash;

    @Schema(description = "保存的文件名")
    private String fileName;

    @Schema(description = "文件存储路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "图片格式（jpg/png/gif等）")
    private String format;

    @Schema(description = "文件MD5值（用于去重）")
    private String md5;

    @Schema(description = "下载状态：PENDING-待下载 SUCCESS-成功 FAILED-失败")
    private String downloadStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
