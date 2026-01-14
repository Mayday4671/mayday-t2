package com.mayday.auth.service;

import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysUser;
import com.mayday.auth.entity.SysUserDept;
import com.mayday.auth.mapper.SysUserDeptMapper;
import com.mayday.auth.mapper.SysUserMapper;
import com.mayday.auth.model.LoginUser;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.mayday.auth.entity.table.SysUserTableDef.SYS_USER;
import static com.mayday.auth.entity.table.SysUserDeptTableDef.SYS_USER_DEPT;

/**
 * 用户详情服务实现
 * <p>
 * 实现 Spring Security 的 {@link UserDetailsService} 接口，
 * 用于根据用户名加载用户信息进行认证。
 * </p>
 *
 * <h3>加载流程:</h3>
 * <ol>
 *   <li>根据用户名查询 sys_user 表</li>
 *   <li>查询用户的默认部门 (sys_user_dept.is_default='Y')</li>
 *   <li>加载用户在默认部门下的角色和权限</li>
 *   <li>构建 {@link LoginUser} 返回</li>
 * </ol>
 *
 * @author MayDay Auth Generator
 * @see UserDetailsService
 * @see LoginUser
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final PermissionService permissionService;

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return UserDetails 实例
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        QueryWrapper wrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.USERNAME.eq(username));

        SysUser user = userMapper.selectOneByQuery(wrapper);

        if (user == null) {
            log.warn("登录用户 {} 不存在", username);
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        if ("1".equals(user.getStatus())) {
            log.warn("登录用户 {} 已被禁用", username);
            throw new UsernameNotFoundException("账号已被禁用");
        }

        // 2. 查询用户的默认部门
        Long defaultDeptId = getDefaultDeptId(user.getId());

        // 3. 加载用户在默认部门下的角色
        List<SysRole> roles = permissionService.getRolesByUserAndDept(user.getId(), defaultDeptId);

        // 4. 加载权限
        Set<String> permissions = permissionService.getPermissionsByRoles(roles, user.getId());

        log.info("--- 登录权限审计 [START] ---");
        log.info("用户账号: {}, 用户ID: {}", username, user.getId());
        log.info("关联角色数: {}", roles.size());
        roles.forEach(r -> log.info("  -> 角色: {} [Key: {}, ID: {}]", r.getRoleName(), r.getRoleKey(), r.getId()));
        log.info("最终计算权限标识数: {}", permissions.size());
        log.info("--- 登录权限审计 [END] ---");

        log.debug("用户 {} 登录成功，默认部门: {}, 角色数: {}, 权限数: {}", 
                username, defaultDeptId, roles.size(), permissions.size());

        // 5. 获取用户关联的所有部门 ID (用于数据权限自动合并)
        List<Long> allDeptIds = getAllDeptIds(user.getId());

        // 6. 构建 LoginUser
        return new LoginUser(user, permissions, roles, defaultDeptId, allDeptIds);
    }

    /**
     * 获取用户的默认部门ID
     * <p>
     * 查询 sys_user_dept 表中 is_default='Y' 的记录。
     * 如果没有默认部门，返回第一个关联的部门。
     * </p>
     *
     * @param userId 用户ID
     * @return 默认部门ID，如果没有则返回 null
     */
    public Long getDefaultDeptId(Long userId) {
        // 优先查找默认部门
        QueryWrapper wrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId))
                .and(SYS_USER_DEPT.IS_DEFAULT.eq("Y"));

        SysUserDept defaultDept = userDeptMapper.selectOneByQuery(wrapper);
        if (defaultDept != null) {
            return defaultDept.getDeptId();
        }

        // 没有默认部门，取第一个
        wrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId))
                .limit(1);

        SysUserDept firstDept = userDeptMapper.selectOneByQuery(wrapper);
        return firstDept != null ? firstDept.getDeptId() : null;
    }

    /**
     * 根据用户ID和指定部门ID加载 LoginUser
     * <p>
     * 用于部门切换场景，加载用户在新部门下的角色和权限。
     * </p>
     *
     * @param userId 用户ID
     * @param deptId 目标部门ID
     * @return LoginUser 实例
     */
    public LoginUser loadUserByIdAndDept(Long userId, Long deptId) {
        SysUser user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<SysRole> roles = permissionService.getRolesByUserAndDept(userId, deptId);
        Set<String> permissions = permissionService.getPermissionsByRoles(roles, userId);

        // 获取用户关联的所有部门 ID
        List<Long> allDeptIds = getAllDeptIds(userId);

        return new LoginUser(user, permissions, roles, deptId, allDeptIds);
    }

    /**
     * 获取用户关联的所有部门 ID
     */
    private List<Long> getAllDeptIds(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select(SYS_USER_DEPT.DEPT_ID)
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId));
        return userDeptMapper.selectListByQuery(wrapper).stream()
                .map(SysUserDept::getDeptId)
                .toList();
    }
}
