/**
 * 仪表盘统计 API
 *
 * @author Antigravity
 * @since 1.0.0
 */
import request from "../../utils/request";

/**
 * 仪表盘统计数据类型
 */
export interface DashboardStats {
    // 基础统计
    taskCount: number;
    runningTaskCount: number;
    articleCount: number;
    todayArticleCount: number;
    imageCount: number;
    todayImageCount: number;
    logCount: number;

    // 图表数据
    taskStatusDistribution: Array<{ type: string; value: number }>;
    articleTrend: Array<{ date: string; value: number }>;
    imageTrend: Array<{ date: string; value: number }>;
}

/**
 * 获取仪表盘统计数据
 */
export function fetchDashboardStats(): Promise<DashboardStats> {
    return request.get("/dashboard/stats");
}
