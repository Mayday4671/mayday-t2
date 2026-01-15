package com.mayday.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mayday.auth.entity.SysRole;
import com.mayday.auth.entity.SysUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户身份权限载体
 * <p>
 * 实现 Spring Security 的 {@link UserDetails} 接口，用于封装当前登录用户的身份信息、
 * 权限列表和角色信息。此类是 Spring Security 认证和授权的核心数据模型。
 * </p>
 *
 * <h3>核心特性:</h3>
 * <ul>
 *   <li><b>多部门支持</b>: 通过 {@code currentDeptId} 字段记录用户当前登录的部门上下文，
 *       支持用户在多个部门之间切换身份。</li>
 *   <li><b>部门级角色</b>: {@code roles} 字段存储的是用户在<b>当前部门</b>下拥有的角色列表，
 *       而非用户的所有角色。这确保了数据权限过滤的准确性。</li>
 *   <li><b>权限标识</b>: {@code permissions} 字段存储用户的权限标识字符串集合 (如 "system:user:list")，
 *       用于方法级别的权限校验。</li>
 * </ul>
 *
 * <h3>JWT Token 中的存储:</h3>
 * <p>
 * 登录成功后，通常会将 {@code userId}, {@code currentDeptId} 等关键信息存入 JWT Token，
 * 后续请求通过 Token 还原用户上下文。
 * </p>
 *
 * <h3>Jackson 序列化注意事项:</h3>
 * <p>
 * 由于此类实现了 {@link UserDetails} 接口，存在多个动态计算的 getter 方法（如 getAuthorities()、
 * isEnabled() 等），这些方法在 Redis 序列化时会被当作属性处理，但反序列化时因缺少 setter 而失败。
 * 因此，所有从 SysUser 动态获取的属性或 UserDetails 接口的布尔方法均使用 {@code @JsonIgnore} 标注。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see com.mayday.auth.util.SecurityUtils
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     * <p>来自 sys_user 表的主键</p>
     */
    private Long userId;

    /**
     * 用户名
     * <p>独立存储，避免 Jackson 序列化问题</p>
     */
    /**
     * 用户名
     * <p>独立存储，避免 Jackson 序列化问题</p>
     */
    private String username;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 当前登录部门ID
     * <p>
     * 用户登录时选择的部门，或系统默认的主部门。
     * 此字段是多部门兼职功能的核心，决定了用户当前的角色和数据权限范围。
     * 用户可以在前端切换部门，切换后需要刷新 Token 并更新此值。
     * </p>
     */
    private Long currentDeptId;

    /**
     * 用户唯一标识 (JWT Token 的 jti 或 uuid)
     * <p>可用于 Token 刷新、主动失效等场景</p>
     */
    private String token;

    /**
     * 登录时间 (毫秒时间戳)
     */
    private Long loginTime;

    /**
     * Token 过期时间 (毫秒时间戳)
     */
    private Long expireTime;

    /**
     * 权限标识列表
     * <p>
     * 存储用户在当前部门下拥有的所有权限标识字符串，如 "system:user:list", "system:role:add"。
     * 用于 {@code @PreAuthorize} 等方法级权限校验。
     * </p>
     */
    private Set<String> permissions;

    /**
     * 角色列表 (当前部门下)
     * <p>
     * 存储用户在<b>当前登录部门 ({@code currentDeptId})</b>下拥有的角色对象列表。
     * 这些角色的 {@code dataScope} 属性将被 {@link com.mayday.auth.aspect.DataScopeAspect}
     * 用于生成数据权限 SQL 条件。
     * </p>
     * <p>
     * <b>注意</b>: 这不是用户所有部门的角色，仅仅是当前部门的角色。
     * 当用户切换部门时，此列表应被更新。
     * </p>
     */
    private List<SysRole> roles;

    /**
     * 用户信息对象
     * <p>封装 sys_user 表的完整信息</p>
     */
    private SysUser sysUser;

    /**
     * 用户所属的所有部门ID
     * <p>
     * 包含主部门和所有兼职部门。用于数据权限自动合并，无需切换部门即可查看所有关联数据。
     * </p>
     */
    private List<Long> allDeptIds;

    /**
     * 构造函数
     *
     * @param sysUser       用户信息对象
     * @param permissions   权限标识集合
     * @param roles         当前部门下的角色列表
     * @param currentDeptId 当前登录部门ID
     * @param allDeptIds    用户所属的所有部门ID
     */
    public LoginUser(SysUser sysUser, Set<String> permissions, List<SysRole> roles, Long currentDeptId, List<Long> allDeptIds) {
        this.sysUser = sysUser;
        this.userId = sysUser.getId();
        this.username = sysUser.getUsername();
        this.avatar = sysUser.getAvatar();
        this.permissions = permissions;
        this.roles = roles;
        this.currentDeptId = currentDeptId;
        this.allDeptIds = allDeptIds;
    }

    /**
     * 获取用户密码 (用于 Spring Security 认证)
     * <p>
     * <b>注意</b>: 使用 @JsonIgnore 忽略序列化，密码不应存储到 Redis
     * </p>
     *
     * @return 加密后的密码
     */
    @JsonIgnore
    @Override
    public String getPassword() {
        return sysUser != null ? sysUser.getPassword() : null;
    }

    /**
     * 获取用户名 (用于 Spring Security 认证)
     * <p>
     * 注意：username 已作为独立字段存储，此方法直接返回该字段值，
     * 不再从 sysUser 动态获取，避免序列化问题。
     * </p>
     *
     * @return 用户登录账号
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * 账户是否未过期
     *
     * @return true 表示未过期
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未被锁定
     *
     * @return true 表示未锁定
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭据(密码)是否未过期
     *
     * @return true 表示凭据有效
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否启用
     * <p>实际业务中可根据 {@code sysUser.getStatus()} 判断</p>
     *
     * @return true 表示账户启用
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 获取授权列表 (用于 Spring Security 授权)
     * <p>将权限标识字符串转换为 {@link SimpleGrantedAuthority} 集合</p>
     * <p>
     * <b>注意</b>: 此方法使用 {@code @JsonIgnore} 注解，因为 authorities 是从 permissions 动态计算的，
     * 不需要在 Redis 中单独存储，同时避免 Jackson 反序列化时的 "setterless property" 问题。
     * </p>
     *
     * @return 授权对象集合
     */
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) {
            return null;
        }
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
