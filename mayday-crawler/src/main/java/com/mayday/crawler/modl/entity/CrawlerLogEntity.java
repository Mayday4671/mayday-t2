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
 * 爬虫日志实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_log")
@Schema(description = "爬虫日志实体")
public class CrawlerLogEntity {

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "日志类型：REQUEST-请求 PARSE-解析 DOWNLOAD-下载 ERROR-错误")
    private String logType;

    @Schema(description = "相关URL")
    private String url;

    @Schema(description = "日志消息")
    private String message;

    @Schema(description = "日志级别：DEBUG INFO WARN ERROR")
    private String level;

    @Schema(description = "异常信息")
    private String exceptionInfo;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "所属部门ID")
    private Long deptId;
}
