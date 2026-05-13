package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UserSmsCodeSendRequest", description = "用户短信验证码发送请求")
public record UserSmsCodeSendRequest(
        @Schema(description = "手机号", example = "13800000000")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String mobile
) {
}
