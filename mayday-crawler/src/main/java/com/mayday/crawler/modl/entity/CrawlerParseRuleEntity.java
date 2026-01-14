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
 * 解析规则实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_parse_rule")
@Schema(description = "解析规则实体")
public class CrawlerParseRuleEntity {

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "规则类型：TITLE-标题 CONTENT-正文 AUTHOR-作者 PUBLISH_TIME-发布时间")
    private String ruleType;

    @Schema(description = "解析方法：CSS_SELECTOR XPATH")
    private String ruleMethod;

    @Schema(description = "规则值")
    private String ruleValue;

    @Schema(description = "优先级（数字越大优先级越高，用于多规则兜底）")
    private Integer priority;

    @Schema(description = "是否启用：0-否 1-是")
    private Integer isActive;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
