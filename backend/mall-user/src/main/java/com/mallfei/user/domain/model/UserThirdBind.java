package com.mallfei.user.domain.model;

public record UserThirdBind(
        Long id,
        Long userId,
        String thirdType,
        String thirdUid,
        String thirdNickname,
        String thirdAvatar
) {
    public static final String THIRD_TYPE_ALIPAY = "ALIPAY";
}
