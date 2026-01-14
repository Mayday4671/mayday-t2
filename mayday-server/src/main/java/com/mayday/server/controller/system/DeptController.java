package com.mayday.server.controller.system;

import com.mayday.auth.common.R;
import com.mayday.auth.entity.SysDept;
import com.mayday.auth.mapper.SysDeptMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

import static com.mayday.auth.entity.table.SysDeptTableDef.SYS_DEPT;

/**
 * 部门管理控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptMapper deptMapper;

    /**
     * 获取部门列表（带数据权限过滤）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<Map<String, Object>>> list() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        List<SysDept> allDepts;
        
        // 根据数据权限过滤部门
        String dataScope = "5"; // 默认仅本人
        if (loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
            dataScope = loginUser.getRoles().get(0).getDataScope();
        }
        
        switch (dataScope) {
            case "1" -> {
                // 全部数据权限
                allDepts = deptMapper.selectAll();
            }
            case "2", "4" -> {
                // 本部门及以下：支持多部门聚合
                List<Long> deptIds = loginUser.getAllDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    allDepts = getDeptsAndChildren(deptIds);
                } else {
                    allDepts = new ArrayList<>();
                }
            }
            case "3", "5" -> {
                // 本部门/仅本人：支持多部门聚合（不含下级）
                List<Long> deptIds = loginUser.getAllDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    allDepts = getDeptsOnly(deptIds);
                } else {
                    allDepts = new ArrayList<>();
                }
            }
            default -> {
                allDepts = new ArrayList<>();
            }
        }
        
        // 构建树形结构
        List<Map<String, Object>> tree = buildDeptTree(allDepts);
        log.info("用户 {} 的部门权限树: {}", loginUser.getUsername(), tree);
        
        return R.ok(tree);
    }

    /**
     * 获取部门详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:query')")
    public R<SysDept> getInfo(@PathVariable("id") Long id) {
        SysDept dept = deptMapper.selectOneById(id);
        return dept != null ? R.ok(dept) : R.fail("部门不存在");
    }

    /**
     * 新增部门
     * <p>
     * 权限校验：用户只能在自己有权限的部门下创建子部门
     * </p>
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public R<Void> add(@Valid @RequestBody DeptRequest request) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        // 1. 获取用户的数据权限范围
        String dataScope = "5"; // 默认仅本人
        if (loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
            dataScope = loginUser.getRoles().get(0).getDataScope();
        }
        
        // 2. 只有全部数据权限(1)的用户才能创建顶级部门
        Long parentId = request.getParentId();
        if (parentId == null || parentId == 0) {
            if (!"1".equals(dataScope)) {
                log.warn("用户 {} 尝试创建顶级部门，但没有权限 (dataScope={})", 
                        loginUser.getUsername(), dataScope);
                return R.fail("您没有权限创建顶级部门，请选择上级部门");
            }
        } else {
            // 3. 检查用户是否有权限在指定父部门下创建
            List<Long> allowedDeptIds = loginUser.getAllDeptIds();
            if (!"1".equals(dataScope)) {
                // 非全部权限：检查父部门是否在用户权限范围内
                boolean hasPermission = false;
                
                if ("2".equals(dataScope) || "4".equals(dataScope)) {
                    // 本部门及以下：需要检查父部门是否在用户部门树下
                    hasPermission = isInDeptTree(parentId, allowedDeptIds);
                } else if ("3".equals(dataScope) || "5".equals(dataScope)) {
                    // 仅本部门/本人：只能在自己部门下创建
                    hasPermission = allowedDeptIds != null && allowedDeptIds.contains(parentId);
                }
                
                if (!hasPermission) {
                    log.warn("用户 {} 尝试在部门 {} 下创建子部门，但没有权限", 
                            loginUser.getUsername(), parentId);
                    return R.fail("您没有权限在该部门下创建子部门");
                }
            }
        }
        
        SysDept dept = new SysDept();
        dept.setParentId(parentId);
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum());
        
        // 构建 ancestors
        if (parentId != null && parentId > 0) {
            SysDept parent = deptMapper.selectOneById(parentId);
            if (parent != null) {
                dept.setAncestors(parent.getAncestors() + "," + parentId);
            }
        } else {
            dept.setAncestors("0");
        }
        
        deptMapper.insert(dept);
        log.info("用户 {} 新增部门: {} (父部门ID: {})", 
                loginUser.getUsername(), request.getDeptName(), parentId);
        return R.ok();
    }
    
    /**
     * 检查目标部门是否在用户的部门树下（包括子部门）
     */
    private boolean isInDeptTree(Long targetDeptId, List<Long> userDeptIds) {
        if (userDeptIds == null || userDeptIds.isEmpty()) {
            return false;
        }
        
        // 如果目标部门就是用户的部门之一，直接返回true
        if (userDeptIds.contains(targetDeptId)) {
            return true;
        }
        
        // 检查目标部门的祖先是否包含用户的部门
        SysDept targetDept = deptMapper.selectOneById(targetDeptId);
        if (targetDept != null && targetDept.getAncestors() != null) {
            for (Long deptId : userDeptIds) {
                if (targetDept.getAncestors().contains(String.valueOf(deptId))) {
                    return true;
                }
            }
        }
        
        return false;
    }


    /**
     * 修改部门
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public R<Void> update(@PathVariable("id") Long id, @Valid @RequestBody DeptRequest request) {
        SysDept dept = deptMapper.selectOneById(id);
        if (dept == null) {
            return R.fail("部门不存在");
        }
        
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum());
        deptMapper.update(dept);
        
        log.info("更新部门: {}", request.getDeptName());
        return R.ok();
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:remove')")
    public R<Void> delete(@PathVariable("id") Long id) {
        // 检查是否有子部门
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_DEPT.PARENT_ID.eq(id));
        if (deptMapper.selectCountByQuery(wrapper) > 0) {
            return R.fail("存在子部门，不能删除");
        }
        
        deptMapper.deleteById(id);
        log.info("删除部门: {}", id);
        return R.ok();
    }

    /**
     * 获取指定部门列表及其所有子部门 (多部门聚合)
     */
    private List<SysDept> getDeptsAndChildren(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        
        // 查询所有 ID 在列表中的部门 OR 祖先包含列表 ID 的部门
        QueryWrapper wrapper = QueryWrapper.create().from(SYS_DEPT);
        
        // 构造动态 OR 条件：id IN (...) OR FIND_IN_SET(id1, ancestors) OR FIND_IN_SET(id2, ancestors)...
        wrapper.where(SYS_DEPT.ID.in(deptIds));
        for (Long deptId : deptIds) {
            wrapper.or("FIND_IN_SET(" + deptId + ", ancestors)");
        }
        
        return deptMapper.selectListByQuery(wrapper);
    }

    /**
     * 获取指定部门列表 (仅限本级，支持多部门)
     */
    private List<SysDept> getDeptsOnly(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        return deptMapper.selectListByIds(deptIds);
    }

    /**
     * 构建部门树形结构
     * <p>
     * 自动找到列表中的最顶层节点作为根节点
     * </p>
     */
    private List<Map<String, Object>> buildDeptTree(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return List.of();
        }
        
        // 找到列表中所有部门的ID集合
        Set<Long> deptIds = depts.stream().map(SysDept::getId).collect(Collectors.toSet());
        
        // 找到列表中的根节点（其 parentId 不在列表中的节点）
        List<SysDept> rootDepts = depts.stream()
                .filter(d -> !deptIds.contains(d.getParentId()))
                .sorted(Comparator.comparing(SysDept::getOrderNum))
                .collect(Collectors.toList());
        
        // 递归构建树
        return rootDepts.stream()
                .map(d -> buildTreeNode(d, depts))
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> buildTreeNode(SysDept dept, List<SysDept> allDepts) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", dept.getId());
        node.put("deptName", dept.getDeptName());
        node.put("parentId", dept.getParentId());
        node.put("orderNum", dept.getOrderNum());
        
        // 查找子节点
        List<Map<String, Object>> children = allDepts.stream()
                .filter(d -> dept.getId().equals(d.getParentId()))
                .sorted(Comparator.comparing(SysDept::getOrderNum))
                .map(child -> buildTreeNode(child, allDepts))
                .collect(Collectors.toList());
        
        if (!children.isEmpty()) {
            node.put("children", children);
        }
        
        return node;
    }

    @Data
    public static class DeptRequest {
        @NotBlank(message = "部门名称不能为空")
        private String deptName;
        private Long parentId = 0L;
        private Integer orderNum = 0;
    }
}
