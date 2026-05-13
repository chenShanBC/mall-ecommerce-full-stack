package com.mallfei.user.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginCaptchaVerifyResult", description = "登录拼图验证码校验结果")
public record LoginCaptchaVerifyResult(
        @Schema(description = "是否校验通过")
        boolean verified,
        @Schema(description = "校验通过凭证")
        String verifyToken
) {
}
