package com.automotive.costing.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Multi-instance safe fixed-window counter using Redis INCR + EXPIRE.
 * Enable with: app.security.rate-limit-store=redis and spring.data.redis.*
 */
@Component
@ConditionalOnProperty(name = "app.security.rate-limit-store", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitStore implements RateLimitStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String redisKey = "rl:" + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            return count <= limit;
        } catch (Exception ex) {
            // Fail-open with warning — prefer availability; edge gateway should still protect
            log.warn("Redis rate limit unavailable, allowing request: {}", ex.getMessage());
            return true;
        }
    }
}
