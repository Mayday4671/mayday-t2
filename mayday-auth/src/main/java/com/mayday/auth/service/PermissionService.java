package com.mayday.auth.service;

import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysUserRole;
import com.mayday.auth.mapper.SysMenuMapper;
import com.mayday.auth.mapper.SysRoleMapper;
import com.mayday.auth.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mayday.auth.entity.table.SysMenuTableDef.SYS_MENU;
import static com.mayday.auth.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static com.mayday.auth.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

/**
 * 权限服务
 * <p>
 * 提供用户角色和权限的查询功能，支持多部门上下文。
 * </p>
 *
 * <h3>关键设计：部门级角色查询</h3>
 * <p>
 * 查询角色时带上 dept_id 条件，SQL 语义为：
 * {@code SELECT * FROM sys_user_role WHERE user_id = ? AND (dept_id = ? OR dept_id IS NULL)}
 * </p>
 * <p>
 * 其中 {@code dept_id IS NULL} 用于支持"全局角色"场景（不限定部门的角色）。
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 获取用户在指定部门下的角色列表
     * <p>
     * <b>关键逻辑</b>：查询条件为 {@code (dept_id = ? OR dept_id IS NULL)}，
     * 支持两种角色类型：
     * <ul>
     *   <li>部门级角色：dept_id 有值，仅在指定部门生效</li>
     *   <li>全局角色：dept_id 为 NULL，在所有部门都生效</li>
     * </ul>
     * </p>
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 用户在该部门下的角色列表（包含全局角色）
     */
    public List<SysRole> getRolesByUserAndDept(Long userId, Long deptId) {
        // 查询用户在指定部门下的角色ID列表 (包含全局角色)
        // SQL: SELECT role_id FROM sys_user_role WHERE user_id = ? AND (dept_id = ? OR dept_id IS NULL)
        QueryWrapper wrapper = QueryWrapper.create()
                .select(SYS_USER_ROLE.ROLE_ID)
                .from(SYS_USER_ROLE)
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_USER_ROLE.DEPT_ID.eq(deptId).or(SYS_USER_ROLE.DEPT_ID.isNull()));

        List<SysUserRole> userRoles = userRoleMapper.selectListByQuery(wrapper);
        
        if (userRoles == null || userRoles.isEmpty()) {
            log.debug("用户 {} 在部门 {} 下没有角色", userId, deptId);
            return List.of();
        }

        // 获取角色ID列表
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // 查询角色详情
        List<SysRole> roles = roleMapper.selectListByIds(roleIds);
        log.debug("用户 {} 在部门 {} 下的角色: {}", userId, deptId, 
                roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList()));
        
        return roles;
    }

    /**
     * 获取角色对应的权限标识集合
     *
     * @param roles 角色列表
     * @param userId 用户ID (用于判断超级管理员特权)
     * @return 权限标识集合
     */
    public Set<String> getPermissionsByRoles(List<SysRole> roles, Long userId) {
        if (roles == null || roles.isEmpty()) {
             log.debug("用户 [{}] 无任何角色，权限集为空", userId);
            return new HashSet<>();
        }

        // 1. 如果是超级管理员 (UserID=1)，直接赋予所有正常状态的权限标识
        // 注意：这里我们严格只认 UserID，不认角色 ID，确保其他用户的角色配置可以灵活调整
        if (userId != null && 1L == userId) {
            log.info("检测到超级管理员(ID=1)登录，开启上帝模式，透传全量权限标识");
            QueryWrapper wrapper = QueryWrapper.create()
                    .select(SYS_MENU.PERMS)
                    .from(SYS_MENU)
                    .where(SYS_MENU.STATUS.eq("0"))
                    .and(SYS_MENU.PERMS.isNotNull())
                    .and(SYS_MENU.PERMS.ne(""));
            return menuMapper.selectListByQuery(wrapper).stream()
                    .map(com.mayday.auth.entity.SysMenu::getPerms)
                    .collect(Collectors.toSet());
        }

        List<Long> roleIds = roles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());
        
        log.debug("开始为用户 [{}] 加载权限，涉及角色ID集合: {}", userId, roleIds);

        // 通过角色ID查询关联的菜单权限标识
        QueryWrapper wrapper = QueryWrapper.create()
                .select(SYS_MENU.PERMS)
                .from(SYS_MENU)
                .leftJoin(SYS_ROLE_MENU).on(SYS_MENU.ID.eq(SYS_ROLE_MENU.MENU_ID))
                .where(SYS_ROLE_MENU.ROLE_ID.in(roleIds))
                .and(SYS_MENU.STATUS.eq("0"))
                .and(SYS_MENU.PERMS.isNotNull())
                .and(SYS_MENU.PERMS.ne(""));

        List<com.mayday.auth.entity.SysMenu> menus = menuMapper.selectListByQuery(wrapper);
        Set<String> permsSet = menus.stream()
                .map(com.mayday.auth.entity.SysMenu::getPerms)
                .filter(perms -> perms != null && !perms.isEmpty())
                .collect(Collectors.toSet());
        
        log.info("权限加载完成。用户: {}, 角色数: {}, 最终提取权限数: {}", userId, roles.size(), permsSet.size());
        return permsSet;
    }

    // 保留旧方法兼容性 (不推荐使用)
    public Set<String> getPermissionsByRoles(List<SysRole> roles) {
        return getPermissionsByRoles(roles, null);
    }

    /**
     * 获取用户在指定部门下的所有权限标识
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 权限标识集合
     */
    public Set<String> getPermissionsByUserAndDept(Long userId, Long deptId) {
        List<SysRole> roles = getRolesByUserAndDept(userId, deptId);
        return getPermissionsByRoles(roles);
    }
}
