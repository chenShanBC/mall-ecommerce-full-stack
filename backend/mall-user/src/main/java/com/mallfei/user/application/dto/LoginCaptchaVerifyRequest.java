package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginCaptchaVerifyRequest", description = "登录拼图验证码校验请求")
public record LoginCaptchaVerifyRequest(
        @Schema(description = "验证码令牌")
        @NotBlank(message = "验证码令牌不能为空") String captchaToken,
        @Schema(description = "滑块偏移量", example = "188")
        @Min(value = 0, message = "滑块偏移量不能小于0") int offset
) {
}
