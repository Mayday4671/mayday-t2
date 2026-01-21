package com.mayday.ai.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 密钥查询参数
 * 
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class AiKeyQuery implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 提供商 */
    private String provider;

    /** 名称 */
    private String name;

    /** 状态 */
    private String status;
}
