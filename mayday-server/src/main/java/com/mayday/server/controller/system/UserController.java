package com.mayday.server.controller.system;

import com.mayday.auth.common.R;
import com.mayday.auth.entity.SysUser;
import com.mayday.auth.entity.SysUserDept;
import com.mayday.auth.entity.SysUserRole;
import com.mayday.auth.mapper.SysUserDeptMapper;
import com.mayday.auth.mapper.SysUserMapper;
import com.mayday.auth.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.mayday.auth.service.TokenService;
import com.mayday.server.service.UserQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mayday.auth.entity.table.SysUserTableDef.SYS_USER;
import static com.mayday.auth.entity.table.SysUserDeptTableDef.SYS_USER_DEPT;
import static com.mayday.auth.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

/**
 * 用户管理控制器
 *
 * @author MayDay Auth Generator
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserQueryService userQueryService;

    /**
     * 获取用户列表（带数据权限过滤）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<List<Map<String, Object>>> list() {
        // 使用 UserQueryService 获取带数据权限过滤的用户列表
        SysUser queryParam = new SysUser();
        List<Map<String, Object>> userList = userQueryService.selectUserList(queryParam);
        
        return R.ok(userList);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public R<Map<String, Object>> getInfo(@PathVariable("id") Long id) {
        SysUser user = userMapper.selectOneById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("status", user.getStatus());
        data.put("deptIds", getDeptIdsByUserId(id));
        data.put("roleIds", getRoleIdsByUserId(id));
        
        return R.ok(data);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Void> add(@Valid @RequestBody AddUserRequest request) {
        // 校验用户名唯一
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq(request.getUsername()));
        if (userMapper.selectCountByQuery(wrapper) > 0) {
            return R.fail("用户名已存在");
        }
        
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(request.getStatus());
        userMapper.insert(user);
        
        // 设置部门
        if (request.getDeptIds() != null) {
            boolean first = true;
            for (Long deptId : request.getDeptIds()) {
                SysUserDept ud = new SysUserDept();
                ud.setUserId(user.getId());
                ud.setDeptId(deptId);
                ud.setIsDefault(first ? "Y" : "N");
                userDeptMapper.insert(ud);
                first = false;
            }
        }
        
        // 设置角色
        if (request.getRoleIds() != null && request.getDeptIds() != null && !request.getDeptIds().isEmpty()) {
            Long defaultDeptId = request.getDeptIds().get(0);
            for (Long roleId : request.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                ur.setDeptId(defaultDeptId);
                userRoleMapper.insert(ur);
            }
        }
        
        log.info("新增用户: {}", request.getUsername());
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateUserRequest request) {
        SysUser user = userMapper.selectOneById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        
        // 更新基本信息
        user.setStatus(request.getStatus());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.update(user);
        
        // 更新部门
        if (request.getDeptIds() != null) {
            userDeptMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_DEPT.USER_ID.eq(id)));
            boolean first = true;
            for (Long deptId : request.getDeptIds()) {
                SysUserDept ud = new SysUserDept();
                ud.setUserId(id);
                ud.setDeptId(deptId);
                ud.setIsDefault(first ? "Y" : "N");
                userDeptMapper.insert(ud);
                first = false;
            }
        }
        
        log.info("更新用户: {}", user.getUsername());
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public R<Void> delete(@PathVariable("id") Long id) {
        SysUser user = userMapper.selectOneById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        
        // 删除用户关联
        userDeptMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_DEPT.USER_ID.eq(id)));
        userRoleMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_ROLE.USER_ID.eq(id)));
        userMapper.deleteById(id);
        
        log.info("删除用户: {}", user.getUsername());
        return R.ok();
    }

    /**
     * 分配角色
     * <p>
     * 为用户的所有部门都分配角色，确保在任何部门登录都能获取到角色
     * </p>
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRoles(@PathVariable("id") Long id, @RequestBody AssignRolesRequest request) {
        // 获取用户的所有部门
        List<Long> deptIds = getDeptIdsByUserId(id);
        
        // 清除旧角色
        userRoleMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_ROLE.USER_ID.eq(id)));
        
        // 为每个部门分配角色
        for (Long deptId : deptIds) {
            for (Long roleId : request.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                ur.setDeptId(deptId);
                userRoleMapper.insert(ur);
            }
        }
        
        // 如果用户没有部门，则使用 NULL 作为全局角色
        if (deptIds.isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                ur.setDeptId(null);  // 全局角色
                userRoleMapper.insert(ur);
            }
        }
        
        log.info("用户 {} 分配角色: {}, 部门: {}", id, request.getRoleIds(), deptIds);
        
        // 清除用户的所有旧Token，强制重新登录以获取新角色
        tokenService.deleteTokensByUserId(id);
        
        return R.ok();
    }

    private List<Long> getDeptIdsByUserId(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_USER_DEPT.USER_ID.eq(userId));
        return userDeptMapper.selectListByQuery(wrapper).stream()
                .map(SysUserDept::getDeptId).toList();
    }

    private List<Long> getRoleIdsByUserId(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_USER_ROLE.USER_ID.eq(userId));
        return userRoleMapper.selectListByQuery(wrapper).stream()
                .map(SysUserRole::getRoleId).toList();
    }

    @Data
    public static class AddUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String status = "0";
        private List<Long> deptIds;
        private List<Long> roleIds;
    }

    @Data
    public static class UpdateUserRequest {
        private String password;
        private String status;
        private List<Long> deptIds;
    }

    @Data
    public static class AssignRolesRequest {
        @NotEmpty(message = "角色列表不能为空")
        private List<Long> roleIds;
    }
}
