package com.mallfei.admin.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AdminPasswordLoginRequest", description = "管理员密码登录请求")
public record AdminPasswordLoginRequest(
        @Schema(description = "管理员账号", example = "admin")
        @NotBlank(message = "管理员账号不能为空") String username,
        @Schema(description = "管理员密码", example = "123456")
        @NotBlank(message = "管理员密码不能为空") String password
) {
}
