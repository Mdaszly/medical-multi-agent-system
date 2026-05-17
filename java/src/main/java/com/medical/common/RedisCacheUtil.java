package com.medical.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;

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

    /**
     * 带穿透防护的查询方法
     * 
     * <p>当查询不存在的数据时，会缓存空值，防止缓存穿透攻击。
     * 
     * @param key 缓存键
     * @param clazz 返回类型
     * @param dbFallback 数据库查询函数
     * @param cacheTtl 正常数据缓存时间
     * @param nullTtl 空值缓存时间
     * @return 查询结果
     */
    public <T> T queryWithPassThrough(String key, Class<T> clazz, 
                                       Function<String, T> dbFallback,
                                       Duration cacheTtl, Duration nullTtl) {
        try {
            // 1. 查询缓存
            String json = getString(key);
            if (StrUtil.isNotBlank(json)) {
                log.debug("缓存命中: {}", key);
                return JSONUtil.toBean(json, clazz);
            }

            // 2. 判断是否为空值缓存
            if (json != null) {
                log.debug("空值缓存命中: {}", key);
                return null;
            }

            // 3. 查询数据库
            T result = dbFallback.apply(key);

            // 4. 如果数据库也无数据，缓存空值
            if (result == null) {
                log.debug("数据库无数据，设置空值缓存: {}", key);
                redisTemplate.opsForValue().set(key, "", nullTtl);
                return null;
            }

            // 5. 正常缓存数据
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(result), cacheTtl);
            log.debug("数据缓存写入: {}", key);
            return result;

        } catch (Exception e) {
            log.error("缓存查询异常，降级到数据库: key={}, error={}", key, e.getMessage());
            return dbFallback.apply(key);
        }
    }

    /**
     * 获取字符串类型的缓存值
     */
    private String getString(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 带随机TTL的缓存设置（防止缓存雪崩）
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param baseTtl 基准TTL
     * @param jitterSeconds 随机抖动范围（秒）
     */
    public void setWithRandomTtl(String key, Object value, Duration baseTtl, int jitterSeconds) {
        try {
            long baseSeconds = baseTtl.getSeconds();
            long randomSeconds = baseSeconds + (long) (Math.random() * jitterSeconds * 2 - jitterSeconds);
            Duration actualTtl = Duration.ofSeconds(Math.max(randomSeconds, 60));
            
            redisTemplate.opsForValue().set(key, value, actualTtl);
            log.debug("缓存写入(随机TTL): key={}, ttl={}s", key, actualTtl.getSeconds());
        } catch (Exception e) {
            log.error("Redis setWithRandomTtl error: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 带随机TTL的JSON缓存设置
     */
    public void setJsonWithRandomTtl(String key, Object value, Duration baseTtl, int jitterSeconds) {
        try {
            String json = JSONUtil.toJsonStr(value);
            long baseSeconds = baseTtl.getSeconds();
            long randomSeconds = baseSeconds + (long) (Math.random() * jitterSeconds * 2 - jitterSeconds);
            Duration actualTtl = Duration.ofSeconds(Math.max(randomSeconds, 60));
            
            redisTemplate.opsForValue().set(key, json, actualTtl);
            log.debug("JSON缓存写入(随机TTL): key={}, ttl={}s", key, actualTtl.getSeconds());
        } catch (Exception e) {
            log.error("Redis setJsonWithRandomTtl error: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取缓存值（JSON格式）
     */
    public <T> T getJson(String key, Class<T> clazz) {
        try {
            String json = getString(key);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            return JSONUtil.toBean(json, clazz);
        } catch (Exception e) {
            log.error("Redis getJson error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 设置缓存值（JSON格式）
     */
    public void setJson(String key, Object value, Duration timeout) {
        try {
            String json = JSONUtil.toJsonStr(value);
            redisTemplate.opsForValue().set(key, json, timeout);
        } catch (Exception e) {
            log.error("Redis setJson error: key={}, error={}", key, e.getMessage());
        }
    }
}