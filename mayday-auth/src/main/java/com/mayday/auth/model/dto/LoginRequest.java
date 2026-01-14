package com.mayday.auth.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码 (可选)
     */
    private String code;

    /**
     * 验证码唯一标识 (可选)
     */
    private String uuid;
}
