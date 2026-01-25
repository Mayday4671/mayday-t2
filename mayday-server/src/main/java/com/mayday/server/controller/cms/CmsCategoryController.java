package com.mayday.server.controller.cms;

import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.entity.CmsCategoryEntity;
import com.mayday.crawler.service.ICmsCategoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/cms/category")
@Tag(name = "CMS分类管理")
@RequiredArgsConstructor
public class CmsCategoryController {

    private final ICmsCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "分类列表")
    public AjaxResult list() {
        // 后台管理通常列出所有，支持排序
        List<CmsCategoryEntity> list = categoryService.list(
                new QueryWrapper().orderBy("sort", true).orderBy("id", false)
        );
        return AjaxResult.success(list);
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public AjaxResult add(@RequestBody CmsCategoryEntity entity) {
        // Check uniqueness of code
        long count = categoryService.count(new QueryWrapper().eq("code", entity.getCode()));
        if (count > 0) {
            return AjaxResult.error("分类编码已存在");
        }
        categoryService.save(entity);
        return AjaxResult.success();
    }

    @PutMapping
    @Operation(summary = "修改分类")
    public AjaxResult update(@RequestBody CmsCategoryEntity entity) {
        // Check uniqueness if code changed
        CmsCategoryEntity old = categoryService.getById(entity.getId());
        if (!old.getCode().equals(entity.getCode())) {
            long count = categoryService.count(new QueryWrapper().eq("code", entity.getCode()));
            if (count > 0) {
                return AjaxResult.error("分类编码已存在");
            }
        }
        categoryService.updateById(entity);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public AjaxResult delete(@PathVariable("id") Long id) {
        categoryService.removeById(id);
        return AjaxResult.success();
    }
}
