package com.mayday.server.controller.system;

import com.mayday.auth.common.R;
import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysRoleMenu;
import com.mayday.auth.mapper.SysRoleMapper;
import com.mayday.auth.mapper.SysRoleMenuMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mayday.auth.entity.table.SysRoleTableDef.SYS_ROLE;
import static com.mayday.auth.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;

/**
 * 角色管理控制器
 *
 * @author MayDay Auth Generator
 */
@Slf4j
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * 获取角色列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<Map<String, Object>> list() {
        List<SysRole> roles = roleMapper.selectAll();
        return R.ok(Map.of("list", roles, "total", roles.size()));
    }

    /**
     * 获取所有角色（用于下拉选择）
     */
    @GetMapping("/all")
    public R<List<SysRole>> all() {
        return R.ok(roleMapper.selectAll());
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:query')")
    public R<Map<String, Object>> getInfo(@PathVariable("id") Long id) {
        SysRole role = roleMapper.selectOneById(id);
        if (role == null) {
            return R.fail("角色不存在");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", role.getId());
        data.put("roleKey", role.getRoleKey());
        data.put("roleName", role.getRoleName());
        data.put("dataScope", role.getDataScope());
        data.put("menuIds", getMenuIdsByRoleId(id));
        
        return R.ok(data);
    }

    /**
     * 新增角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Void> add(@Valid @RequestBody AddRoleRequest request) {
        // 校验角色标识唯一
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_ROLE.ROLE_KEY.eq(request.getRoleKey()));
        if (roleMapper.selectCountByQuery(wrapper) > 0) {
            return R.fail("角色标识已存在");
        }
        
        // 创建角色
        SysRole role = new SysRole();
        role.setRoleKey(request.getRoleKey());
        role.setRoleName(request.getRoleName());
        role.setDataScope(request.getDataScope());
        roleMapper.insert(role);
        
        // 分配菜单
        if (request.getMenuIds() != null) {
            for (Long menuId : request.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        
        log.info("新增角色: {}", request.getRoleName());
        return R.ok();
    }

    /**
     * 修改角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateRoleRequest request) {
        SysRole role = roleMapper.selectOneById(id);
        if (role == null) {
            return R.fail("角色不存在");
        }
        
        role.setRoleName(request.getRoleName());
        role.setDataScope(request.getDataScope());
        roleMapper.update(role);
        
        log.info("更新角色: {}", role.getRoleName());
        return R.ok();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:remove')")
    public R<Void> delete(@PathVariable("id") Long id) {
        SysRole role = roleMapper.selectOneById(id);
        if (role == null) {
            return R.fail("角色不存在");
        }
        
        // 删除角色菜单关联
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.eq(id)));
        roleMapper.deleteById(id);
        
        log.info("删除角色: {}", role.getRoleName());
        return R.ok();
    }

    /**
     * 分配菜单权限
     */
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignMenus(@PathVariable("id") Long id, @RequestBody AssignMenusRequest request) {
        // 清除旧菜单
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.eq(id)));
        
        // 分配新菜单
        for (Long menuId : request.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(id);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        
        log.info("角色 {} 分配菜单: {}", id, request.getMenuIds());
        return R.ok();
    }

    private List<Long> getMenuIdsByRoleId(Long roleId) {
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.eq(roleId));
        return roleMenuMapper.selectListByQuery(wrapper).stream()
                .map(SysRoleMenu::getMenuId).toList();
    }

    @Data
    public static class AddRoleRequest {
        @NotBlank(message = "角色标识不能为空")
        private String roleKey;
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        private String dataScope = "5";
        private List<Long> menuIds;
    }

    @Data
    public static class UpdateRoleRequest {
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        private String dataScope;
    }

    @Data
    public static class AssignMenusRequest {
        @NotEmpty(message = "菜单列表不能为空")
        private List<Long> menuIds;
    }
}
