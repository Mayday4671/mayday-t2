package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;
import java.util.Date;

/**
 * 用户信息实体
 * <p>
 * 对应数据库表 {@code sys_user}，存储系统用户的基本信息。
 * </p>
 *
 * <h3>设计说明:</h3>
 * <ul>
 *   <li><b>无 deptId 字段</b>: 用户与部门的关联通过 {@link SysUserDept} 表实现，
 *       支持用户属于多个部门（兼职场景）。</li>
 *   <li><b>密码加密</b>: password 字段应使用 BCrypt 等算法加密存储。</li>
 *   <li><b>状态控制</b>: status 字段控制账户启用/禁用状态。</li>
 * </ul>
 *
 * @author MayDay Auth Generator
 * @see SysUserDept
 * @see SysUserRole
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键，自增)
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户登录账号 (唯一)
     */
    private String username;

    /**
     * 密码 (BCrypt 加密)
     */
    private String password;

    /**
     * 帐号状态
     * <ul>
     *   <li>0: 正常</li>
     *   <li>1: 停用</li>
     * </ul>
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
