package com.mayday.crawler.util;

import com.mayday.auth.entity.SysRole;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 爬虫模块数据权限工具类
 * <p>
 * 用于获取当前登录用户信息并构建数据权限过滤条件。
 * </p>
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
public class CrawlerDataScopeUtil {

    private CrawlerDataScopeUtil() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，如果获取失败返回null
     */
    public static Long getCurrentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 获取当前登录用户的部门ID
     *
     * @return 部门ID，如果获取失败返回null
     */
    public static Long getCurrentDeptId() {
        try {
            return SecurityUtils.getDeptId();
        } catch (Exception e) {
            log.warn("获取当前部门ID失败", e);
            return null;
        }
    }

    /**
     * 获取当前用户的数据权限范围
     * <ul>
     *     <li>1 = 全部数据</li>
     *     <li>2 = 自定义数据</li>
     *     <li>3 = 本部门数据</li>
     *     <li>4 = 本部门及以下数据</li>
     *     <li>5 = 仅本人数据</li>
     * </ul>
     *
     * @return dataScope 值，默认返回 "5"（仅本人）
     */
    public static String getDataScope() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
                // 取权限最大的角色的 dataScope（数值越小权限越大）
                return loginUser.getRoles().stream()
                        .map(SysRole::getDataScope)
                        .filter(ds -> ds != null && !ds.isEmpty())
                        .min(String::compareTo)
                        .orElse("5");
            }
        } catch (Exception e) {
            log.warn("获取数据权限范围失败", e);
        }
        return "5"; // 默认仅本人
    }

    /**
     * 应用数据权限过滤到 QueryWrapper
     * <p>
     * 根据当前用户的 dataScope 自动添加 WHERE 条件：
     * <ul>
     *     <li>1 (全部): 不添加条件</li>
     *     <li>3 (本部门): WHERE dept_id = 当前部门ID</li>
     *     <li>4 (本部门及以下): WHERE dept_id IN (当前部门及子部门) - 暂简化为本部门</li>
     *     <li>5 (仅本人): WHERE create_by = 当前用户ID</li>
     * </ul>
     * </p>
     *
     * @param wrapper   QueryWrapper 实例
     * @param createByColumn 创建人字段列
     * @param deptIdColumn   部门ID字段列
     */
    public static void applyDataScope(QueryWrapper wrapper, QueryColumn createByColumn, QueryColumn deptIdColumn) {
        String dataScope = getDataScope();
        log.debug("当前用户数据权限: {}", dataScope);

        switch (dataScope) {
            case "1" -> {
                // 全部数据，不添加条件
            }
            case "2" -> {
                // 自定义数据权限（暂简化为本部门）
                Long deptId = getCurrentDeptId();
                if (deptId != null) {
                    wrapper.and(deptIdColumn.eq(deptId));
                }
            }
            case "3" -> {
                // 本部门数据
                Long deptId = getCurrentDeptId();
                if (deptId != null) {
                    wrapper.and(deptIdColumn.eq(deptId));
                }
            }
            case "4" -> {
                // 本部门及以下数据（暂简化为本部门，完整实现需要查询子部门）
                Long deptId = getCurrentDeptId();
                if (deptId != null) {
                    wrapper.and(deptIdColumn.eq(deptId));
                }
            }
            case "5" -> {
                // 仅本人数据
                Long userId = getCurrentUserId();
                if (userId != null) {
                    wrapper.and(createByColumn.eq(userId));
                }
            }
            default -> {
                // 未知权限，默认仅本人
                Long userId = getCurrentUserId();
                if (userId != null) {
                    wrapper.and(createByColumn.eq(userId));
                }
            }
        }
    }

    /**
     * 判断当前用户是否为管理员（拥有全部数据权限）
     *
     * @return true 表示拥有全部数据权限
     */
    public static boolean hasAllDataScope() {
        return "1".equals(getDataScope()) || SecurityUtils.isAdmin();
    }
}
