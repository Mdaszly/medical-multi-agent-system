package com.medical.service;

import com.medical.common.CacheMonitor;
import com.medical.common.DistributedLock;
import com.medical.common.RedisCacheUtil;
import com.medical.constant.RedisKeyConstant;
import com.medical.model.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 医生缓存管理器
 * 
 * <p>负责管理医生数据的Redis缓存，提供以下核心功能：
 * <ul>
 *   <li>缓存查询与加载</li>
 *   <li>缓存更新与删除</li>
 *   <li>缓存穿透/击穿/雪崩防护</li>
 *   <li>缓存预热</li>
 * </ul>
 * 
 * <p>Redis是一个内存数据库，用于快速存储和读取数据。
 * 我们将医生信息缓存在Redis中，可以大大提高查询速度，减少数据库压力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorCacheManager {

    private final RedisCacheUtil redisCacheUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final DistributedLock distributedLock;
    private final CacheMonitor cacheMonitor;

    private static final Random RANDOM = new Random();

    /**
     * 根据科室名称获取医生列表（带缓存机制）
     * 
     * <p>查询流程：
     * 1. 先检查是否有空值缓存（防止缓存穿透）
     * 2. 再检查正常缓存
     * 3. 如果缓存未命中，使用分布式锁保护加载数据（防止缓存击穿）
     * 4. 如果Redis异常，降级到直接查询数据库
     * 
     * @param department 科室名称
     * @param dataLoader 数据加载器，用于从数据库查询数据
     * @return 医生列表
     */
    public List<DoctorVO> getDoctorListByDepartment(String department, Supplier<List<DoctorVO>> dataLoader) {
        String cacheKey = buildCacheKey(department);
        String emptyCacheKey = RedisKeyConstant.DOCTOR_DEPT_EMPTY.formatted(department);

        try {
            // 检查空值缓存（防止缓存穿透）
            Boolean hasEmptyCache = redisCacheUtil.hasKey(emptyCacheKey);
            if (Boolean.TRUE.equals(hasEmptyCache)) {
                log.debug("空值缓存命中: {}", emptyCacheKey);
                cacheMonitor.recordHit();
                return Collections.emptyList();
            }

            // 检查正常缓存
            @SuppressWarnings("unchecked")
            List<DoctorVO> cached = redisCacheUtil.get(cacheKey, List.class);
            if (cached != null) {
                log.debug("缓存命中: {}", cacheKey);
                cacheMonitor.recordHit();
                return cached;
            }

            // 缓存未命中，记录并加载数据
            cacheMonitor.recordMiss();
            return loadWithLockProtection(department, cacheKey, emptyCacheKey, dataLoader);

        } catch (Exception e) {
            // Redis异常时降级到数据库查询
            log.error("缓存获取异常, 降级到数据库: department={}, error={}", department, e.getMessage());
            cacheMonitor.recordError();
            return dataLoader.get();
        }
    }

    /**
     * 使用分布式锁保护数据加载过程（防止缓存击穿）
     * 
     * <p>缓存击穿：当某个热门缓存过期时，大量请求同时访问，
     * 会导致所有请求都直接访问数据库，造成数据库压力过大。
     * 
     * <p>解决方法：使用分布式锁，只让一个请求去加载数据，
     * 其他请求等待后读取缓存。
     */
    private List<DoctorVO> loadWithLockProtection(String department, String cacheKey,
                                                   String emptyCacheKey, Supplier<List<DoctorVO>> dataLoader) {
        String lockKey = RedisKeyConstant.DOCTOR_LOCK_PREFIX.formatted(department);

        // 尝试获取分布式锁，最多等待5秒
        boolean locked = distributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS);

        if (locked) {
            try {
                return loadAndCacheData(department, cacheKey, emptyCacheKey, dataLoader);
            } finally {
                distributedLock.unlock(lockKey);
            }
        }

        // 未获取到锁，等待一小段时间后重试读取缓存
        log.debug("未获取到锁，等待后重试: {}", lockKey);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 重试读取缓存
        @SuppressWarnings("unchecked")
        List<DoctorVO> cached = redisCacheUtil.get(cacheKey, List.class);
        if (cached != null) {
            cacheMonitor.recordHit();
            return cached;
        }

        // 重试后仍未命中，直接加载数据（兜底策略）
        log.warn("重试后仍未命中，直接加载数据: department={}", department);
        cacheMonitor.recordLoad();
        List<DoctorVO> data = dataLoader.get();
        cacheData(department, cacheKey, emptyCacheKey, data);
        return data;
    }

    /**
     * 加载数据并写入缓存
     */
    private List<DoctorVO> loadAndCacheData(String department, String cacheKey,
                                             String emptyCacheKey, Supplier<List<DoctorVO>> dataLoader) {
        // 双重检查：获取锁后再次检查缓存，防止重复加载
        @SuppressWarnings("unchecked")
        List<DoctorVO> cached = redisCacheUtil.get(cacheKey, List.class);
        if (cached != null) {
            cacheMonitor.recordHit();
            return cached;
        }

        // 检查空值缓存
        Boolean hasEmptyCache = redisCacheUtil.hasKey(emptyCacheKey);
        if (Boolean.TRUE.equals(hasEmptyCache)) {
            cacheMonitor.recordHit();
            return Collections.emptyList();
        }

        // 从数据库加载数据
        cacheMonitor.recordLoad();
        List<DoctorVO> data = dataLoader.get();
        cacheData(department, cacheKey, emptyCacheKey, data);
        return data;
    }

    /**
     * 将数据写入缓存
     * 
     * <p>根据数据是否为空，采用不同的缓存策略：
     * - 数据为空：设置空值缓存（短TTL），防止缓存穿透
     * - 数据不为空：设置正常缓存（带随机TTL），防止缓存雪崩
     */
    private void cacheData(String department, String cacheKey, String emptyCacheKey, List<DoctorVO> data) {
        if (data == null || data.isEmpty()) {
            // 空结果缓存，TTL较短（5分钟）
            Duration emptyTtl = getRandomTtl(RedisKeyConstant.DOCTOR_EMPTY_CACHE_TTL);
            redisCacheUtil.set(emptyCacheKey, "1", emptyTtl);
            log.debug("空结果缓存: key={}, ttl={}", emptyCacheKey, emptyTtl);
        } else {
            // 正常数据缓存，TTL随机化（25-35分钟）
            Duration ttl = getRandomTtl(RedisKeyConstant.DOCTOR_CACHE_TTL);
            redisCacheUtil.set(cacheKey, data, ttl);
            log.debug("数据缓存写入: key={}, ttl={}, count={}", cacheKey, ttl, data.size());
        }
    }

    /**
     * 获取随机TTL时间（防止缓存雪崩）
     * 
     * <p>缓存雪崩：当大量缓存同时过期时，会导致大量请求直接访问数据库。
     * 
     * <p>解决方法：为每个缓存设置随机的过期时间，避免同时过期。
     */
    private Duration getRandomTtl(Duration baseTtl) {
        long minMillis = RedisKeyConstant.DOCTOR_CACHE_TTL_MIN.toMillis();
        long maxMillis = RedisKeyConstant.DOCTOR_CACHE_TTL_MAX.toMillis();
        long randomMillis = minMillis + RANDOM.nextLong(maxMillis - minMillis + 1);
        return Duration.ofMillis(randomMillis);
    }

    /**
     * 删除指定科室的缓存
     * 
     * <p>当医生数据发生变化时（如新增、更新、删除医生），需要删除相关缓存，
     * 确保下次查询时能获取最新数据。
     */
    public void evictDepartmentCache(String department) {
        String cacheKey = buildCacheKey(department);
        String emptyCacheKey = RedisKeyConstant.DOCTOR_DEPT_EMPTY.formatted(department);

        redisCacheUtil.delete(cacheKey);
        redisCacheUtil.delete(emptyCacheKey);

        cacheMonitor.recordDelete();
        log.info("科室缓存已删除: department={}", department);
    }

    /**
     * 删除所有医生相关缓存
     */
    public void evictAllDoctorCache() {
        try {
            // 使用通配符删除所有科室缓存
            String pattern = RedisKeyConstant.DOCTOR_DEPT_LIST.replace("%s", "*");
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                cacheMonitor.recordDelete();
                log.info("所有科室医生缓存已删除: count={}", keys.size());
            }

            // 删除所有空值缓存
            String emptyPattern = RedisKeyConstant.DOCTOR_DEPT_EMPTY.formatted("*");
            var emptyKeys = stringRedisTemplate.keys(emptyPattern);
            if (emptyKeys != null && !emptyKeys.isEmpty()) {
                stringRedisTemplate.delete(emptyKeys);
                log.info("所有空值缓存已删除: count={}", emptyKeys.size());
            }
        } catch (Exception e) {
            log.error("清除缓存异常: error={}", e.getMessage());
            cacheMonitor.recordError();
        }
    }

    /**
     * 缓存预热（系统启动时预加载热门数据）
     * 
     * <p>缓存预热：在系统启动时或定时任务中，提前将常用数据加载到缓存中，
     * 可以避免用户请求时的首次查询延迟。
     */
    public void warmupDepartment(String department, Supplier<List<DoctorVO>> dataLoader) {
        String cacheKey = buildCacheKey(department);
        String emptyCacheKey = RedisKeyConstant.DOCTOR_DEPT_EMPTY.formatted(department);

        // 先删除旧缓存
        redisCacheUtil.delete(cacheKey);
        redisCacheUtil.delete(emptyCacheKey);

        try {
            List<DoctorVO> data = dataLoader.get();
            if (data != null && !data.isEmpty()) {
                Duration ttl = getRandomTtl(RedisKeyConstant.DOCTOR_CACHE_TTL);
                redisCacheUtil.set(cacheKey, data, ttl);
                log.info("缓存预热完成: department={}, count={}, ttl={}", department, data.size(), ttl);
            } else {
                redisCacheUtil.set(emptyCacheKey, "1", RedisKeyConstant.DOCTOR_EMPTY_CACHE_TTL);
                log.info("缓存预热完成(空值): department={}", department);
            }
        } catch (Exception e) {
            log.error("缓存预热失败: department={}, error={}", department, e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheMonitor.CacheStats getCacheStats() {
        return cacheMonitor.getStats();
    }

    /**
     * 检查Redis健康状态
     */
    public boolean isCacheHealthy() {
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(String department) {
        return RedisKeyConstant.DOCTOR_DEPT_LIST.formatted(department);
    }
}