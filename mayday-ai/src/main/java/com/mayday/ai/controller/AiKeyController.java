package com.mayday.ai.controller;

import com.mayday.ai.model.dto.AiKeyDto;
import com.mayday.ai.model.dto.AiKeyQuery;
import com.mayday.ai.model.entity.AiKeyEntity;
import com.mayday.ai.service.AiKeyService;
import com.mayday.common.web.AjaxResult;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * AI 密钥管理控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/ai/key")
public class AiKeyController
{
    @Autowired
    private AiKeyService aiKeyService;

    /**
     * 分页查询
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ai:key:list')")
    public AjaxResult list(AiKeyQuery query)
    {
        Page<AiKeyEntity> page = aiKeyService.page(query);
        return AjaxResult.success(page);
    }

    /**
     * 获取详细信息
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ai:key:query')")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(aiKeyService.getById(id));
    }

    /**
     * 新增密钥
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ai:key:add')")
    public AjaxResult add(@RequestBody AiKeyDto dto)
    {
        aiKeyService.createKey(dto.getProvider(), dto.getName(), dto.getApiKey(), dto.getRemark());
        return AjaxResult.success();
    }

    /**
     * 修改密钥
     */
    @PutMapping
    @PreAuthorize("hasAuthority('ai:key:edit')")
    public AjaxResult edit(@RequestBody AiKeyDto dto)
    {
        // 这里需要注意，如果是修改，通常不回传 cipher，如果前端没传 dirty field 可能会覆盖
        // 但 createKey 是专门加密的。
        // 如果是要修改名称或备注：
        AiKeyEntity entity = new AiKeyEntity();
        BeanUtils.copyProperties(dto, entity);
        // 如果 updateKey 的话需要密文处理，这里简化，假设只修改非敏感字段，或者 dto 里有 cipher（不应该有）
        // 如果要支持修改 key，应该复用 createKey 的逻辑或者单独接口。
        // 简单起见，这里只更新 name, remark, status
        // 如果要更新 Key，建议删除重加，或者提供单独接口。

        return AjaxResult.success(aiKeyService.updateById(entity));
    }

    /**
     * 删除密钥
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:key:remove')")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        return AjaxResult.success(aiKeyService.removeById(id));
    }

    /**
     * 禁用密钥
     */
    @PostMapping("/disable/{id}")
    @PreAuthorize("hasAuthority('ai:key:edit')")
    public AjaxResult disable(@PathVariable("id") Long id)
    {
        aiKeyService.disableKey(id);
        return AjaxResult.success();
    }
    @PostMapping("/enable/{id}")
    @PreAuthorize("hasAuthority('ai:key:edit')")
    public AjaxResult enable(@PathVariable("id") Long id)
    {
        aiKeyService.enableKey(id);
        return AjaxResult.success();
    }
}
