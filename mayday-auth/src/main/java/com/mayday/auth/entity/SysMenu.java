package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;
import java.util.Date;

/**
 * 菜单权限实体
 * <p>
 * 对应数据库表 {@code sys_menu}，存储系统菜单和按钮级权限信息，支持树形结构。
 * </p>
 *
 * <h3>菜单类型 (menuType):</h3>
 * <ul>
 *   <li><b>M</b>: 目录 - 菜单分类容器</li>
 *   <li><b>C</b>: 菜单 - 可点击跳转的页面菜单</li>
 *   <li><b>F</b>: 按钮 - 页面内的功能按钮，用于细粒度权限控制</li>
 * </ul>
 *
 * <h3>权限标识 (perms):</h3>
 * <p>
 * 权限标识字符串，格式通常为 "模块:功能:操作"，如 "system:user:list"、"system:user:add"。
 * 在 Spring Security 中可通过 {@code @PreAuthorize("hasAuthority('system:user:list')")} 进行校验。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see SysRoleMenu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID (主键，自增)
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单ID
     * <p>顶级菜单的 parentId 为 0</p>
     */
    private Long parentId;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 路由地址
     * <p>前端路由 path，如 "/system/user"</p>
     */
    private String path;

    /**
     * 组件路径
     * <p>前端组件路径，如 "system/user/index"</p>
     */
    private String component;

    /**
     * 是否为外链
     * <ul>
     *   <li>0: 是外链</li>
     *   <li>1: 否</li>
     * </ul>
     */
    private Integer isFrame;

    /**
     * 是否缓存
     * <ul>
     *   <li>0: 缓存</li>
     *   <li>1: 不缓存</li>
     * </ul>
     */
    private Integer isCache;

    /**
     * 菜单类型
     * <ul>
     *   <li>M: 目录</li>
     *   <li>C: 菜单</li>
     *   <li>F: 按钮</li>
     * </ul>
     */
    private String menuType;

    /**
     * 菜单可见状态
     * <ul>
     *   <li>0: 显示</li>
     *   <li>1: 隐藏</li>
     * </ul>
     */
    private String visible;

    /**
     * 菜单状态
     * <ul>
     *   <li>0: 正常</li>
     *   <li>1: 停用</li>
     * </ul>
     */
    private String status;

    /**
     * 权限标识
     * <p>
     * 格式: "模块:功能:操作"，如 "system:user:list"
     * 用于 {@code @PreAuthorize} 等权限校验
     * </p>
     */
    private String perms;

    /**
     * 菜单图标
     * <p>前端显示的图标类名</p>
     */
    private String icon;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
