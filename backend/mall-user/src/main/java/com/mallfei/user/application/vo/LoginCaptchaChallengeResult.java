package com.mallfei.user.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginCaptchaChallengeResult", description = "登录拼图验证码挑战结果")
public record LoginCaptchaChallengeResult(
        @Schema(description = "验证码令牌")
        String captchaToken,
        @Schema(description = "背景图base64")
        String backgroundImage,
        @Schema(description = "拼图块图base64")
        String sliderImage,
        @Schema(description = "目标偏移量")
        int targetOffset,
        @Schema(description = "拼图块Y坐标")
        int topOffset,
        @Schema(description = "拼图块尺寸")
        int puzzleSize,
        @Schema(description = "允许误差")
        int tolerance,
        @Schema(description = "有效期秒数")
        int expireSeconds
) {
}
