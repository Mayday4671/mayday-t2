package com.mayday.server.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据 DTO
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class DashboardStatsDTO {

    // ========== 基础统计 ==========

    /**
     * 任务总数
     */
    private Long taskCount;

    /**
     * 运行中任务数
     */
    private Long runningTaskCount;

    /**
     * 文章总数
     */
    private Long articleCount;

    /**
     * 今日新增文章数
     */
    private Long todayArticleCount;

    /**
     * 图片总数
     */
    private Long imageCount;

    /**
     * 今日新增图片数
     */
    private Long todayImageCount;

    /**
     * 日志总数
     */
    private Long logCount;

    // ========== 图表数据 ==========

    /**
     * 任务状态分布（饼图数据）
     * key: 状态名称, value: 数量
     */
    private List<Map<String, Object>> taskStatusDistribution;

    /**
     * 近7天文章趋势（折线图数据）
     * 每项包含: date（日期）, count（数量）
     */
    private List<Map<String, Object>> articleTrend;

    /**
     * 近7天图片趋势（折线图数据）
     * 每项包含: date（日期）, count（数量）
     */
    private List<Map<String, Object>> imageTrend;
}
