package com.mayday.ai.controller;

import com.mayday.ai.model.dto.AiConfigDto;
import com.mayday.ai.model.dto.AiConfigQuery;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.service.AiConfigService;
import com.mayday.common.web.AjaxResult;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 配置管理控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    /**
     * 分页查询
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ai:config:list')")
    public AjaxResult list(AiConfigQuery query) {
        Page<AiConfigEntity> page = aiConfigService.page(query);
        return AjaxResult.success(page);
    }

    /**
     * 获取详细信息
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ai:config:query')")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(aiConfigService.getById(id));
    }

    /**
     * 新增配置
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ai:config:add')")
    public AjaxResult add(@RequestBody AiConfigDto dto) {
        AiConfigEntity entity = new AiConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        // 新增时版本初始化
        entity.setVersion(1);
        return AjaxResult.success(aiConfigService.save(entity));
    }

    /**
     * 修改配置
     */
    @PutMapping
    @PreAuthorize("hasAuthority('ai:config:edit')")
    public AjaxResult edit(@RequestBody AiConfigDto dto) {
        AiConfigEntity entity = new AiConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        // 更新并 bump version
        aiConfigService.updateAndBumpVersion(entity);
        return AjaxResult.success();
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:config:remove')")
    public AjaxResult remove(@PathVariable("id") Long id) {
        return AjaxResult.success(aiConfigService.removeById(id));
    }

    /**
     * 启用/停用
     */
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('ai:config:edit')")
    public AjaxResult switchStatus(@RequestBody AiConfigDto dto) {
       // dto used for id and enabled
       boolean enabled = dto.getEnabled() != null && dto.getEnabled() == 1;
       aiConfigService.setEnabled(dto.getId(), enabled);
       return AjaxResult.success();
    }
}
