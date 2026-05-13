package com.mallfei.auth.infrastructure.auth;

import cn.dev33.satoken.dao.SaTokenDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Primary
public class RedisSaTokenDao implements SaTokenDao {

    private static final String DEFAULT_KEY_PREFIX = "mall:auth:sa-token:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String redisKeyPrefix;

    public RedisSaTokenDao(StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper,
                           @Value("${sa-token.redis-key-prefix:mall:auth:sa-token}") String redisKeyPrefix) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisKeyPrefix = normalizeKeyPrefix(redisKeyPrefix);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(buildRedisKey(key));
    }

    @Override
    public void set(String key, String value, long timeout) {
        String redisKey = buildRedisKey(key);
        if (timeout == NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(redisKey, value);
            return;
        }
        if (timeout <= 0) {
            stringRedisTemplate.delete(redisKey);
            return;
        }
        stringRedisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(timeout));
    }

    @Override
    public void update(String key, String value) {
        String redisKey = buildRedisKey(key);
        Long expireSeconds = stringRedisTemplate.getExpire(redisKey);
        if (expireSeconds == null || expireSeconds == -1) {
            stringRedisTemplate.opsForValue().set(redisKey, value);
            return;
        }
        if (expireSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(buildRedisKey(key));
    }

    @Override
    public long getTimeout(String key) {
        return normalizeTimeout(stringRedisTemplate.getExpire(buildRedisKey(key)));
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        String redisKey = buildRedisKey(key);
        if (timeout == NEVER_EXPIRE) {
            stringRedisTemplate.persist(redisKey);
            return;
        }
        if (timeout <= 0) {
            stringRedisTemplate.delete(redisKey);
            return;
        }
        stringRedisTemplate.expire(redisKey, Duration.ofSeconds(timeout));
    }

    @Override
    public Object getObject(String key) {
        String json = stringRedisTemplate.opsForValue().get(buildRedisKey(key));
        if (json == null) {
            return null;
        }
        try {
            RedisObjectValue wrapper = objectMapper.readValue(json, RedisObjectValue.class);
            Class<?> targetClass = Class.forName(wrapper.className());
            return objectMapper.readValue(wrapper.json(), targetClass);
        } catch (Exception e) {
            throw new IllegalStateException("读取 Sa-Token Redis 对象失败: " + key, e);
        }
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        String redisKey = buildRedisKey(key);
        String payload = serializeObject(key, object);
        if (timeout == NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(redisKey, payload);
            return;
        }
        if (timeout <= 0) {
            stringRedisTemplate.delete(redisKey);
            return;
        }
        stringRedisTemplate.opsForValue().set(redisKey, payload, Duration.ofSeconds(timeout));
    }

    @Override
    public void updateObject(String key, Object object) {
        String redisKey = buildRedisKey(key);
        Long expireSeconds = stringRedisTemplate.getExpire(redisKey);
        String payload = serializeObject(key, object);
        if (expireSeconds == null || expireSeconds == -1) {
            stringRedisTemplate.opsForValue().set(redisKey, payload);
            return;
        }
        if (expireSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(redisKey, payload, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void deleteObject(String key) {
        stringRedisTemplate.delete(buildRedisKey(key));
    }

    @Override
    public long getObjectTimeout(String key) {
        return normalizeTimeout(stringRedisTemplate.getExpire(buildRedisKey(key)));
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        updateTimeout(key, timeout);
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        String pattern = buildPattern(prefix, keyword);
        List<String> keys = stringRedisTemplate.execute((RedisCallback<List<String>>) connection -> scanKeys(connection, pattern));
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        keys.sort(sortType ? String::compareTo : Collections.reverseOrder());
        int safeStart = Math.max(start, 0);
        if (safeStart >= keys.size()) {
            return Collections.emptyList();
        }
        int end = size <= 0 ? keys.size() : Math.min(safeStart + size, keys.size());
        return new ArrayList<>(keys.subList(safeStart, end));
    }

    private List<String> scanKeys(RedisConnection connection, String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        List<String> result = new ArrayList<>();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                result.add(removeRedisKeyPrefix(new String(cursor.next(), StandardCharsets.UTF_8)));
            }
        }
        return result;
    }

    private String buildPattern(String prefix, String keyword) {
        String safePrefix = buildRedisKey(prefix == null ? "" : prefix);
        String safeKeyword = keyword == null ? "" : keyword;
        return safePrefix + "*" + safeKeyword + "*";
    }

    private long normalizeTimeout(Long expireSeconds) {
        if (expireSeconds == null || expireSeconds == -2) {
            return NOT_VALUE_EXPIRE;
        }
        if (expireSeconds == -1) {
            return NEVER_EXPIRE;
        }
        return expireSeconds;
    }

    private String serializeObject(String key, Object object) {
        try {
            return objectMapper.writeValueAsString(new RedisObjectValue(object.getClass().getName(), objectMapper.writeValueAsString(object)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("写入 Sa-Token Redis 对象失败: " + key, e);
        }
    }

    private String buildRedisKey(String key) {
        return redisKeyPrefix + (key == null ? "" : key);
    }

    private String removeRedisKeyPrefix(String redisKey) {
        if (redisKey == null || !redisKey.startsWith(redisKeyPrefix)) {
            return redisKey;
        }
        return redisKey.substring(redisKeyPrefix.length());
    }

    private String normalizeKeyPrefix(String keyPrefix) {
        if (!StringUtils.hasText(keyPrefix)) {
            return DEFAULT_KEY_PREFIX;
        }
        String normalized = keyPrefix.trim();
        return normalized.endsWith(":") ? normalized : normalized + ":";
    }

    private record RedisObjectValue(String className, String json) {
    }
}
