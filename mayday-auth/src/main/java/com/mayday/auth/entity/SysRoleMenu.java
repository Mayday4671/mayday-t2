package com.mayday.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;

/**
 * 角色-菜单关联实体
 * <p>
 * 对应数据库表 {@code sys_role_menu}，实现角色与菜单/权限的多对多关联。
 * 用于控制角色拥有的功能权限。
 * </p>
 *
 * <h3>使用场景:</h3>
 * <ol>
 *   <li>角色授权时，将菜单/按钮权限关联到角色</li>
 *   <li>用户登录时，根据角色查询所有关联的菜单，提取 perms 字段作为权限列表</li>
 *   <li>前端根据菜单列表渲染导航栏和按钮</li>
 * </ol>
 *
 * <h3>与权限校验的关系:</h3>
 * <p>
 * 登录时通过本表查询用户拥有的所有权限标识 (perms)，存入 {@link com.mayday.auth.model.LoginUser#getPermissions()}。
 * 后续使用 {@code @PreAuthorize("hasAuthority('xxx')")} 进行权限校验。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see SysRole
 * @see SysMenu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_menu")
public class SysRoleMenu extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long roleId;

    /**
     * 菜单ID (联合主键)
     */
    @Id(keyType = KeyType.None)
    private Long menuId;
}
