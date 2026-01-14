package com.mayday.auth.aspect;

import cn.hutool.core.util.StrUtil;
import com.mayday.auth.annotation.DataScope;
import com.mayday.auth.entity.BaseEntity;
import com.mayday.auth.entity.SysRole;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限过滤切面
 * <p>
 * 本类是数据权限模块的核心实现，通过 AOP 技术在目标方法执行前动态注入 SQL 过滤条件，
 * 实现行级数据权限控制。
 * </p>
 *
 * <h3>工作原理:</h3>
 * <ol>
 *   <li>拦截所有标记了 {@link DataScope} 注解的方法</li>
 *   <li>从 Spring Security 上下文获取当前登录用户 {@link LoginUser}</li>
 *   <li>如果用户是超级管理员且注解配置 {@code ignoreAdmin=true}，则跳过过滤</li>
 *   <li>遍历用户在当前部门下的角色列表，根据每个角色的 {@code dataScope} 生成 SQL 片段</li>
 *   <li>将生成的 SQL 条件注入到方法第一个 {@link BaseEntity} 参数的 {@code params} 中</li>
 * </ol>
 *
 * <h3>SQL 生成规则:</h3>
 * <table border="1">
 *   <tr><th>data_scope 值</th><th>说明</th><th>生成的 SQL 片段</th></tr>
 *   <tr><td>1</td><td>全部数据</td><td>不生成任何条件</td></tr>
 *   <tr><td>2</td><td>自定义</td><td>{@code d.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ?)}</td></tr>
 *   <tr><td>3</td><td>本部门</td><td>{@code d.dept_id = ?}</td></tr>
 *   <tr><td>4</td><td>本部门及以下</td><td>{@code d.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = ? OR FIND_IN_SET(?, ancestors))}</td></tr>
 *   <tr><td>5</td><td>仅本人</td><td>{@code u.user_id = ?}</td></tr>
 * </table>
 *
 * <h3>Mapper XML 中的使用:</h3>
 * <pre>{@code
 * <select id="selectUserList" resultType="SysUser">
 *     SELECT u.*, d.dept_name
 *     FROM sys_user u
 *     LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
 *     WHERE u.status = '0'
 *     ${params.dataScope}
 * </select>
 * }</pre>
 *
 * <h3>关键设计说明:</h3>
 * <ul>
 *   <li><b>currentDeptId</b>: 从 Token 中解析的当前部门上下文，用于"本部门"相关的数据权限过滤</li>
 *   <li><b>roles</b>: 用户在当前部门下的角色列表（非全部角色），确保跨部门兼职时数据隔离</li>
 *   <li><b>OR 连接</b>: 多个角色的权限条件用 OR 连接，取并集</li>
 * </ul>
 *
 * @author MayDay Auth Generator
 * @see DataScope
 * @see BaseEntity
 * @see LoginUser
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class DataScopeAspect {

    /**
     * 全部数据权限 - 可查看所有数据，不做任何过滤
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定义数据权限 - 通过 sys_role_dept 表配置可查看的部门列表
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 本部门数据权限 - 只能查看当前登录部门的数据
     */
    public static final String DATA_SCOPE_DEPT = "3";

    /**
     * 本部门及以下数据权限 - 可查看当前部门及其所有子部门的数据
     */
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

    /**
     * 仅本人数据权限 - 只能查看自己创建/负责的数据
     */
    public static final String DATA_SCOPE_SELF = "5";

    /**
     * 数据权限过滤关键字 - 用于在 params 中存储生成的 SQL 条件
     */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 切面前置通知
     * <p>
     * 在目标方法执行前进行数据权限处理，先清空原有的 dataScope 参数，
     * 然后根据当前用户的角色生成新的 SQL 条件。
     * </p>
     *
     * @param point               切入点
     * @param controllerDataScope DataScope 注解实例
     */
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) {
        clearDataScope(point);
        handleDataScope(point, controllerDataScope);
    }

    /**
     * 处理数据权限逻辑
     *
     * @param joinPoint           切入点
     * @param controllerDataScope DataScope 注解实例
     */
    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope) {
        // 获取当前用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            log.warn("数据权限过滤：无法获取当前登录用户，跳过过滤");
            return;
        }

        // 如果是超管且配置忽略，则直接返回
        if (controllerDataScope.ignoreAdmin() && SecurityUtils.isAdmin(loginUser.getUserId())) {
            log.debug("数据权限过滤：当前用户为管理员且配置忽略，跳过过滤");
            return;
        }

        // 获取用户当前部门下的角色
        List<SysRole> roles = loginUser.getRoles();
        Long currentDeptId = loginUser.getCurrentDeptId();
        Long userId = loginUser.getUserId();

        log.debug("数据权限过滤：userId={}, currentDeptId={}, roles.size={}", 
                userId, currentDeptId, roles != null ? roles.size() : 0);

        dataScopeFilter(joinPoint, roles, currentDeptId, userId, 
                controllerDataScope.deptAlias(), controllerDataScope.userAlias());
    }

    /**
     * 根据角色数据权限范围生成并注入 SQL 条件
     * <p>
     * 遍历用户的角色列表，根据每个角色的 {@code dataScope} 属性生成对应的 SQL 片段，
     * 多个角色的条件用 OR 连接，最终用 AND (...) 包裹后注入到 BaseEntity 的 params 中。
     * </p>
     *
     * @param joinPoint     切入点
     * @param roles         用户角色列表（已按当前部门过滤）
     * @param currentDeptId 当前部门ID (来自 Token)
     * @param userId        当前用户ID
     * @param deptAlias     部门表别名（来自注解配置）
     * @param userAlias     用户表别名（来自注解配置）
     */
    public static void dataScopeFilter(JoinPoint joinPoint, List<SysRole> roles, 
            Long currentDeptId, Long userId, String deptAlias, String userAlias) {
        
        StringBuilder sqlBuilder = new StringBuilder();
        boolean conditions = false; // 标记是否已添加任何条件

        if (roles == null || roles.isEmpty()) {
            // 无角色则默认仅本人数据权限，确保数据安全
            log.debug("用户无角色，默认使用仅本人数据权限");
            sqlBuilder.append(StrUtil.format(" {}.user_id = {} ", userAlias, userId));
            conditions = true;
        } else {
            for (SysRole role : roles) {
                String dataScope = role.getDataScope();
                
                if (DATA_SCOPE_ALL.equals(dataScope)) {
                    // 全部数据权限：清空所有条件，不做过滤
                    log.debug("角色 {} 拥有全部数据权限，跳过过滤", role.getRoleKey());
                    sqlBuilder = new StringBuilder();
                    conditions = false;
                    break;
                } else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
                    // 自定义数据权限：通过 sys_role_dept 表查询
                    sqlBuilder.append(StrUtil.format(
                            " {} {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ",
                            conditions ? "OR" : "", deptAlias, role.getId()));
                    conditions = true;
                } else if (DATA_SCOPE_DEPT.equals(dataScope)) {
                    // 本部门数据权限：使用 currentDeptId
                    sqlBuilder.append(StrUtil.format(
                            " {} {}.dept_id = {} ",
                            conditions ? "OR" : "", deptAlias, currentDeptId));
                    conditions = true;
                } else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                    // 本部门及以下数据权限：使用 ancestors 字段实现层级查询
                    sqlBuilder.append(StrUtil.format(
                            " {} {}.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} OR FIND_IN_SET( {} , ancestors ) ) ",
                            conditions ? "OR" : "", deptAlias, currentDeptId, currentDeptId));
                    conditions = true;
                } else if (DATA_SCOPE_SELF.equals(dataScope)) {
                    // 仅本人数据权限
                    sqlBuilder.append(StrUtil.format(
                            " {} {}.user_id = {} ",
                            conditions ? "OR" : "", userAlias, userId));
                    conditions = true;
                }
            }
        }

        // 将生成的 SQL 条件注入到第一个 BaseEntity 参数中
        if (conditions) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof BaseEntity baseEntity) {
                        // 去除开头的空格，用 AND (...) 包裹所有 OR 条件
                        String sql = sqlBuilder.toString().trim();
                        baseEntity.getParams().put(DATA_SCOPE, " AND (" + sql + ")");
                        log.debug("数据权限 SQL 已注入: AND ({})", sql);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 清空数据权限参数
     * <p>
     * 在每次处理前清空原有的 dataScope 参数，避免参数污染。
     * </p>
     *
     * @param joinPoint 切入点
     */
    private void clearDataScope(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof BaseEntity baseEntity) {
                    baseEntity.getParams().put(DATA_SCOPE, "");
                    break;
                }
            }
        }
    }
}
