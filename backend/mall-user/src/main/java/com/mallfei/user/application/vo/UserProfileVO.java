package com.mallfei.user.application.vo;

public record UserProfileVO(
        Long userId,
        String mobile,
        String nickname,
        String avatarUrl,
        String identityLabel
) {
}
