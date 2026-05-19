package com.medical.knowledgegraph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Neo4j配置属性类
 */
@Data
@ConfigurationProperties(prefix = "app.neo4j")
public class Neo4jProperties {

    /**
     * Neo4j连接URI
     */
    private String uri = "bolt://localhost:7687";

    /**
     * 用户名
     */
    private String username = "neo4j";

    /**
     * 密码
     */
    private String password = "neo4jpass";

    /**
     * 最大连接数
     */
    private Integer maxConnectionPoolSize = 50;

    /**
     * 连接超时时间 (毫秒)
     */
    private Long connectionTimeout = 30000L;

    /**
     * 执行超时时间 (毫秒)
     */
    private Long maxTransactionRetryTime = 30000L;

    /**
     * 是否启用加密
     */
    private Boolean encrypted = false;

    /**
     * 信任策略
     */
    private String trustStrategy = "TRUST_ALL_CERTIFICATES";

    /**
     * 获取连接超时Duration
     */
    public Duration getConnectionTimeoutDuration() {
        return Duration.ofMillis(connectionTimeout);
    }

    /**
     * 获取事务重试超时Duration
     */
    public Duration getMaxTransactionRetryTimeDuration() {
        return Duration.ofMillis(maxTransactionRetryTime);
    }
}
