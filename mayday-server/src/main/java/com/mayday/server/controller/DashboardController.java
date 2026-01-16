package com.mayday.server.controller;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mayday.crawler.service.ICrawlerLogService;
import com.mayday.crawler.service.ICrawlerTaskService;
import com.mayday.server.model.dto.DashboardStatsDTO;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mayday.crawler.modl.entity.table.CrawlerArticleEntityTableDef.CRAWLER_ARTICLE_ENTITY;
import static com.mayday.crawler.modl.entity.table.CrawlerImageEntityTableDef.CRAWLER_IMAGE_ENTITY;
import static com.mayday.crawler.modl.entity.table.CrawlerTaskEntityTableDef.CRAWLER_TASK_ENTITY;

/**
 * 仪表盘统计接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "仪表盘统计接口")
@RequiredArgsConstructor
public class DashboardController {

    private final ICrawlerTaskService taskService;
    private final ICrawlerArticleService articleService;
    private final ICrawlerImageService imageService;
    private final ICrawlerLogService logService;

    /**
     * 获取仪表盘统计数据
     *
     * @return 统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public AjaxResult getStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // ========== 基础统计 ==========
        // 任务总数
        stats.setTaskCount(taskService.count());

        // 运行中任务数
        QueryWrapper runningQuery = QueryWrapper.create()
                .where(CRAWLER_TASK_ENTITY.STATUS.eq("RUNNING"));
        stats.setRunningTaskCount(taskService.count(runningQuery));

        // 文章总数
        stats.setArticleCount(articleService.count());

        // 今日新增文章数
        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        QueryWrapper todayArticleQuery = QueryWrapper.create()
                .where(CRAWLER_ARTICLE_ENTITY.CREATE_TIME.ge(todayStart));
        stats.setTodayArticleCount(articleService.count(todayArticleQuery));

        // 图片总数
        stats.setImageCount(imageService.count());

        // 今日新增图片数
        QueryWrapper todayImageQuery = QueryWrapper.create()
                .where(CRAWLER_IMAGE_ENTITY.CREATE_TIME.ge(todayStart));
        stats.setTodayImageCount(imageService.count(todayImageQuery));

        // 日志总数
        stats.setLogCount(logService.count());

        // ========== 任务状态分布（饼图） ==========
        List<Map<String, Object>> taskStatusDistribution = new ArrayList<>();
        String[] statuses = {"NOT_STARTED", "RUNNING", "PAUSED", "COMPLETED", "ERROR", "STOPPED"};
        String[] statusLabels = {"未启动", "运行中", "已暂停", "已完成", "异常", "已停止"};

        for (int i = 0; i < statuses.length; i++) {
            QueryWrapper statusQuery = QueryWrapper.create()
                    .where(CRAWLER_TASK_ENTITY.STATUS.eq(statuses[i]));
            long count = taskService.count(statusQuery);
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", statusLabels[i]);
                item.put("value", count);
                taskStatusDistribution.add(item);
            }
        }
        stats.setTaskStatusDistribution(taskStatusDistribution);

        // ========== 近7天文章趋势（折线图） ==========
        List<Map<String, Object>> articleTrend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Date dateStart = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date dateEnd = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            QueryWrapper dateQuery = QueryWrapper.create()
                    .where(CRAWLER_ARTICLE_ENTITY.CREATE_TIME.ge(dateStart))
                    .and(CRAWLER_ARTICLE_ENTITY.CREATE_TIME.lt(dateEnd));
            long count = articleService.count(dateQuery);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(formatter));
            item.put("value", count);
            articleTrend.add(item);
        }
        stats.setArticleTrend(articleTrend);

        // ========== 近7天图片趋势（折线图） ==========
        List<Map<String, Object>> imageTrend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Date dateStart = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date dateEnd = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            QueryWrapper dateQuery = QueryWrapper.create()
                    .where(CRAWLER_IMAGE_ENTITY.CREATE_TIME.ge(dateStart))
                    .and(CRAWLER_IMAGE_ENTITY.CREATE_TIME.lt(dateEnd));
            long count = imageService.count(dateQuery);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(formatter));
            item.put("value", count);
            imageTrend.add(item);
        }
        stats.setImageTrend(imageTrend);

        return AjaxResult.success(stats);
    }
}

