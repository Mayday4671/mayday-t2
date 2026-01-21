package com.mayday.ai.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 调用日志查询参数
 * 
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class AiCallLogQuery implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String requestId;
    private String sceneCode;
    private String provider;
    private Integer success;
}
