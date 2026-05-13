package com.mallfei.start.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;

@Configuration
@Profile("dev")
public class RedisDevConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisDevConfig.class);

    @Bean
    public ApplicationRunner redisMisconfAutoFixRunner(RedisConnectionFactory redisConnectionFactory) {
        return args -> {
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                Object response = connection.execute(
                        "CONFIG",
                        "SET".getBytes(StandardCharsets.UTF_8),
                        "stop-writes-on-bgsave-error".getBytes(StandardCharsets.UTF_8),
                        "no".getBytes(StandardCharsets.UTF_8)
                );
                String result = formatRedisResponse(response);
                log.info("Redis dev auto fix applied: CONFIG SET stop-writes-on-bgsave-error no, result={}", result);
            } catch (Exception exception) {
                log.warn("Redis dev auto fix skipped: {}", exception.getMessage());
            }
        };
    }

    private String formatRedisResponse(Object response) {
        if (response == null) {
            return "null";
        }
        if (response instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (response instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof byte[] bytes) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                if (item != null) {
                    return String.valueOf(item);
                }
            }
            return "empty";
        }
        return String.valueOf(response);
    }
}
