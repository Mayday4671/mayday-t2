package com.mayday.ai.service.impl;

import com.mayday.ai.mapper.AiKeyMapper;
import com.mayday.ai.model.dto.AiKeyQuery;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mayday.ai.security.KeyCipher;
import com.mayday.ai.service.AiKeyService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.mayday.ai.model.entity.table.AiKeyEntityTableDef.AI_KEY_ENTITY;

/**
 * AI Key 管理实现
 */
@Service
@RequiredArgsConstructor
public class AiKeyServiceImpl extends ServiceImpl<AiKeyMapper, AiKeyEntity> implements AiKeyService {

    private final AiKeyMapper aiKeyMapper;
    private final KeyCipher keyCipher;

    @Override
    public Page<AiKeyEntity> page(AiKeyQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        
        // 只有非空时才添加条件
        if (query.getProvider() != null && !query.getProvider().isEmpty()) {
            qw.and(AI_KEY_ENTITY.PROVIDER.eq(query.getProvider()));
        }
        if (query.getName() != null && !query.getName().isEmpty()) {
            qw.and(AI_KEY_ENTITY.NAME.like(query.getName()));
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            qw.and(AI_KEY_ENTITY.STATUS.eq(query.getStatus()));
        }
        qw.orderBy(AI_KEY_ENTITY.CREATE_TIME.desc());
        
        return page(new Page<>(query.getPageNum(), query.getPageSize()), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKey(String provider, String name, String apiKeyPlain, String remark) {
        AiKeyEntity e = new AiKeyEntity();
        e.setProvider(provider);
        e.setName(name);

        // ✅ 明文加密后入库，严禁明文存储
        // 自检：保证 decrypt 能还原
        String cipher = keyCipher.encrypt(apiKeyPlain);
        // ✅ decrypt 的参数必须是 cipher（密文）
        String check = keyCipher.decrypt(cipher);
        if (!apiKeyPlain.equals(check)) {
            throw new IllegalStateException("KeyCipher self-check failed");
        }
        e.setKeyCipher(cipher);

        e.setStatus("ACTIVE");
        e.setRemark(remark);

        aiKeyMapper.insertSelective(e);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableKey(Long keyId) {
        AiKeyEntity upd = new AiKeyEntity();
        upd.setId(keyId);
        upd.setStatus("DISABLED");
        aiKeyMapper.update(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableKey(Long keyId) {
        AiKeyEntity upd = new AiKeyEntity();
        upd.setId(keyId);
        upd.setStatus("ACTIVE");
        aiKeyMapper.update(upd);
    }
}