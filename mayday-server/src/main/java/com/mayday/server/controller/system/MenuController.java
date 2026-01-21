package com.mayday.server.controller.system;

import com.mayday.auth.common.R;
import com.mayday.auth.entity.SysMenu;
import com.mayday.auth.mapper.SysMenuMapper;
import com.mayday.auth.mapper.SysRoleMenuMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mayday.auth.entity.table.SysMenuTableDef.SYS_MENU;
import static com.mayday.auth.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;

/**
 * 菜单控制器
 *
 * @author MayDay Auth Generator
 */
@Slf4j
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    // ============ 菜单管理 CRUD ============

    /**
     * 获取菜单列表（树形结构）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<MenuTreeVo>> list() {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy(SYS_MENU.PARENT_ID.asc(), SYS_MENU.ORDER_NUM.asc());
        List<SysMenu> menus = menuMapper.selectListByQuery(wrapper);
        return R.ok(buildMenuTree(menus, 0L));
    }

    /**
     * 获取菜单树（用于角色权限分配）
     */
    @GetMapping("/tree")
    public R<List<MenuTreeVo>> tree() {
        List<SysMenu> menus = menuMapper.selectAll();
        return R.ok(buildMenuTree(menus, 0L));
    }

    /**
     * 获取菜单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public R<SysMenu> getInfo(@PathVariable("id") Long id) {
        SysMenu menu = menuMapper.selectOneById(id);
        return menu != null ? R.ok(menu) : R.fail("菜单不存在");
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public R<Void> add(@Valid @RequestBody MenuRequest request) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(request.getMenuName());
        menu.setParentId(request.getParentId());
        menu.setOrderNum(request.getOrderNum());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setMenuType(request.getMenuType());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setStatus(request.getStatus());
        menuMapper.insert(menu);
        
        log.info("新增菜单: {}", request.getMenuName());
        return R.ok();
    }

    /**
     * 修改菜单
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public R<Void> update(@PathVariable("id") Long id, @Valid @RequestBody MenuRequest request) {
        SysMenu menu = menuMapper.selectOneById(id);
        if (menu == null) {
            return R.fail("菜单不存在");
        }
        
        menu.setMenuName(request.getMenuName());
        menu.setParentId(request.getParentId());
        menu.setOrderNum(request.getOrderNum());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setMenuType(request.getMenuType());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setStatus(request.getStatus());
        menuMapper.update(menu);
        
        log.info("更新菜单: {}", menu.getMenuName());
        return R.ok();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public R<Void> delete(@PathVariable("id") Long id) {
        // 检查是否有子菜单
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_MENU.PARENT_ID.eq(id));
        if (menuMapper.selectCountByQuery(wrapper) > 0) {
            return R.fail("存在子菜单，无法删除");
        }
        
        // 删除角色菜单关联
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.MENU_ID.eq(id)));
        menuMapper.deleteById(id);
        
        log.info("删除菜单: {}", id);
        return R.ok();
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeVo> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .sorted(Comparator.comparing(SysMenu::getOrderNum))
                .map(m -> {
                    MenuTreeVo vo = new MenuTreeVo();
                    vo.setId(m.getId());
                    vo.setParentId(m.getParentId());
                    vo.setMenuName(m.getMenuName());
                    vo.setPath(m.getPath());
                    vo.setComponent(m.getComponent());
                    vo.setMenuType(m.getMenuType());
                    vo.setPerms(m.getPerms());
                    vo.setIcon(m.getIcon());
                    vo.setOrderNum(m.getOrderNum());
                    vo.setStatus(m.getStatus());
                    List<MenuTreeVo> children = buildMenuTree(menus, m.getId());
                    vo.setChildren(children.isEmpty() ? null : children);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ============ 动态路由 ============

    /**
     * 获取路由菜单
     * <p>
     * 自动补充父级菜单，确保菜单树完整
     * </p>
     */
    @GetMapping("/getRouters")
    public R<List<RouterVo>> getRouters() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        // 1. 如果是超级管理员，直接查询所有菜单 (不根据角色过滤)
        // 注意：此处 SecurityUtils.isAdmin() 内部判断的是 UserID == 1L
        if (SecurityUtils.isAdmin()) {
            QueryWrapper menuWrapper = QueryWrapper.create()
                    .from(SYS_MENU)
                    .where(SYS_MENU.MENU_TYPE.ne("F"))
                    .and(SYS_MENU.STATUS.eq("0"))
                    .orderBy(SYS_MENU.PARENT_ID.asc(), SYS_MENU.ORDER_NUM.asc());
            List<SysMenu> menus = menuMapper.selectListByQuery(menuWrapper);
            return R.ok(buildRouterTree(menus, 0L));
        }

        List<Long> roleIds = loginUser.getRoles().stream()
                .map(role -> role.getId())
                .collect(Collectors.toList());
        
        if (roleIds.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        
        // 2. 查询角色直接关联的菜单ID
        QueryWrapper roleMenuWrapper = QueryWrapper.create()
                .select(SYS_ROLE_MENU.MENU_ID)
                .from(SYS_ROLE_MENU)
                .where(SYS_ROLE_MENU.ROLE_ID.in(roleIds));
        
        Set<Long> directMenuIds = roleMenuMapper.selectListByQuery(roleMenuWrapper).stream()
                .map(rm -> rm.getMenuId())
                .collect(Collectors.toSet());
        
        if (directMenuIds.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        
        // 3. 查询这些菜单的详细信息
        List<SysMenu> directMenus = menuMapper.selectListByIds(new ArrayList<>(directMenuIds));
        
        // 4. 递归补充所有父级菜单ID
        Set<Long> allMenuIds = new java.util.HashSet<>(directMenuIds);
        for (SysMenu menu : directMenus) {
            Long parentId = menu.getParentId();
            while (parentId != null && parentId > 0) {
                allMenuIds.add(parentId);
                SysMenu parent = menuMapper.selectOneById(parentId);
                if (parent != null) {
                    parentId = parent.getParentId();
                } else {
                    break;
                }
            }
        }
        
        // 5. 查询完整的菜单列表 (排除按钮)
        QueryWrapper menuWrapper = QueryWrapper.create()
                .from(SYS_MENU)
                .where(SYS_MENU.ID.in(allMenuIds))
                .and(SYS_MENU.MENU_TYPE.ne("F"))
                .and(SYS_MENU.STATUS.eq("0"))
                .orderBy(SYS_MENU.PARENT_ID.asc(), SYS_MENU.ORDER_NUM.asc());
        
        List<SysMenu> menus = menuMapper.selectListByQuery(menuWrapper);
        return R.ok(buildRouterTree(menus, 0L));
    }

    private List<RouterVo> buildRouterTree(List<SysMenu> menus, Long parentId) {
        List<RouterVo> routers = new ArrayList<>();
        
        List<SysMenu> childMenus = menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .sorted(Comparator.comparing(SysMenu::getOrderNum))
                .collect(Collectors.toList());
        
        for (SysMenu menu : childMenus) {
            RouterVo router = new RouterVo();
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu, parentId == 0L));
            router.setComponent(getComponent(menu));
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
            
            List<RouterVo> children = buildRouterTree(menus, menu.getId());
            if (!children.isEmpty()) {
                router.setChildren(children);
                if ("M".equals(menu.getMenuType())) {
                    router.setRedirect(menu.getPath() + "/" + children.get(0).getPath());
                }
            } else {
                router.setChildren(null);
            }
            routers.add(router);
        }
        return routers;
    }

    private String getRouteName(SysMenu menu) {
        String path = menu.getPath();
        return path.substring(0, 1).toUpperCase() + path.substring(1);
    }

    private String getRouterPath(SysMenu menu, boolean isTopLevel) {
        return isTopLevel ? "/" + menu.getPath() : menu.getPath();
    }

    private String getComponent(SysMenu menu) {
        return "M".equals(menu.getMenuType()) ? "Layout" : menu.getComponent();
    }

    // ============ DTOs ============

    @Data
    public static class MenuRequest {
        @NotBlank(message = "菜单名称不能为空")
        private String menuName;
        private Long parentId = 0L;
        private Integer orderNum = 0;
        private String path;
        private String component;
        @NotBlank(message = "菜单类型不能为空")
        private String menuType;
        private String perms;
        private String icon;
        private String status = "0";
    }

    @Data
    public static class MenuTreeVo {
        private Long id;
        private Long parentId;
        private String menuName;
        private String path;
        private String component;
        private String menuType;
        private String perms;
        private String icon;
        private Integer orderNum;
        private String status;
        private List<MenuTreeVo> children;
    }

    @Data
    public static class RouterVo {
        private String name;
        private String path;
        private String redirect;
        private String component;
        private MetaVo meta;
        private List<RouterVo> children;
    }

    @Data
    public static class MetaVo {
        private String title;
        private String icon;

        public MetaVo(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }
}

