package com.mayday.ai.model.vo;

import lombok.Data;


/**
 * AI 对话请求 DTO
 */
@Data
public class ChatReq {

    /** 业务场景（对应 ai_config.scene_code），默认 chat */
    private String sceneCode = "chat";

    /** 租户ID（单租户可固定 "*"） */
    private String tenantId = "*";

    /** 用户输入 */
    private String prompt;
}