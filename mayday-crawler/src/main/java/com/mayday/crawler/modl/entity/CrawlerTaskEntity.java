package com.mayday.crawler.modl.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫任务实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_task")
@Schema(description = "爬虫任务实体")
public class CrawlerTaskEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务描述")
    private String taskDesc;

    @Schema(description = "起始URL列表（JSON数组）")
    private String startUrls;

    @Schema(description = "爬取类型：ARTICLE-文章 IMAGE-图片 BOTH-文章+图片")
    private String crawlType;

    @Schema(description = "最大爬取深度")
    private Integer maxDepth;

    @Schema(description = "站点范围：SITE-仅站内 ALL-全站")
    private String scopeType;

    @Schema(description = "请求间隔（毫秒）")
    private Integer requestInterval;

    @Schema(description = "请求超时（毫秒）")
    private Integer requestTimeout;

    @Schema(description = "最大重试次数")
    private Integer maxRetries;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "自定义请求头（JSON）")
    private String headers;

    @Schema(description = "Cookie配置（JSON）")
    private String cookies;

    @Schema(description = "Referer")
    private String referer;

    @Schema(description = "是否使用代理：0-否 1-是")
    private Integer useProxy;

    @Schema(description = "代理列表（JSON数组）")
    private String proxyList;

    @Schema(description = "是否随机化间隔：0-否 1-是")
    private Integer randomInterval;

    @Schema(description = "是否轮换User-Agent：0-否 1-是")
    private Integer rotateUserAgent;

    @Schema(description = "列表页最大翻页数（仅列表页场景，默认1）")
    private Integer listMaxPages;

    @Schema(description = "是否下载图片：0-否 1-是")
    private Integer downloadImages;

    @Schema(description = "正文容器选择器（CSS选择器，用于定位正文区域，如：#conttpc, .content, article等）")
    private String contentSelector;

    @Schema(description = "图片选择器（CSS选择器，用于定位正文中的图片，如：img, .post-content img等。如果为空，则从正文容器中提取所有img）")
    private String imageSelector;

    @Schema(description = "排除选择器（CSS选择器，用于排除不需要的区域，如：.related, .recommend, aside等）")
    private String excludeSelector;

    @Schema(description = "任务状态：NOT_STARTED-未启动 RUNNING-运行中 PAUSED-已暂停 COMPLETED-已完成 ERROR-异常 STOPPED-已停止")
    private String status;

    @Schema(description = "总URL数")
    private Integer totalUrls;

    @Schema(description = "已爬取URL数")
    private Integer crawledUrls;

    @Schema(description = "成功数")
    private Integer successCount;

    @Schema(description = "失败数")
    private Integer errorCount;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "所属部门ID")
    private Long deptId;

    /**
     * 请求参数容器（不映射到数据库）
     */
    @Column(ignore = true)
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }
}
