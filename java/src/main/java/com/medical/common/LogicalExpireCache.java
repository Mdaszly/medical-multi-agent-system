package com.medical.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.medical.constant.RedisKeyConstant;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 逻辑过期缓存工具类
 *
 * <p>用于解决缓存击穿问题：当热点数据过期时，不是直接删除，而是返回旧数据，
 * 同时异步刷新缓存，这样可以保证高并发场景下的性能和数据一致性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogicalExpireCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutorService refreshExecutor = Executors.newFixedThreadPool(4);

    /**
     * 封装逻辑过期数据结构
     */
    @Data
    public static class LogicalExpireData<T> {
        private LocalDateTime expireTime;
        private T data;
    }

    /**
     * 设置逻辑过期缓存
     *
     * @param key 缓存键
     * @param data 数据
     * @param expireDuration 过期时间
     */
    public <T> void set(String key, T data, Duration expireDuration) {
        try {
            LogicalExpireData<T> wrapper = new LogicalExpireData<>();
            wrapper.setData(data);
            wrapper.setExpireTime(LocalDateTime.now().plus(expireDuration));

            String json = JSONUtil.toJsonStr(wrapper);
            stringRedisTemplate.opsForValue().set(key, json);
            log.debug("逻辑过期缓存设置: key={}, expireTime={}", key, wrapper.getExpireTime());
        } catch (Exception e) {
            log.error("逻辑过期缓存设置失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取逻辑过期缓存（带自动刷新）
     *
     * <p>查询流程：
     * 1. 读取缓存
     * 2. 如果缓存不存在，返回null
     * 3. 如果缓存未过期，直接返回数据
     * 4. 如果缓存已过期，获取锁并异步刷新，同时返回旧数据
     *
     * @param key 缓存键
     * @param clazz 返回类型
     * @param refreshSupplier 数据刷新函数（从数据库获取最新数据）
     * @param expireDuration 新数据的过期时间
     * @return 缓存数据（可能是旧数据，如果正在刷新）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz, Supplier<T> refreshSupplier, Duration expireDuration) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);

            // 缓存不存在
            if (StrUtil.isBlank(json)) {
                log.debug("逻辑过期缓存不存在: {}", key);
                return null;
            }

            // 解析缓存数据
            LogicalExpireData<T> wrapper = JSONUtil.toBean(json, LogicalExpireData.class);

            // 检查是否过期
            if (wrapper.getExpireTime().isAfter(LocalDateTime.now())) {
                // 未过期，直接返回
                log.debug("逻辑过期缓存未过期: {}", key);
                return (T) wrapper.getData();
            }

            // 已过期，尝试获取锁刷新缓存
            String lockKey = String.format(RedisKeyConstant.LOCK_REFRESH, key);
            String lockValue = UUID.randomUUID().toString();

            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(30));

            if (Boolean.TRUE.equals(locked)) {
                // 获取锁成功，异步刷新缓存
                log.debug("逻辑过期缓存已过期，开始异步刷新: {}", key);
                refreshExecutor.submit(() -> {
                    try {
                        T newData = refreshSupplier.get();
                        if (newData != null) {
                            set(key, newData, expireDuration);
                        }
                    } catch (Exception e) {
                        log.error("逻辑过期缓存刷新失败: key={}, error={}", key, e.getMessage());
                    } finally {
                        // 释放锁
                        stringRedisTemplate.delete(lockKey);
                    }
                });
            }

            // 返回旧数据（即使正在刷新）
            log.debug("逻辑过期缓存已过期，返回旧数据: {}", key);
            return (T) wrapper.getData();

        } catch (Exception e) {
            log.error("逻辑过期缓存获取异常: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除逻辑过期缓存
     */
    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
            log.debug("逻辑过期缓存删除: {}", key);
        } catch (Exception e) {
            log.error("逻辑过期缓存删除失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 强制刷新缓存（同步）
     */
    public <T> void refresh(String key, Class<T> clazz, Supplier<T> refreshSupplier, Duration expireDuration) {
        try {
            T data = refreshSupplier.get();
            if (data != null) {
                set(key, data, expireDuration);
                log.info("逻辑过期缓存强制刷新成功: {}", key);
            }
        } catch (Exception e) {
            log.error("逻辑过期缓存强制刷新失败: key={}, error={}", key, e.getMessage());
        }
    }
}