package com.qblog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录 DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
