package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;

/**
 * 角色-部门关联实体
 * <p>
 * 对应数据库表 {@code sys_role_dept}，用于"自定义数据权限"场景。
 * 当角色的 dataScope=2 (自定义) 时，通过本表配置该角色可以访问哪些部门的数据。
 * </p>
 *
 * <h3>使用场景:</h3>
 * <ol>
 *   <li>创建角色时，设置 dataScope=2 (自定义数据权限)</li>
 *   <li>在本表中配置该角色可以访问的部门列表</li>
 *   <li>数据权限 AOP 在生成 SQL 时，会查询本表获取部门列表</li>
 * </ol>
 *
 * <h3>SQL 生成示例:</h3>
 * <pre>{@code
 * -- 当 dataScope=2 时生成的条件
 * AND d.dept_id IN (
 *     SELECT dept_id FROM sys_role_dept WHERE role_id = {角色ID}
 * )
 * }</pre>
 *
 * @author MayDay Auth Generator
 * @see SysRole
 * @see SysDept
 * @see com.mayday.auth.aspect.DataScopeAspect
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_dept")
public class SysRoleDept extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long roleId;

    /**
     * 部门ID (联合主键)
     * <p>该角色可以访问的部门</p>
     */
    @Id(keyType = KeyType.None)
    private Long deptId;
}
