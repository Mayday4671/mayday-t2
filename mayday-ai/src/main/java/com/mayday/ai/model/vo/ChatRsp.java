package com.mayday.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 对话响应 DTO
 */
@Data
@AllArgsConstructor
public class ChatRsp {

    /** 模型输出 */
    private String result;
}