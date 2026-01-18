package com.mayday.auth.util;

import com.mayday.auth.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全服务工具类
 * <p>
 * 提供静态方法，用于从 Spring Security 上下文 ({@link SecurityContextHolder}) 中
 * 便捷地获取当前登录用户的信息。
 * </p>
 *
 * <h3>使用场景:</h3>
 * <ul>
 *   <li>在 Service 层获取当前操作用户的 ID</li>
 *   <li>在 Controller 层获取当前用户的部门上下文</li>
 *   <li>在 AOP 切面中判断用户是否为管理员</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 获取当前登录用户
 * LoginUser loginUser = SecurityUtils.getLoginUser();
 *
 * // 获取用户ID
 * Long userId = SecurityUtils.getUserId();
 *
 * // 获取当前部门ID
 * Long deptId = SecurityUtils.getDeptId();
 *
 * // 判断是否为管理员
 * if (SecurityUtils.isAdmin()) {
 *     // ...
 * }
 * }</pre>
 *
 * <h3>注意事项:</h3>
 * <p>
 * 此工具类的方法必须在经过 Spring Security 认证的请求上下文中调用。
 * 如果在未认证的上下文（如定时任务、消息队列消费者）中使用，将抛出 RuntimeException。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see LoginUser
 * @see SecurityContextHolder
 * @since 1.0.0
 */
@Slf4j
public class SecurityUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户对象 {@link LoginUser}
     * @throws RuntimeException 如果未能获取到用户信息（如未登录、Token 无效等）
     */
    public static LoginUser getLoginUser() {
        try {
            Authentication authentication = getAuthentication();
            if (authentication == null) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof LoginUser) {
                return (LoginUser) principal;
            }
            // Handle anonymous user (principal is String "anonymousUser")
            return null;
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            throw new RuntimeException("获取用户信息异常", e);
        }
    }

    /**
     * 获取 Authentication 对象
     *
     * @return Spring Security 认证对象
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取用户账号
     *
     * @return 用户登录账号 (username)
     * @throws RuntimeException 如果未能获取到用户信息
     */
    public static String getUsername() {
        try {
            LoginUser loginUser = getLoginUser();
            return loginUser != null ? loginUser.getUsername() : null;
        } catch (Exception e) {
            log.error("获取用户账户异常", e);
            throw new RuntimeException("获取用户账户异常", e);
        }
    }

    /**
     * 获取用户ID
     *
     * @return 当前登录用户的 ID (sys_user.id)
     * @throws RuntimeException 如果未能获取到用户信息
     */
    public static Long getUserId() {
        try {
            LoginUser loginUser = getLoginUser();
            return loginUser != null ? loginUser.getUserId() : null;
        } catch (Exception e) {
            log.error("获取用户ID异常", e);
            throw new RuntimeException("获取用户ID异常", e);
        }
    }

    /**
     * 获取当前部门ID
     * <p>
     * 返回用户当前登录上下文的部门ID。此值对应 {@link LoginUser#getCurrentDeptId()}，
     * 是多部门兼职功能的核心，用于数据权限过滤和业务逻辑处理。
     * </p>
     *
     * @return 当前登录部门 ID
     * @throws RuntimeException 如果未能获取到用户信息
     */
    public static Long getDeptId() {
        try {
            LoginUser loginUser = getLoginUser();
            return loginUser != null ? loginUser.getCurrentDeptId() : null;
        } catch (Exception e) {
            log.error("获取部门ID异常", e);
            throw new RuntimeException("获取部门ID异常", e);
        }
    }

    /**
     * 判断指定用户是否为管理员
     * <p>
     * 默认逻辑：用户 ID 为 1 的用户被视为超级管理员。
     * 实际项目中可根据业务需求修改此判断逻辑（如通过角色 key 判断）。
     * </p>
     *
     * @param userId 用户ID
     * @return true 表示是管理员，false 表示普通用户
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    /**
     * 判断当前登录用户是否为管理员
     *
     * @return true 表示当前用户是管理员
     * @see #isAdmin(Long)
     */
    public static boolean isAdmin() {
        return isAdmin(getUserId());
    }
}
