package com.mallfei.user.domain.repository;

public interface LoginSmsCodeRepository {

    void saveLoginCode(String mobile, String code, int expireSeconds);

    String getLoginCode(String mobile);

    void deleteLoginCode(String mobile);

    boolean canSend(String mobile);

    void markSendCooldown(String mobile, int cooldownSeconds);

    void saveMobileBindCode(Long userId, String mobile, String code, int expireSeconds);

    String getMobileBindCode(Long userId, String mobile);

    void deleteMobileBindCode(Long userId, String mobile);

    boolean canSendMobileBindCode(Long userId, String mobile);

    void markMobileBindSendCooldown(Long userId, String mobile, int cooldownSeconds);
}
