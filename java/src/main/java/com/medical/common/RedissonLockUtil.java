package com.medical.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁工具类
 * 
 * <p>提供基于 Redisson 的分布式锁操作，支持：
 * <ul>
 *   <li>可重入锁</li>
 *   <li>看门狗自动续期机制</li>
 *   <li>公平锁</li>
 *   <li>锁超时控制</li>
 * </ul>
 * 
 * <p>看门狗机制说明：
 * 当获取锁成功后，Redisson 会启动一个定时任务（默认每10秒执行一次），
 * 自动延长锁的过期时间（默认30秒）。这样可以避免业务执行时间超过锁超时时间
 * 导致锁提前释放的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockUtil {

    private final RedissonClient redissonClient;

    /**
     * 获取锁（带看门狗机制）
     * 
     * <p>锁获取后会自动续期，直到手动释放或进程退出。
     * 
     * @param lockKey 锁的键
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的初始过期时间（秒），如果为 -1 则使用看门狗机制
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (locked) {
                log.debug("获取锁成功: {}", lockKey);
            } else {
                log.debug("获取锁失败: {}", lockKey);
            }
            return locked;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 获取锁（使用看门狗机制，默认等待时间）
     * 
     * @param lockKey 锁的键
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, 10, -1);
    }

    /**
     * 获取锁（指定等待时间，使用看门狗机制）
     * 
     * @param lockKey 锁的键
     * @param waitTime 等待获取锁的最大时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime) {
        return tryLock(lockKey, waitTime, -1);
    }

    /**
     * 获取公平锁
     * 
     * <p>公平锁会按照请求顺序分配锁，避免某些线程长时间无法获取锁。
     * 
     * @param lockKey 锁的键
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的初始过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryFairLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getFairLock(lockKey);
        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (locked) {
                log.debug("获取公平锁成功: {}", lockKey);
            } else {
                log.debug("获取公平锁失败: {}", lockKey);
            }
            return locked;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取公平锁被中断: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     * 
     * @param lockKey 锁的键
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁异常: {}", lockKey, e);
        }
    }

    /**
     * 强制释放锁（不管是否当前线程持有）
     * 
     * @param lockKey 锁的键
     */
    public void forceUnlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isLocked()) {
                lock.forceUnlock();
                log.debug("强制释放锁成功: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("强制释放锁异常: {}", lockKey, e);
        }
    }

    /**
     * 检查锁是否被持有
     * 
     * @param lockKey 锁的键
     * @return 是否被持有
     */
    public boolean isLocked(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isLocked();
        } catch (Exception e) {
            log.error("检查锁状态异常: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 检查当前线程是否持有锁
     * 
     * @param lockKey 锁的键
     * @return 当前线程是否持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("检查锁持有状态异常: {}", lockKey, e);
            return false;
        }
    }
}