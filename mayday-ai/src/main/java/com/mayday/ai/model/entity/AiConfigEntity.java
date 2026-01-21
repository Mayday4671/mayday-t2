package com.mayday.ai.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 配置实体（核心）：
 * 一条记录代表一个可用的“模型通道”（provider+model+key+baseUrl+参数）。
 * 同一 (tenant_id + scene_code) 可以配置多条，用于权重轮询与失败回退。
 */
@Data
@Table("ai_config")
public class AiConfigEntity {

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 租户ID：* 表示全局默认 */
    private String tenantId;

    /** 场景编码：article_generate/summary/qa... */
    private String sceneCode;

    /** 提供商：google/openai/deepseek/ollama... */
    private String provider;

    /** 模型名称：gemini-1.5-flash/gpt-4o-mini/deepseek-chat... */
    private String modelName;

    /** 自定义请求地址：代理/兼容接口/本地模型地址 */
    private String baseUrl;

    /** 关联 ai_key.id */
    private Long keyId;

    /** temperature：0~2（越大越发散） */
    private BigDecimal temperature;

    /** max_tokens：最大输出 tokens（建议控成本） */
    private Integer maxTokens;

    /** timeout_ms：请求超时 */
    private Integer timeoutMs;

    /** priority：越小越优先（主选组），失败后尝试更大priority */
    private Integer priority;

    /** weight：同priority组内按权重选择 */
    private Integer weight;

    /** enabled：1启用/0停用 */
    private Integer enabled;

    /** version：配置版本，用于缓存失效 */
    private Integer version;

    /** 备注 */
    private String remark;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
