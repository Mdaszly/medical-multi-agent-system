package com.medical.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控器
 * 
 * <p>用于监控缓存的使用情况，收集以下指标：
 * <ul>
 *   <li>缓存命中次数</li>
 *   <li>缓存未命中次数</li>
 *   <li>缓存加载次数</li>
 *   <li>缓存错误次数</li>
 *   <li>缓存删除次数</li>
 * </ul>
 * 
 * <p>通过监控这些指标，可以评估缓存策略的效果，
 * 及时发现缓存问题。
 */
@Slf4j
@Component
public class CacheMonitor {

    /**
     * 缓存命中次数：从缓存中成功获取到数据的次数
     */
    private final AtomicLong hitCount = new AtomicLong(0);

    /**
     * 缓存未命中次数：缓存中没有找到数据的次数
     */
    private final AtomicLong missCount = new AtomicLong(0);

    /**
     * 缓存加载次数：从数据库加载数据的次数
     */
    private final AtomicLong loadCount = new AtomicLong(0);

    /**
     * 缓存错误次数：缓存操作发生错误的次数
     */
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 缓存删除次数：删除缓存的次数
     */
    private final AtomicLong deleteCount = new AtomicLong(0);

    /**
     * 记录缓存命中
     */
    public void recordHit() {
        hitCount.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        missCount.incrementAndGet();
    }

    /**
     * 记录数据加载
     */
    public void recordLoad() {
        loadCount.incrementAndGet();
    }

    /**
     * 记录缓存错误
     */
    public void recordError() {
        errorCount.incrementAndGet();
    }

    /**
     * 记录缓存删除
     */
    public void recordDelete() {
        deleteCount.incrementAndGet();
    }

    /**
     * 计算缓存命中率
     * 
     * <p>命中率 = 命中次数 / (命中次数 + 未命中次数) * 100%
     * 
     * <p>命中率是衡量缓存效果的重要指标：
     * - 命中率越高，说明缓存越有效
     * - 命中率越低，说明缓存策略可能需要调整
     */
    public double getHitRate() {
        long hit = hitCount.get();
        long total = hit + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hit / total * 100;
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        long hit = hitCount.get();
        long miss = missCount.get();
        long total = hit + miss;
        double hitRate = total == 0 ? 0.0 : (double) hit / total * 100;

        return new CacheStats(
            hit,
            miss,
            total,
            hitRate,
            loadCount.get(),
            errorCount.get(),
            deleteCount.get()
        );
    }

    /**
     * 定时打印缓存统计报告（每分钟执行一次）
     * 
     * <p>通过定时任务定期输出缓存统计信息，方便监控和排查问题。
     */
    @Scheduled(fixedRate = 60000)
    public void reportStats() {
        CacheStats stats = getStats();
        if (stats.totalRequests > 0) {
            log.info("=== 缓存监控报告 ===");
            log.info("命中次数: {}, 未命中次数: {}, 总请求: {}",
                stats.hitCount, stats.missCount, stats.totalRequests);
            log.info("缓存命中率: {}%", String.format("%.2f", stats.hitRate));
            log.info("加载次数: {}, 错误次数: {}, 删除次数: {}",
                stats.loadCount, stats.errorCount, stats.deleteCount);
            log.info("====================");
        }
    }

    /**
     * 重置统计数据
     * 
     * <p>用于定期重置统计数据，重新开始统计。
     */
    public void reset() {
        hitCount.set(0);
        missCount.set(0);
        loadCount.set(0);
        errorCount.set(0);
        deleteCount.set(0);
    }

    /**
     * 缓存统计信息类
     */
    @Data
    public static class CacheStats {
        /**
         * 缓存命中次数
         */
        private final long hitCount;

        /**
         * 缓存未命中次数
         */
        private final long missCount;

        /**
         * 总请求次数
         */
        private final long totalRequests;

        /**
         * 缓存命中率（百分比）
         */
        private final double hitRate;

        /**
         * 数据加载次数
         */
        private final long loadCount;

        /**
         * 缓存错误次数
         */
        private final long errorCount;

        /**
         * 缓存删除次数
         */
        private final long deleteCount;

        /**
         * 判断命中率是否达到目标值
         * 
         * @param targetRate 目标命中率（百分比）
         * @return 是否达到目标
         */
        public boolean isHitRateAcceptable(double targetRate) {
            return hitRate >= targetRate;
        }
    }
}