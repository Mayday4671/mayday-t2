package com.mayday.ai.controller.req;

import lombok.Data;


/**
 *
 */
@Data
public class CreateAiKeyReq {
    /** 提供商标识：google / openai / deepseek / ollama */
    private String provider;

    /** Key 名称（给人看的，比如：gemini-main-key） */
    private String name;

    /** 明文 API Key（只在本次请求出现，不回显、不落日志） */
    private String apiKey;

    /** 备注 */
    private String remark;
}