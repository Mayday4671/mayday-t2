package com.mayday.server.controller.crawler;

import com.mayday.common.sse.SsePublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 爬虫任务SSE推送接口
 * 
 * 提供任务状态的实时推送，前端订阅后可接收任务状态变化事件。
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/crawlerTask/sse")
@Tag(name = "爬虫任务SSE推送")
@RequiredArgsConstructor
public class TaskSseController {

    /**
     * SSE主题：任务状态
     */
    public static final String TOPIC_TASK_STATUS = "crawler-task-status";

    private final SsePublisher ssePublisher;

    /**
     * 订阅任务状态推送
     * 
     * 前端使用 EventSource 订阅此端点，接收事件类型：
     * - connected: 连接成功
     * - task.started: 任务启动
     * - task.progress: 任务进度更新
     * - task.completed: 任务完成
     * - task.stopped: 任务停止
     * - task.error: 任务错误
     *
     * @return SseEmitter
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅任务状态推送")
    @PreAuthorize("hasAuthority('crawler:task:list')")
    public SseEmitter subscribe() {
        return ssePublisher.subscribe(TOPIC_TASK_STATUS);
    }
}
