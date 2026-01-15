package com.mayday.common.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE事件对象
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {

    /**
     * 事件名称（前端通过 event.type 获取）
     */
    private String eventName;

    /**
     * 事件数据（JSON序列化后发送）
     */
    private Object data;
}
