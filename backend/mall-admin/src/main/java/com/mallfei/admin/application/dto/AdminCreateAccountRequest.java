package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminCreateAccountRequest(
        Long userId,
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "昵称不能为空") String nickname,
        @NotBlank(message = "角色编码不能为空") String roleCode,
        @NotEmpty(message = "权限列表不能为空") List<String> permissions
) {
}
