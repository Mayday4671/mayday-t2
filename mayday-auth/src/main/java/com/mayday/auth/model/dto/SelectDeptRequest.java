package com.mayday.auth.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 选择部门请求 DTO (多部门用户登录第二步)
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
public class SelectDeptRequest {

    /**
     * 临时 Token (登录第一步返回的)
     */
    @NotBlank(message = "临时Token不能为空")
    private String tempToken;

    /**
     * 选择的部门ID
     */
    @NotNull(message = "部门ID不能为空")
    private Long deptId;
}
