package com.mayday.auth.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 部门切换请求 DTO
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
public class SwitchDeptRequest {

    /**
     * 目标部门ID
     */
    @NotNull(message = "部门ID不能为空")
    private Long deptId;
}
