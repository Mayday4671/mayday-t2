package com.mayday.server.controller.cms;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.entity.SysPortalMenuEntity;
import com.mayday.crawler.service.ISysPortalMenuService;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/portal/menu")
@Tag(name = "门户菜单管理")
@RequiredArgsConstructor
public class SysPortalMenuController {

    private final ISysPortalMenuService menuService;

    @GetMapping("/list")
    @Operation(summary = "菜单列表")
    public AjaxResult list() {
        List<SysPortalMenuEntity> list = menuService.list(
                new QueryWrapper().orderBy("sort", true).orderBy("id", false)
        );
        return AjaxResult.success(list);
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    public AjaxResult add(@RequestBody SysPortalMenuEntity entity) {
        menuService.save(entity);
        return AjaxResult.success();
    }

    @PutMapping
    @Operation(summary = "修改菜单")
    public AjaxResult update(@RequestBody SysPortalMenuEntity entity) {
        menuService.updateById(entity);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    public AjaxResult delete(@PathVariable("id") Long id) {
        menuService.removeById(id);
        return AjaxResult.success();
    }
}
