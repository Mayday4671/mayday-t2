package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;

/**
 * 用户-角色关联实体
 * <p>
 * 对应数据库表 {@code sys_user_role}，实现用户与角色的多对多关联。
 * <b>关键设计：包含 deptId 字段，表示该角色仅在指定部门上下文中生效</b>。
 * </p>
 *
 * <h3>设计背景:</h3>
 * <p>
 * 传统的用户-角色关联只有 userId 和 roleId，角色在所有场景下都生效。
 * 本设计增加了 deptId 字段，实现"部门级角色"，即同一个用户在不同部门可以拥有不同的角色。
 * </p>
 *
 * <h3>使用场景示例:</h3>
 * <ul>
 *   <li>张三在"技术部"是"部门经理"角色，拥有审批权限</li>
 *   <li>张三在"市场部"兼职，只是普通"员工"角色，无审批权限</li>
 *   <li>张三切换到"市场部"后，其角色和数据权限自动变更</li>
 * </ul>
 *
 * <h3>与 Token 的配合:</h3>
 * <p>
 * 登录或切换部门时，系统根据 currentDeptId 从本表查询用户在该部门下的角色列表，
 * 并存入 {@link com.mayday.auth.model.LoginUser#getRoles()}。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see SysUser
 * @see SysRole
 * @see com.mayday.auth.model.LoginUser
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_role")
public class SysUserRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long userId;

    /**
     * 角色ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long roleId;

    /**
     * 部门ID (联合主键)
     * <p>
     * 表示该角色仅在此部门上下文中生效。
     * 这是实现"跨部门兼职不同角色"的关键字段。
     * </p>
     */
    @Id(keyType = KeyType.None)
    private Long deptId;
}
