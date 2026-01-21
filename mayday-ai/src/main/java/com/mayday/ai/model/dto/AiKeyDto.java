package com.mayday.ai.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 密钥 DTO
 * 
 * @author Antigravity
 * @since 1.0.0
 */
@Data
public class AiKeyDto implements Serializable {

    private Long id;

    /** 提供商编码 */
    private String provider;

    /** 密钥名称 */
    private String name;

    /** API Key 明文 (仅在新增/修改且不为空时处理) */
    private String apiKey;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
}
