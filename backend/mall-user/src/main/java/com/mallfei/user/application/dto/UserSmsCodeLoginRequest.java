package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UserSmsCodeLoginRequest", description = "用户短信验证码登录请求")
public record UserSmsCodeLoginRequest(
        @Schema(description = "手机号", example = "13800000000")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String mobile,
        @Schema(description = "验证码", example = "123456")
        @NotBlank(message = "验证码不能为空")
        @Size(min = 6, max = 6, message = "验证码长度必须为6位") String code
) {
}
