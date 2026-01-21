package com.mayday.ai.service;



import com.mayday.ai.model.dto.AiKeyQuery;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * AI Key 管理服务
 *
 * 注意：
 * - 数据库只存密文（key_cipher）
 * - 明文 key 只在创建时出现一次，不应回显
 */
public interface AiKeyService extends IService<AiKeyEntity> {

    /**
     * 分页查询
     */
    Page<AiKeyEntity> page(AiKeyQuery query);

    /**
     * 创建一条 API Key（明文入参 -> 加密后入库）
     *
     * @param provider     提供商编码（google/openai/deepseek/ollama...）
     * @param name         key 名称（方便运维识别）
     * @param apiKeyPlain  明文 key（仅本次调用使用）
     * @param remark       备注
     * @return 新 key 的 id
     */
    Long createKey(String provider, String name, String apiKeyPlain, String remark);

    /**
     * 禁用 key（不删除，便于审计）
     */
    void disableKey(Long keyId);

    /**
     * 启用 key
     */
    void enableKey(Long keyId);
}