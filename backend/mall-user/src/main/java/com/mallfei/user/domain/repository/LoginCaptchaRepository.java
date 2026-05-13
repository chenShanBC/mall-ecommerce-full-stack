package com.mallfei.user.domain.repository;

public interface LoginCaptchaRepository {

    void saveChallenge(String captchaToken, int targetOffset, int expireSeconds);

    Integer getTargetOffset(String captchaToken);

    void deleteChallenge(String captchaToken);

    void saveVerifiedToken(String captchaToken, String verifyToken, int expireSeconds);

    boolean consumeVerifiedToken(String captchaToken, String verifyToken);
}
