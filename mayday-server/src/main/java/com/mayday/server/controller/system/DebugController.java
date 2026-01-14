package com.mayday.server.controller.system;

import com.mayday.auth.common.R;
import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysRoleMenu;
import com.mayday.auth.entity.SysUserDept;
import com.mayday.auth.entity.SysUserRole;
import com.mayday.auth.mapper.SysRoleMenuMapper;
import com.mayday.auth.mapper.SysUserDeptMapper;
import com.mayday.auth.mapper.SysUserRoleMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mayday.auth.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static com.mayday.auth.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static com.mayday.auth.entity.table.SysUserDeptTableDef.SYS_USER_DEPT;

/**
 * 调试用接口
 */
@RestController
@RequiredArgsConstructor
public class DebugController {

    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserDeptMapper userDeptMapper;

    /**
     * 测试密码匹配
     */
    @GetMapping("/debug/password")
    public R<Map<String, Object>> testPassword(
            @RequestParam("raw") String raw,
            @RequestParam("encoded") String encoded) {
        
        boolean matches = passwordEncoder.matches(raw, encoded);
        String newEncoded = passwordEncoder.encode(raw);
        
        return R.ok(Map.of(
            "rawPassword", raw,
            "encodedPassword", encoded,
            "matches", matches,
            "newEncodedSample", newEncoded
        ));
    }

    /**
     * 生成 BCrypt 密码
     */
    @GetMapping("/debug/encode")
    public R<String> encodePassword(@RequestParam("password") String password) {
        return R.ok(passwordEncoder.encode(password));
    }
    
    /**
     * 调试菜单权限
     * 查看当前用户的角色ID、菜单ID等信息
     */
    @GetMapping("/debug/menu")
    public R<Map<String, Object>> debugMenu() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Map<String, Object> result = new HashMap<>();
        
        result.put("userId", loginUser.getUserId());
        result.put("username", loginUser.getUsername());
        result.put("currentDeptId", loginUser.getCurrentDeptId());
        
        // 从 LoginUser 中获取的角色
        List<SysRole> roles = loginUser.getRoles();
        result.put("rolesFromLoginUser", roles != null ? roles.stream()
                .map(r -> Map.of("id", r.getId(), "roleKey", r.getRoleKey(), "roleName", r.getRoleName()))
                .collect(Collectors.toList()) : "null");
        
        // 从数据库中查询用户角色关联
        QueryWrapper userRoleWrapper = QueryWrapper.create()
                .where(SYS_USER_ROLE.USER_ID.eq(loginUser.getUserId()));
        List<SysUserRole> userRoles = userRoleMapper.selectListByQuery(userRoleWrapper);
        result.put("userRolesFromDB", userRoles.stream()
                .map(ur -> Map.of("roleId", ur.getRoleId(), "deptId", ur.getDeptId()))
                .collect(Collectors.toList()));
        
        // 从数据库中查询角色菜单关联
        if (roles != null && !roles.isEmpty()) {
            List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
            QueryWrapper roleMenuWrapper = QueryWrapper.create()
                    .where(SYS_ROLE_MENU.ROLE_ID.in(roleIds));
            List<SysRoleMenu> roleMenus = roleMenuMapper.selectListByQuery(roleMenuWrapper);
            result.put("menuIdsFromDB", roleMenus.stream()
                    .map(rm -> Map.of("roleId", rm.getRoleId(), "menuId", rm.getMenuId()))
                    .collect(Collectors.toList()));
        } else {
            result.put("menuIdsFromDB", "no roles");
        }
        
        return R.ok(result);
    }
    
    /**
     * 直接查询数据库中的用户角色 (无需登录)
     * 访问: /debug/userRole?userId=5
     */
    @GetMapping("/debug/userRole")
    public R<Map<String, Object>> debugUserRole(@RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        
        // 查询 sys_user_role 表
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_USER_ROLE.USER_ID.eq(userId));
        List<SysUserRole> userRoles = userRoleMapper.selectListByQuery(wrapper);
        result.put("userRoles", userRoles.stream()
                .map(ur -> Map.of("roleId", ur.getRoleId(), "deptId", ur.getDeptId() != null ? ur.getDeptId() : "NULL"))
                .collect(Collectors.toList()));
        
        return R.ok(result);
    }
    
    /**
     * 直接查询数据库中的用户部门 (无需登录)
     * 访问: /debug/userDept?userId=5
     */
    @GetMapping("/debug/userDept")
    public R<Map<String, Object>> debugUserDept(@RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        
        // 查询 sys_user_dept 表
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_USER_DEPT.USER_ID.eq(userId));
        List<SysUserDept> userDepts = userDeptMapper.selectListByQuery(wrapper);
        result.put("userDepts", userDepts.stream()
                .map(ud -> Map.of("deptId", ud.getDeptId(), "isDefault", ud.getIsDefault()))
                .collect(Collectors.toList()));
        
        return R.ok(result);
    }
}

