package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserPasswordChangeRequest", description = "用户密码修改请求")
public record UserPasswordChangeRequest(
        @Schema(description = "原密码", example = "123456")
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @Schema(description = "新密码", example = "12345678")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度需在6到20位之间") String newPassword,
        @Schema(description = "确认新密码", example = "12345678")
        @NotBlank(message = "确认密码不能为空") String confirmPassword
) {
}
