package com.mallfei.user.infrastructure.repository;

import com.mallfei.user.domain.repository.LoginCaptchaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisLoginCaptchaRepository implements LoginCaptchaRepository {

    private static final String CHALLENGE_KEY_PREFIX = "mall:user:login:captcha:challenge:";
    private static final String VERIFIED_KEY_PREFIX = "mall:user:login:captcha:verified:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLoginCaptchaRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveChallenge(String captchaToken, int targetOffset, int expireSeconds) {
        stringRedisTemplate.opsForValue().set(CHALLENGE_KEY_PREFIX + captchaToken, String.valueOf(targetOffset), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Integer getTargetOffset(String captchaToken) {
        String value = stringRedisTemplate.opsForValue().get(CHALLENGE_KEY_PREFIX + captchaToken);
        return value == null ? null : Integer.parseInt(value);
    }

    @Override
    public void deleteChallenge(String captchaToken) {
        stringRedisTemplate.delete(CHALLENGE_KEY_PREFIX + captchaToken);
    }

    @Override
    public void saveVerifiedToken(String captchaToken, String verifyToken, int expireSeconds) {
        stringRedisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + captchaToken, verifyToken, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean consumeVerifiedToken(String captchaToken, String verifyToken) {
        String key = VERIFIED_KEY_PREFIX + captchaToken;
        String cachedToken = stringRedisTemplate.opsForValue().get(key);
        if (cachedToken == null || !cachedToken.equals(verifyToken)) {
            return false;
        }
        stringRedisTemplate.delete(key);
        stringRedisTemplate.delete(CHALLENGE_KEY_PREFIX + captchaToken);
        return true;
    }
}
