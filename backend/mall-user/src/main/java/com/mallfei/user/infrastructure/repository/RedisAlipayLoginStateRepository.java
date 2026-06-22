package com.mallfei.user.infrastructure.repository;

import com.mallfei.user.domain.service.AlipayLoginStateRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisAlipayLoginStateRepository implements AlipayLoginStateRepository {

    private static final String STATE_PREFIX = "mall:user:login:alipay:state:";
    private static final String TICKET_PREFIX = "mall:user:login:alipay:ticket:";
    private static final String AUTH_CODE_PROCESSING_PREFIX = "mall:user:login:alipay:auth-code:processing:";
    private static final String AUTH_CODE_RESULT_PREFIX = "mall:user:login:alipay:auth-code:result:";

    private final StringRedisTemplate redisTemplate;

    public RedisAlipayLoginStateRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveState(String state, int expireSeconds) {
        redisTemplate.opsForValue().set(STATE_PREFIX + state, "1", expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean consumeState(String state) {
        String key = STATE_PREFIX + state;
        Boolean deleted = redisTemplate.delete(key);
        return deleted != null && deleted;
    }

    @Override
    public void saveLoginTicket(String ticket, Long userId, int expireSeconds) {
        redisTemplate.opsForValue().set(TICKET_PREFIX + ticket, String.valueOf(userId), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Long consumeLoginTicket(String ticket) {
        String key = TICKET_PREFIX + ticket;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        redisTemplate.delete(key);
        return Long.valueOf(value);
    }

    @Override
    public Long getAuthCodeLoginUserId(String authCode) {
        String value = redisTemplate.opsForValue().get(AUTH_CODE_RESULT_PREFIX + authCode);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Override
    public boolean markAuthCodeProcessing(String authCode, int expireSeconds) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(AUTH_CODE_PROCESSING_PREFIX + authCode, "1", expireSeconds, TimeUnit.SECONDS);
        return success != null && success;
    }

    @Override
    public void saveAuthCodeLoginUserId(String authCode, Long userId, int expireSeconds) {
        redisTemplate.opsForValue().set(AUTH_CODE_RESULT_PREFIX + authCode, String.valueOf(userId), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void clearAuthCodeProcessing(String authCode) {
        redisTemplate.delete(AUTH_CODE_PROCESSING_PREFIX + authCode);
    }
}
