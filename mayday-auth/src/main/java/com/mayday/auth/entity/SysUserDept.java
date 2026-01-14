package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;

/**
 * 用户-部门关联实体
 * <p>
 * 对应数据库表 {@code sys_user_dept}，实现用户与部门的多对多关联。
 * <b>这是支持用户跨部门兼职的核心表</b>。
 * </p>
 *
 * <h3>设计背景:</h3>
 * <p>
 * 传统设计中用户表直接包含 dept_id 字段，一个用户只能属于一个部门。
 * 本设计通过独立的关联表，支持一个用户同时属于多个部门（兼职场景）。
 * </p>
 *
 * <h3>字段说明:</h3>
 * <ul>
 *   <li><b>userId</b>: 用户ID (联合主键)</li>
 *   <li><b>deptId</b>: 部门ID (联合主键)</li>
 *   <li><b>isDefault</b>: 是否为用户的默认部门。登录时若未指定部门，默认使用此部门</li>
 * </ul>
 *
 * <h3>使用场景:</h3>
 * <ol>
 *   <li>用户登录时，根据 isDefault='Y' 确定初始的 currentDeptId</li>
 *   <li>用户可以在前端切换当前部门上下文</li>
 *   <li>查询用户所属的所有部门列表</li>
 * </ol>
 *
 * @author MayDay Auth Generator
 * @see SysUser
 * @see SysDept
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_dept")
public class SysUserDept extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long userId;

    /**
     * 部门ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long deptId;

    /**
     * 是否默认部门
     * <ul>
     *   <li>Y: 是默认部门</li>
     *   <li>N: 非默认部门</li>
     * </ul>
     * <p>每个用户应有且只有一个默认部门</p>
     */
    private String isDefault;
}
