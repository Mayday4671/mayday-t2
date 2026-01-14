package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;
import java.util.Date;

/**
 * 角色实体
 * <p>
 * 对应数据库表 {@code sys_role}，存储系统角色信息及其数据权限范围配置。
 * </p>
 *
 * <h3>核心字段说明:</h3>
 * <ul>
 *   <li><b>roleKey</b>: 角色权限字符串，如 "admin"、"editor"，用于代码中的权限判断</li>
 *   <li><b>dataScope</b>: 数据权限范围，决定此角色可以访问的数据范围</li>
 * </ul>
 *
 * <h3>dataScope 枚举值:</h3>
 * <table border="1">
 *   <tr><th>值</th><th>说明</th><th>SQL 条件生成规则</th></tr>
 *   <tr><td>1</td><td>全部数据权限</td><td>不添加任何过滤条件</td></tr>
 *   <tr><td>2</td><td>自定义数据权限</td><td>通过 sys_role_dept 表配置</td></tr>
 *   <tr><td>3</td><td>本部门数据权限</td><td>dept_id = 当前部门ID</td></tr>
 *   <tr><td>4</td><td>本部门及以下数据权限</td><td>使用 ancestors 查询子孙部门</td></tr>
 *   <tr><td>5</td><td>仅本人数据权限</td><td>user_id = 当前用户ID</td></tr>
 * </table>
 *
 * @author MayDay Auth Generator
 * @see SysUserRole
 * @see SysRoleDept
 * @see SysRoleMenu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID (主键，自增)
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 角色权限字符串
     * <p>如 "admin"、"editor"，用于代码中的权限判断</p>
     */
    private String roleKey;

    /**
     * 角色名称
     * <p>显示用，如 "系统管理员"、"内容编辑"</p>
     */
    private String roleName;

    /**
     * 数据范围
     * <ul>
     *   <li>1: 全部数据权限</li>
     *   <li>2: 自定义数据权限</li>
     *   <li>3: 本部门数据权限</li>
     *   <li>4: 本部门及以下数据权限</li>
     *   <li>5: 仅本人数据权限</li>
     * </ul>
     */
    private String dataScope;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
