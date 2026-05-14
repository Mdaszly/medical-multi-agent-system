package com.medical.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式锁工具类
 * 
 * <p>分布式锁用于在多个服务器节点之间协调访问共享资源。
 * 在缓存场景中，主要用于防止缓存击穿。
 * 
 * <p>工作原理：
 * 1. 使用Redis的setIfAbsent命令实现分布式锁
 * 2. 设置锁的过期时间，防止死锁
 * 3. 提供本地锁作为降级方案
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 锁的前缀，用于区分不同类型的锁
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 本地重入锁，作为分布式锁的降级方案
     * 当Redis不可用时，可以使用本地锁保证单进程内的线程安全
     */
    private static final ReentrantLock LOCAL_LOCK = new ReentrantLock();

    /**
     * 尝试获取分布式锁
     * 
     * <p>使用Redis的setIfAbsent命令实现：
     * - 如果key不存在，设置key并返回true（获取锁成功）
     * - 如果key已存在，返回false（获取锁失败）
     * 
     * @param key 锁的名称
     * @param timeout 锁的过期时间
     * @param unit 时间单位
     * @return 是否获取锁成功
     */
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        String lockKey = LOCK_PREFIX + key;
        long waitTime = unit.toMillis(timeout);

        // 使用Redis的setIfAbsent命令设置锁，同时设置过期时间
        boolean locked = Boolean.TRUE.equals(
            stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMillis(waitTime))
        );

        if (locked) {
            log.debug("获取分布式锁成功: {}", lockKey);
        } else {
            log.debug("获取分布式锁失败: {}", lockKey);
        }

        return locked;
    }

    /**
     * 尝试获取分布式锁（带本地锁降级）
     * 
     * <p>先尝试获取分布式锁，如果失败则使用本地锁。
     * 这样可以在Redis不可用时，保证单进程内的线程安全。
     */
    public boolean tryLockWithLocalFallback(String key, long timeout, TimeUnit unit) {
        // 先尝试获取分布式锁
        if (tryLock(key, timeout, unit)) {
            return true;
        }

        // 分布式锁失败，尝试本地锁
        LOCAL_LOCK.lock();
        try {
            // 获取本地锁后，再次尝试获取分布式锁
            if (tryLock(key, timeout, unit)) {
                return true;
            }
            log.warn("本地锁获取失败: {}", key);
            return false;
        } finally {
            LOCAL_LOCK.unlock();
        }
    }

    /**
     * 释放分布式锁
     * 
     * <p>直接删除Redis中的锁key来释放锁。
     */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        try {
            Boolean result = stringRedisTemplate.delete(lockKey);
            if (Boolean.TRUE.equals(result)) {
                log.debug("释放分布式锁成功: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: {}, error: {}", lockKey, e.getMessage());
        }
    }

    /**
     * 释放分布式锁（带本地锁降级）
     */
    public void unlockWithLocalFallback(String key) {
        try {
            unlock(key);
        } finally {
            LOCAL_LOCK.unlock();
        }
    }

    /**
     * 在锁的保护下执行操作
     * 
     * <p>这是一个便捷方法，自动处理获取锁、执行操作、释放锁的流程。
     * 
     * @param key 锁的名称
     * @param timeout 锁的过期时间
     * @param unit 时间单位
     * @param action 要执行的操作
     * @return 操作结果，如果获取锁失败返回null
     */
    public <T> T executeWithLock(String key, long timeout, TimeUnit unit, Callable<T> action) {
        try {
            if (tryLockWithLocalFallback(key, timeout, unit)) {
                return action.call();
            }
            log.warn("无法获取锁，执行降级: {}", key);
            return null;
        } catch (Exception e) {
            log.error("锁执行异常: {}, error: {}", key, e.getMessage());
            return null;
        } finally {
            if (LOCAL_LOCK.isHeldByCurrentThread()) {
                LOCAL_LOCK.unlock();
            }
        }
    }

    /**
     * 函数式接口，用于封装需要在锁保护下执行的操作
     */
    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }
}