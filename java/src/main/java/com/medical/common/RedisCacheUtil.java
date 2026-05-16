package com.medical.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
        } catch (Exception e) {
            log.error("Redis set error: key={}, error={}", key, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            // 处理 BigDecimal 类型转换（Redis 可能反序列化为 Double）
            if (clazz == BigDecimal.class && value instanceof Number) {
                return (T) new BigDecimal(value.toString());
            }
            return (T) value;
        } catch (Exception e) {
            log.error("Redis get error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete error: key={}, error={}", key, e.getMessage());
        }
    }

    public void deleteAll(Collection<String> keys) {
        try {
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis deleteAll error: error={}", e.getMessage());
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis increment error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Redis decrement error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
        } catch (Exception e) {
            log.error("Redis setIfAbsent error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }
}