package com.mallfei.user.application.vo;

public record UserLoginResult(
        String token,
        Long userId,
        String mobile,
        String nickname,
        String avatarUrl,
        boolean mobileBound
) {
}
