package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;
import java.util.Date;

/**
 * 部门实体
 * <p>
 * 对应数据库表 {@code sys_dept}，存储组织架构中的部门信息，支持树形结构。
 * </p>
 *
 * <h3>树形结构设计:</h3>
 * <ul>
 *   <li><b>parentId</b>: 父部门ID，顶级部门的 parentId 为 0</li>
 *   <li><b>ancestors</b>: 祖级列表，格式为 "0,1,2" 的逗号分隔字符串，
 *       用于快速查询所有子部门。例如部门 ID=5 的 ancestors 为 "0,1,3"，
 *       表示其祖级链路为 0 -> 1 -> 3 -> 5。</li>
 * </ul>
 *
 * <h3>数据权限中的使用:</h3>
 * <p>
 * 在"本部门及以下"数据权限范围中，使用 {@code FIND_IN_SET(currentDeptId, ancestors)}
 * 快速查询当前部门的所有子孙部门。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see SysUserDept
 * @see SysRoleDept
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class SysDept extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID (主键，自增)
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 父部门ID
     * <p>顶级部门的 parentId 为 0</p>
     */
    private Long parentId;

    /**
     * 祖级列表
     * <p>
     * 格式: "0,1,2,3" (逗号分隔的祖先部门ID)
     * 用于数据权限中快速查询子孙部门
     * </p>
     */
    private String ancestors;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
