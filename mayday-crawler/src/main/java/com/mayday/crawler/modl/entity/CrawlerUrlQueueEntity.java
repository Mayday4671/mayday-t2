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
 * URL队列表实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_url_queue")
@Schema(description = "URL队列表实体")
public class CrawlerUrlQueueEntity {

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "URL地址")
    private String url;

    @Schema(description = "URL哈希值（用于去重）")
    private String urlHash;

    @Schema(description = "URL深度")
    private Integer depth;

    @Schema(description = "优先级（数字越大优先级越高）")
    private Integer priority;

    @Schema(description = "状态：PENDING-待处理 PROCESSING-处理中 SUCCESS-成功 FAILED-失败")
    private String status;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
