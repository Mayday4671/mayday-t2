package com.mayday.ai.model.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 配置 DTO
 * 
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class AiConfigDto implements Serializable {

    private Long id;

    private String tenantId;
    private String sceneCode;
    private String provider;
    private String modelName;
    private String baseUrl;
    private Long keyId;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutMs;
    private Integer priority;
    private Integer weight;
    private Integer enabled;
    private String remark;
}
