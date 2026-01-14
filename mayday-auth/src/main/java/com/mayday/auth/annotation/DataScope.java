package com.mayday.auth.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 * <p>
 * 用于标记需要进行数据范围过滤的 Service 层方法。
 * 当方法被此注解标记时，{@link com.mayday.auth.aspect.DataScopeAspect} 切面
 * 会在方法执行前根据当前登录用户的角色和数据权限范围，动态生成 SQL 条件，
 * 并将其注入到方法的第一个 {@link com.mayday.auth.entity.BaseEntity} 类型的参数中。
 * </p>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * @DataScope(deptAlias = "d", userAlias = "u")
 * public List<SysUser> selectUserList(SysUser user) {
 *     // user.getParams().get("dataScope") 将包含动态 SQL 条件
 *     return userMapper.selectUserList(user);
 * }
 * }</pre>
 *
 * <h3>数据权限范围 (data_scope) 枚举值说明:</h3>
 * <ul>
 *   <li>1: 全部数据权限 - 不进行任何过滤</li>
 *   <li>2: 自定义数据权限 - 根据 sys_role_dept 表配置过滤</li>
 *   <li>3: 本部门数据权限 - 只能查看当前登录部门的数据</li>
 *   <li>4: 本部门及以下数据权限 - 可查看当前部门及其所有子部门的数据</li>
 *   <li>5: 仅本人数据权限 - 只能查看自己创建的数据</li>
 * </ul>
 *
 * @author MayDay Auth Generator
 * @see com.mayday.auth.aspect.DataScopeAspect
 * @see com.mayday.auth.entity.BaseEntity
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名
     * <p>
     * 在生成 SQL 条件时，用于指定部门表的别名。
     * 例如：当 SQL 为 {@code SELECT * FROM sys_user u JOIN sys_dept d ON ...}
     * 时，应设置 {@code deptAlias = "d"}。
     * </p>
     *
     * @return 部门表别名，默认为 "d"
     */
    String deptAlias() default "d";

    /**
     * 用户表的别名
     * <p>
     * 在生成 SQL 条件时，用于指定用户表的别名。
     * 当数据权限为"仅本人"时，会使用此别名生成 {@code {userAlias}.user_id = xxx} 的条件。
     * </p>
     *
     * @return 用户表别名，默认为 "u"
     */
    String userAlias() default "u";

    /**
     * 是否忽略管理员
     * <p>
     * 当设置为 {@code true} 时，如果当前登录用户是超级管理员，则跳过数据权限过滤，
     * 超级管理员可以查看所有数据。
     * 当设置为 {@code false} 时，即使是管理员也会进行数据权限过滤。
     * </p>
     *
     * @return 是否忽略管理员，默认为 true
     */
    boolean ignoreAdmin() default true;
}
