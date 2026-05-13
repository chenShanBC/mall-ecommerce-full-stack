package com.mallfei.user.domain.repository;

public interface LoginSmsCodeRepository {

    void saveLoginCode(String mobile, String code, int expireSeconds);

    String getLoginCode(String mobile);

    void deleteLoginCode(String mobile);

    boolean canSend(String mobile);

    void markSendCooldown(String mobile, int cooldownSeconds);
}
