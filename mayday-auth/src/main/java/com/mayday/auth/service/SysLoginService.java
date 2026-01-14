package com.mayday.auth.service;

import com.mayday.auth.entity.SysDept;
import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysUser;
import com.mayday.auth.entity.SysUserDept;
import com.mayday.auth.mapper.SysDeptMapper;
import com.mayday.auth.mapper.SysUserDeptMapper;
import com.mayday.auth.mapper.SysUserMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.model.vo.LoginVo;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mayday.auth.entity.table.SysUserTableDef.SYS_USER;
import static com.mayday.auth.entity.table.SysUserDeptTableDef.SYS_USER_DEPT;

/**
 * 登录服务
 * <p>
 * 实现多部门用户的两步登录流程：
 * <ol>
 *   <li><b>第一步 (login)</b>: 验证账号密码
 *       <ul>
 *         <li>单部门用户：直接生成最终 Token</li>
 *         <li>多部门用户：返回临时 Token + 部门列表，等待选择</li>
 *       </ul>
 *   </li>
 *   <li><b>第二步 (selectDept)</b>: 多部门用户选择部门后，生成最终 Token</li>
 * </ol>
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginService {

    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PermissionService permissionService;

    /**
     * 用户登录 (第一步)
     * <p>
     * 验证账号密码后，根据用户所属部门数量决定返回内容：
     * <ul>
     *   <li><b>单部门</b>: 直接生成最终 Token，返回 needSelectDept=false</li>
     *   <li><b>多部门</b>: 生成临时 Token，返回 needSelectDept=true + 部门列表</li>
     * </ul>
     * </p>
     *
     * @param username 用户名
     * @param password 密码 (明文)
     * @return 登录响应
     */
    public LoginVo login(String username, String password) {
        // 1. 查询用户
        SysUser user = getUserByUsername(username);
        if (user == null) {
            log.warn("登录失败：用户 {} 不存在", username);
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败：用户 {} 密码错误", username);
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 3. 检查账户状态
        if ("1".equals(user.getStatus())) {
            log.warn("登录失败：用户 {} 已被禁用", username);
            throw new BadCredentialsException("账号已被禁用");
        }

        // 4. 查询用户所属部门列表
        List<SysUserDept> userDepts = getUserDepts(user.getId());
        
        LoginVo vo = new LoginVo();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNeedSelectDept(false);  // 不再需要选择部门

        if (userDepts.isEmpty()) {
            // 无部门用户，直接登录 (使用 null 作为 deptId)
            log.warn("用户 {} 没有关联任何部门，将以无部门身份登录", username);
            LoginUser loginUser = buildLoginUser(user, null);
            String token = tokenService.createToken(loginUser);
            
            vo.setToken(token);
            vo.setCurrentDeptId(null);
            vo.setPermissions(new ArrayList<>(loginUser.getPermissions()));
        } else {
            // 有部门用户：优先使用默认部门，没有默认部门则使用第一个部门
            Long defaultDeptId = userDepts.stream()
                    .filter(ud -> "Y".equals(ud.getIsDefault()))
                    .map(SysUserDept::getDeptId)
                    .findFirst()
                    .orElse(userDepts.get(0).getDeptId());
            
            LoginUser loginUser = buildLoginUser(user, defaultDeptId);
            String token = tokenService.createToken(loginUser);
            
            vo.setToken(token);
            vo.setCurrentDeptId(defaultDeptId);
            vo.setPermissions(new ArrayList<>(loginUser.getPermissions()));
            
            // 如果有多个部门，返回部门列表供页面切换使用
            if (userDepts.size() > 1) {
                vo.setDeptList(buildDeptOptions(userDepts));
            }
            
            log.info("用户 {} 登录成功 (默认部门: {}, 共有 {} 个部门)", 
                    username, defaultDeptId, userDepts.size());
        }

        return vo;
    }

    /**
     * 选择部门 (第二步)
     * <p>
     * 多部门用户登录后选择部门，验证临时 Token 和部门归属，生成最终 Token。
     * </p>
     *
     * @param tempToken 临时 Token
     * @param deptId    选择的部门ID
     * @return 登录响应 (包含最终 Token)
     */
    public LoginVo selectDept(String tempToken, Long deptId) {
        // 1. 验证临时 Token
        Long userId = tokenService.validateTempToken(tempToken);
        if (userId == null) {
            throw new BadCredentialsException("临时Token已过期，请重新登录");
        }

        // 2. 验证用户是否属于该部门
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId))
                .and(SYS_USER_DEPT.DEPT_ID.eq(deptId));

        SysUserDept userDept = userDeptMapper.selectOneByQuery(wrapper);
        if (userDept == null) {
            throw new BadCredentialsException("您不属于该部门");
        }

        // 3. 删除临时 Token
        tokenService.deleteTempToken(tempToken);

        // 4. 生成最终 Token
        SysUser user = userMapper.selectOneById(userId);
        LoginUser loginUser = buildLoginUser(user, deptId);
        String token = tokenService.createToken(loginUser);

        // 5. 构建响应
        LoginVo vo = new LoginVo();
        vo.setNeedSelectDept(false);
        vo.setToken(token);
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());
        vo.setCurrentDeptId(deptId);
        vo.setPermissions(new ArrayList<>(loginUser.getPermissions()));

        log.info("用户 {} 选择部门 {} 登录成功", user.getUsername(), deptId);
        return vo;
    }

    /**
     * 切换部门
     * <p>
     * 已登录用户切换到另一个部门，重新生成 Token。
     * </p>
     *
     * @param loginUser     当前登录用户
     * @param targetDeptId  目标部门ID
     * @return 新的登录响应
     */
    public LoginVo switchDept(LoginUser loginUser, Long targetDeptId) {
        Long userId = loginUser.getUserId();

        // 1. 验证用户是否属于目标部门
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId))
                .and(SYS_USER_DEPT.DEPT_ID.eq(targetDeptId));

        SysUserDept userDept = userDeptMapper.selectOneByQuery(wrapper);
        if (userDept == null) {
            throw new RuntimeException("您不属于该部门，无法切换");
        }

        // 2. 删除旧 Token
        tokenService.deleteToken(loginUser.getToken());

        // 3. 重新生成 Token (包含新的 currentDeptId)
        SysUser user = userMapper.selectOneById(userId);
        LoginUser newLoginUser = buildLoginUser(user, targetDeptId);
        String newToken = tokenService.createToken(newLoginUser);

        // 4. 构建响应
        LoginVo vo = new LoginVo();
        vo.setNeedSelectDept(false);
        vo.setToken(newToken);
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());
        vo.setCurrentDeptId(targetDeptId);
        vo.setPermissions(new ArrayList<>(newLoginUser.getPermissions()));

        log.info("用户 {} 切换部门: {} -> {}", user.getUsername(), loginUser.getCurrentDeptId(), targetDeptId);
        return vo;
    }

    /**
     * 根据用户名查询用户
     */
    private SysUser getUserByUsername(String username) {
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER)
                .where(SYS_USER.USERNAME.eq(username));
        return userMapper.selectOneByQuery(wrapper);
    }

    /**
     * 获取用户所属的所有部门
     */
    private List<SysUserDept> getUserDepts(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId));
        return userDeptMapper.selectListByQuery(wrapper);
    }

    /**
     * 构建部门选项列表
     */
    private List<LoginVo.DeptOption> buildDeptOptions(List<SysUserDept> userDepts) {
        List<Long> deptIds = userDepts.stream()
                .map(SysUserDept::getDeptId)
                .collect(Collectors.toList());

        List<SysDept> depts = deptMapper.selectListByIds(deptIds);

        return userDepts.stream().map(ud -> {
            LoginVo.DeptOption option = new LoginVo.DeptOption();
            option.setDeptId(ud.getDeptId());
            option.setIsDefault("Y".equals(ud.getIsDefault()));
            
            // 查找部门名称
            depts.stream()
                    .filter(d -> d.getId().equals(ud.getDeptId()))
                    .findFirst()
                    .ifPresent(d -> option.setDeptName(d.getDeptName()));
            
            return option;
        }).collect(Collectors.toList());
    }

    /**
     * 构建 LoginUser 对象
     * <p>
     * <b>关键逻辑</b>:
     * 1. 查询用户在指定部门下的角色 (用于功能权限控制)
     * 2. 查询用户所属的所有部门 (用于多部门数据权限合并)
     * </p>
     */
    private LoginUser buildLoginUser(SysUser user, Long deptId) {
        // 1. 查询用户在指定部门下的角色 (包含全局角色)
        List<SysRole> roles = permissionService.getRolesByUserAndDept(user.getId(), deptId);
        
        // 2. 查询权限标识
        Set<String> permissions = permissionService.getPermissionsByRoles(roles);

        // 3. 获取用户所属的所有部门 ID (用于数据权限自动合并)
        List<Long> allDeptIds = getUserDepts(user.getId()).stream()
                .map(SysUserDept::getDeptId)
                .collect(Collectors.toList());

        return new LoginUser(user, permissions, roles, deptId, allDeptIds);
    }
}
