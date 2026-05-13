package com.mallfei.user.domain.service;

public record LoginCaptchaRenderResult(
        String backgroundImage,
        String sliderImage,
        int targetOffset,
        int topOffset,
        int puzzleSize
) {
}
