package com.mayday.ai.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 密钥实体：
 * - key_cipher 存储密文（AES-GCM base64）
 * - 读取时解密只在内存短暂存在，严禁写日志
 */
@Data
@Table("ai_key")
public class AiKeyEntity {

    /** 主键ID（自增） */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 提供商编码：google/openai/deepseek/ollama/... */
    private String provider;

    /** 密钥名称：便于运维识别 */
    private String name;

    /** API Key 密文（AES-GCM base64），严禁明文 */
    private String keyCipher;

    /** 状态：ACTIVE/DISABLED/REVOKED */
    private String status;

    /** 备注 */
    private String remark;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
