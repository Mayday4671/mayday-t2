package com.mayday.ai.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 配置查询参数
 * 
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class AiConfigQuery implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String sceneCode;
    private String modelName;
    private String provider;
    private Integer enabled;
}
