package com.mallfei.user.application.vo;

public record SmsCodeSendResult(
        String mobile,
        int expireSeconds,
        String debugCode
) {
}
