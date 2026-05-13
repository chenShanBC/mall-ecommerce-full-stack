package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UserPasswordLoginRequest", description = "用户密码登录请求")
public record UserPasswordLoginRequest(
        @Schema(description = "手机号", example = "13800000000")
        @NotBlank(message = "手机号不能为空") String mobile,
        @Schema(description = "登录密码", example = "123456")
        @NotBlank(message = "密码不能为空") String password,
        @Schema(description = "验证码令牌")
        @NotBlank(message = "验证码令牌不能为空") String captchaToken,
        @Schema(description = "验证码校验凭证")
        @NotBlank(message = "验证码校验凭证不能为空") String captchaVerifyToken
) {
}
