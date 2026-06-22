package com.mallfei.user.infrastructure.repository;

import com.mallfei.user.domain.repository.LoginSmsCodeRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisLoginSmsCodeRepository implements LoginSmsCodeRepository {

    private static final String LOGIN_CODE_KEY_PREFIX = "mall:user:login:sms:code:";
    private static final String LOGIN_CODE_COOLDOWN_KEY_PREFIX = "mall:user:login:sms:cooldown:";
    private static final String MOBILE_BIND_CODE_KEY_PREFIX = "mall:user:mobile-bind:sms:code:";
    private static final String MOBILE_BIND_CODE_COOLDOWN_KEY_PREFIX = "mall:user:mobile-bind:sms:cooldown:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLoginSmsCodeRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveLoginCode(String mobile, String code, int expireSeconds) {
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY_PREFIX + mobile, code, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getLoginCode(String mobile) {
        return stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY_PREFIX + mobile);
    }

    @Override
    public void deleteLoginCode(String mobile) {
        stringRedisTemplate.delete(LOGIN_CODE_KEY_PREFIX + mobile);
    }

    @Override
    public boolean canSend(String mobile) {
        Boolean exists = stringRedisTemplate.hasKey(LOGIN_CODE_COOLDOWN_KEY_PREFIX + mobile);
        return exists == null || !exists;
    }

    @Override
    public void markSendCooldown(String mobile, int cooldownSeconds) {
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_COOLDOWN_KEY_PREFIX + mobile, "1", cooldownSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void saveMobileBindCode(Long userId, String mobile, String code, int expireSeconds) {
        stringRedisTemplate.opsForValue().set(mobileBindCodeKey(userId, mobile), code, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getMobileBindCode(Long userId, String mobile) {
        return stringRedisTemplate.opsForValue().get(mobileBindCodeKey(userId, mobile));
    }

    @Override
    public void deleteMobileBindCode(Long userId, String mobile) {
        stringRedisTemplate.delete(mobileBindCodeKey(userId, mobile));
    }

    @Override
    public boolean canSendMobileBindCode(Long userId, String mobile) {
        Boolean exists = stringRedisTemplate.hasKey(MOBILE_BIND_CODE_COOLDOWN_KEY_PREFIX + userId + ":" + mobile);
        return exists == null || !exists;
    }

    @Override
    public void markMobileBindSendCooldown(Long userId, String mobile, int cooldownSeconds) {
        stringRedisTemplate.opsForValue().set(MOBILE_BIND_CODE_COOLDOWN_KEY_PREFIX + userId + ":" + mobile, "1", cooldownSeconds, TimeUnit.SECONDS);
    }

    private String mobileBindCodeKey(Long userId, String mobile) {
        return MOBILE_BIND_CODE_KEY_PREFIX + userId + ":" + mobile;
    }
}
