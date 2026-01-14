package com.mayday.server.service;

import com.mayday.auth.annotation.DataScope;
import com.mayday.auth.entity.SysDept;
import com.mayday.auth.entity.SysUser;
import com.mayday.auth.entity.SysUserDept;
import com.mayday.auth.mapper.SysDeptMapper;
import com.mayday.auth.mapper.SysUserDeptMapper;
import com.mayday.auth.mapper.SysUserMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mayday.auth.entity.table.SysUserDeptTableDef.SYS_USER_DEPT;

/**
 * 用户查询服务
 * <p>
 * 演示如何使用 @DataScope 注解进行数据权限过滤
 * </p>
 *
 * @author MayDay Auth Generator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 查询用户列表（带数据权限过滤）
     * <p>
     * 根据当前登录用户的角色过滤数据：
     * <ul>
     *   <li>管理员(data_scope=1): 可看到所有用户</li>
     *   <li>部门经理(data_scope=4): 可看到本部门及子部门用户</li>
     *   <li>普通员工(data_scope=5): 只能看到自己</li>
     * </ul>
     * </p>
     *
     * @param queryParam 查询参数（继承自 BaseEntity，携带 dataScope SQL）
     * @return 用户列表
     */
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectUserList(SysUser queryParam) {
        // 获取 DataScopeAspect 注入的 SQL 条件
        String dataScopeSql = (String) queryParam.getParams().get("dataScope");
        log.info("数据权限 SQL 条件: {}", dataScopeSql);

        // 简化查询逻辑：直接根据数据权限类型过滤
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysUser> users;

        if (dataScopeSql == null || dataScopeSql.isEmpty()) {
            // 管理员：返回所有用户
            users = userMapper.selectAll();
        } else if (dataScopeSql.contains("u.user_id")) {
            // 仅本人：只返回当前用户
            users = new ArrayList<>();
            SysUser self = userMapper.selectOneById(loginUser.getUserId());
            if (self != null) {
                users.add(self);
            }
        } else {
            // 本部门或本部门及以下：支持多部门聚合查询
            List<Long> allQueryDeptIds = loginUser.getAllDeptIds();
            if (allQueryDeptIds != null && !allQueryDeptIds.isEmpty()) {
                // 查询所有关联部门及其子部门的用户 (根据 dataScopeSql 判断是否包含子部门)
                boolean includeChildren = dataScopeSql.contains("ancestors");
                users = getUsersByDeptIds(allQueryDeptIds, includeChildren);
            } else {
                users = new ArrayList<>();
            }
        }
        
        // 组装返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> item = new HashMap<>();
            // 必须返回原始 ID，否则前端编辑时 id 为 undefined
            item.put("id", user.getId()); 
            item.put("username", user.getUsername());
            // 返回原始状态编码，由前端控制渲染（颜色/标签）
            item.put("status", user.getStatus());
            
            // 查询用户所属部门
            List<String> deptNames = getDeptNamesByUserId(user.getId());
            item.put("deptNames", deptNames);
            
            result.add(item);
        }
        
        return result;
    }

    /**
     * 根据多个部门ID聚合查询用户
     * @param deptIds 部门ID列表
     * @param includeChildren 是否包含子部门
     */
    private List<SysUser> getUsersByDeptIds(List<Long> deptIds, boolean includeChildren) {
        // 1. 获取所有相关部门ID列表 (包含子部门)
        List<Long> allQueryDeptIds = new ArrayList<>(deptIds);
        
        if (includeChildren) {
            for (Long deptId : deptIds) {
                QueryWrapper deptWrapper = QueryWrapper.create()
                        .from("sys_dept")
                        .where("FIND_IN_SET(" + deptId + ", ancestors)");
                List<SysDept> childDepts = deptMapper.selectListByQuery(deptWrapper);
                for (SysDept child : childDepts) {
                    if (!allQueryDeptIds.contains(child.getId())) {
                        allQueryDeptIds.add(child.getId());
                    }
                }
            }
        }
        
        // 2. 查询这些部门下的用户
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.DEPT_ID.in(allQueryDeptIds));
        
        List<SysUserDept> userDepts = userDeptMapper.selectListByQuery(wrapper);
        
        // 3. 获取用户列表（去重）
        List<Long> userIds = userDepts.stream()
                .map(SysUserDept::getUserId)
                .distinct()
                .toList();
        
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return userMapper.selectListByIds(userIds);
    }

    /**
     * 获取用户所属的部门名称列表
     */
    private List<String> getDeptNamesByUserId(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .from(SYS_USER_DEPT)
                .where(SYS_USER_DEPT.USER_ID.eq(userId));
        
        List<SysUserDept> userDepts = userDeptMapper.selectListByQuery(wrapper);
        List<String> deptNames = new ArrayList<>();
        
        for (SysUserDept ud : userDepts) {
            SysDept dept = deptMapper.selectOneById(ud.getDeptId());
            if (dept != null) {
                deptNames.add(dept.getDeptName());
            }
        }
        
        return deptNames;
    }

    /**
     * 获取数据权限 SQL 条件（用于前端展示）
     */
    @DataScope(deptAlias = "d", userAlias = "u")
    public String getDataScopeSql(SysUser queryParam) {
        String dataScopeSql = (String) queryParam.getParams().get("dataScope");
        return dataScopeSql != null ? dataScopeSql : "无条件（管理员拥有全部权限）";
    }
}
