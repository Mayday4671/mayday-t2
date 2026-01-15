package com.mayday.auth.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 登录响应 VO
 * <p>
 * 支持两种登录状态：
 * <ul>
 *   <li><b>需要选择部门</b>: needSelectDept=true, 返回 tempToken + deptList</li>
 *   <li><b>登录成功</b>: needSelectDept=false, 返回最终 token + 用户信息</li>
 * </ul>
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
public class LoginVo {

    /**
     * 是否需要选择部门
     * <p>当用户属于多个部门时为 true</p>
     */
    private Boolean needSelectDept = false;

    /**
     * 临时 Token (仅用于多部门选择场景)
     * <p>用户选择部门后，携带此 Token 调用 selectDept 接口</p>
     */
    private String tempToken;

    /**
     * 最终 JWT Token
     * <p>单部门用户或选择部门后返回</p>
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 当前部门ID
     */
    private Long currentDeptId;

    /**
     * 用户可选的部门列表 (多部门场景)
     */
    private List<DeptOption> deptList;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 部门选项
     */
    @Data
    public static class DeptOption {
        private Long deptId;
        private String deptName;
        private Boolean isDefault;
    }
}
