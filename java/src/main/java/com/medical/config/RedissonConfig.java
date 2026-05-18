package com.medical.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置类
 * 
 * <p>Redisson 提供了强大的分布式锁实现，包括：
 * <ul>
 *   <li>可重入锁（Reentrant Lock）</li>
 *   <li>公平锁（Fair Lock）</li>
 *   <li>联锁（MultiLock）</li>
 *   <li>读写锁（ReadWriteLock）</li>
 *   <li>信号量（Semaphore）</li>
 *   <li>看门狗机制（自动续期）</li>
 * </ul>
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单节点模式配置
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort)
              .setPassword(StrUtil.isNotBlank(redisPassword) ? redisPassword : null)
              .setConnectionPoolSize(64)
              .setConnectionMinimumIdleSize(24)
              .setIdleConnectionTimeout(10000)
              .setConnectTimeout(10000)
              .setTimeout(3000)
              .setRetryAttempts(3)
              .setRetryInterval(1000);
        
        return Redisson.create(config);
    }
    
    /**
     * 简单的字符串工具类，避免引入额外依赖
     */
    private static class StrUtil {
        public static boolean isNotBlank(String str) {
            return str != null && !str.trim().isEmpty();
        }
    }
}