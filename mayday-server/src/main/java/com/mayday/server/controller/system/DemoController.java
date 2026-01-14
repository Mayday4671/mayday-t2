package com.mayday.server.controller.system;

import com.mayday.auth.annotation.DataScope;
import com.mayday.auth.common.R;
import com.mayday.auth.entity.BaseEntity;
import com.mayday.auth.entity.SysUser;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mayday.server.service.UserQueryService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demo 控制器
 * <p>
 * 演示如何使用权限模块的各种功能。
 * </p>
 *
 * @author MayDay Auth Generator
 */
@Slf4j
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final UserQueryService userQueryService;

    /**
     * 获取当前登录用户信息
     * <p>
     * 演示如何使用 SecurityUtils 获取当前用户
     * </p>
     */
    @GetMapping("/currentUser")
    public R<Map<String, Object>> getCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        return R.ok(Map.of(
            "userId", loginUser.getUserId(),
            "username", loginUser.getUsername(),
            "currentDeptId", loginUser.getCurrentDeptId() != null ? loginUser.getCurrentDeptId() : "无部门",
            "roleCount", loginUser.getRoles() != null ? loginUser.getRoles().size() : 0,
            "permissionCount", loginUser.getPermissions() != null ? loginUser.getPermissions().size() : 0,
            "permissions", loginUser.getPermissions()
        ));
    }

    /**
     * 查询用户列表（带数据权限表格展示）
     * <p>
     * 这是核心接口：根据当前登录用户的角色，返回过滤后的用户数据。
     * 不同用户登录会看到不同的数据范围！
     * </p>
     */
    @GetMapping("/userList")
    public R<Map<String, Object>> getUserList() {
        SysUser queryParam = new SysUser();
        
        // 获取过滤后的用户列表
        List<Map<String, Object>> userList = userQueryService.selectUserList(queryParam);
        
        // 获取当前数据权限SQL（用于展示）
        String dataScopeSql = userQueryService.getDataScopeSql(new SysUser());
        
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String dataScope = "未知";
        if (loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
            dataScope = loginUser.getRoles().get(0).getDataScope();
            switch (dataScope) {
                case "1" -> dataScope = "全部数据";
                case "2" -> dataScope = "自定义数据";
                case "3" -> dataScope = "本部门数据";
                case "4" -> dataScope = "本部门及以下数据";
                case "5" -> dataScope = "仅本人数据";
            }
        }
        
        return R.ok(Map.of(
            "userList", userList,
            "total", userList.size(),
            "dataScopeSql", dataScopeSql,
            "dataScope", dataScope,
            "currentUser", loginUser.getUsername(),
            "currentDeptId", loginUser.getCurrentDeptId() != null ? loginUser.getCurrentDeptId() : 0
        ));
    }

    /**
     * 权限校验示例 - 需要 system:user:list 权限
     * <p>
     * 演示 @PreAuthorize 注解的使用
     * </p>
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<String> listUsers() {
        return R.ok("您有 system:user:list 权限，可以查看用户列表！");
    }

    /**
     * 权限校验示例 - 需要 system:user:add 权限
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<String> addUser() {
        return R.ok("您有 system:user:add 权限，可以新增用户！");
    }

    /**
     * 权限校验示例 - 需要 system:user:remove 权限
     * <p>
     * 注意: 普通员工角色没有此权限
     * </p>
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public R<String> deleteUser(@PathVariable Long id) {
        return R.ok("您有 system:user:remove 权限，可以删除用户！");
    }

    /**
     * 数据权限示例
     * <p>
     * 演示 @DataScope 注解的使用。
     * 该方法会根据用户的角色，自动过滤数据。
     * </p>
     */
    @GetMapping("/dataScope")
    @DataScope(deptAlias = "d", userAlias = "u")
    public R<Map<String, Object>> dataScopeDemo(QueryParam param) {
        // DataScopeAspect 会自动在 param.params.dataScope 中注入 SQL 条件
        String dataScopeSql = (String) param.getParams().get("dataScope");
        
        return R.ok(Map.of(
            "说明", "DataScopeAspect 已注入 SQL 条件",
            "生成的SQL条件", dataScopeSql != null ? dataScopeSql : "无条件 (可能是管理员)",
            "使用方式", "在 MyBatis XML 中使用: ${params.dataScope}"
        ));
    }

    /**
     * 查询参数 (继承 BaseEntity 以支持 DataScope)
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class QueryParam extends BaseEntity {
        private String keyword;
    }

    /**
     * 多角色演示 - 展示张三在不同部门的权限差异
     */
    @GetMapping("/roleInfo")
    public R<Map<String, Object>> showRoleInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        List<String> roleKeys = new ArrayList<>();
        if (loginUser.getRoles() != null) {
            loginUser.getRoles().forEach(role -> roleKeys.add(role.getRoleKey()));
        }
        
        return R.ok(Map.of(
            "message", "当前用户在当前部门下的角色",
            "username", loginUser.getUsername(),
            "currentDeptId", loginUser.getCurrentDeptId() != null ? loginUser.getCurrentDeptId() : "无部门",
            "roles", roleKeys,
            "tip", "张三在技术部(2)是 manager，在市场部(3)是 employee，切换部门后角色会变化！"
        ));
    }
}

