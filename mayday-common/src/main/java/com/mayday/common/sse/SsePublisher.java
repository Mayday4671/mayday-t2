package com.mayday.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 通用SSE事件发布器
 * 
 * 支持按主题(topic)订阅和发布事件，可被多个业务模块复用。
 * 
 * 使用示例：
 * <pre>
 * // 订阅
 * SseEmitter emitter = ssePublisher.subscribe("task-status");
 * 
 * // 发布
 * ssePublisher.publish("task-status", new SseEvent("task.progress", taskData));
 * </pre>
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@Component
public class SsePublisher {

    /**
     * 默认超时时间（30分钟）
     */
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 主题 -> 订阅者集合
     */
    private final Map<String, Set<SseEmitter>> topicEmitters = new ConcurrentHashMap<>();

    /**
     * 订阅指定主题
     *
     * @param topic 主题名称
     * @return SseEmitter 订阅器
     */
    public SseEmitter subscribe(String topic) {
        return subscribe(topic, DEFAULT_TIMEOUT);
    }

    /**
     * 订阅指定主题（自定义超时）
     *
     * @param topic   主题名称
     * @param timeout 超时时间（毫秒）
     * @return SseEmitter 订阅器
     */
    public SseEmitter subscribe(String topic, long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);

        Set<SseEmitter> emitters = topicEmitters.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);

        log.info("[SSE] 新订阅: topic={}, 当前订阅数={}", topic, emitters.size());

        // 注册回调：完成、超时、错误时移除
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] 订阅完成: topic={}", topic);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] 订阅超时: topic={}", topic);
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("[SSE] 订阅错误: topic={}, error={}", topic, e.getMessage());
        });

        // 发送连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"topic\":\"" + topic + "\"}"));
        } catch (IOException e) {
            log.warn("[SSE] 发送连接事件失败: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * 向指定主题发布事件
     *
     * @param topic 主题名称
     * @param event 事件对象
     */
    public void publish(String topic, SseEvent event) {
        Set<SseEmitter> emitters = topicEmitters.get(topic);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventName())
                        .data(event.getData()));
            } catch (IOException e) {
                // 发送失败，移除该订阅者
                emitters.remove(emitter);
                log.debug("[SSE] 发送失败，移除订阅: topic={}", topic);
            }
        }
    }

    /**
     * 向指定主题发布简单消息
     *
     * @param topic     主题名称
     * @param eventName 事件名称
     * @param data      事件数据
     */
    public void publish(String topic, String eventName, Object data) {
        publish(topic, new SseEvent(eventName, data));
    }

    /**
     * 获取指定主题的订阅者数量
     *
     * @param topic 主题名称
     * @return 订阅者数量
     */
    public int getSubscriberCount(String topic) {
        Set<SseEmitter> emitters = topicEmitters.get(topic);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * 关闭指定主题的所有订阅
     *
     * @param topic 主题名称
     */
    public void closeTopic(String topic) {
        Set<SseEmitter> emitters = topicEmitters.remove(topic);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
            log.info("[SSE] 关闭主题: topic={}, 订阅数={}", topic, emitters.size());
        }
    }
}
