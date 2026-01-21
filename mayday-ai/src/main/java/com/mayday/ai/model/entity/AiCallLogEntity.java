package com.mayday.ai.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志：
 * - 每一次“尝试调用”（包括回退）都写一条
 * - 禁止记录敏感内容（prompt、key）
 */
@Data
@Table("ai_call_log")
public class AiCallLogEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 请求ID：同一次业务调用链路一致 */
    private String requestId;

    private String tenantId;
    private String sceneCode;

    /** 命中的配置ID（每次尝试写一次） */
    private Long configId;

    private String provider;
    private String modelName;

    /** 1成功/0失败 */
    private Integer success;

    /** 耗时毫秒 */
    private Integer latencyMs;

    /** 错误码：异常类名/供应商错误码 */
    private String errorCode;

    /** 错误信息短句：截断存储 */
    private String errorMsg;

    /** tokens（可选） */
    private Integer promptTokens;
    private Integer completionTokens;

    private LocalDateTime createTime;
}
