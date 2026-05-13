package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UserRegisterRequest", description = "用户注册请求")
public record UserRegisterRequest(
        @Schema(description = "手机号", example = "13900000001")
        @NotBlank(message = "手机号不能为空") String mobile,
        @Schema(description = "登录密码", example = "123456")
        @NotBlank(message = "密码不能为空") String password,
        @Schema(description = "用户昵称", example = "manual-user-001")
        @NotBlank(message = "昵称不能为空") String nickname
) {
}
