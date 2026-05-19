package com.medical.knowledgegraph.config;

import com.medical.knowledgegraph.exception.KnowledgeGraphException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Config.TrustStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Neo4j 数据库配置类，基于 Neo4j Java Driver 5.x API。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jConfig {

    private final Neo4jProperties neo4jProperties;

    /**
     * 创建 Neo4j Driver 实例；容器关闭时自动调用 {@link Driver#close()}。
     */
    @Bean(destroyMethod = "close")
    public Driver neo4jDriver() {
        log.info("初始化 Neo4j 驱动: uri={}, username={}",
                neo4jProperties.getUri(),
                neo4jProperties.getUsername());

        try {
            Config config = buildDriverConfig();

            Driver driver = GraphDatabase.driver(
                    neo4jProperties.getUri(),
                    AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()),
                    config
            );

            verifyConnection(driver);
            return driver;
        } catch (Exception e) {
            log.error("Neo4j 驱动初始化失败", e);
            throw new KnowledgeGraphException("NEO4J_INIT_ERROR", "Neo4j 驱动初始化失败: " + e.getMessage(), e);
        }
    }

    private Config buildDriverConfig() {
        Config.ConfigBuilder builder = Config.builder()
                .withMaxConnectionPoolSize(neo4jProperties.getMaxConnectionPoolSize())
                .withConnectionTimeout(neo4jProperties.getConnectionTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS)
                .withMaxTransactionRetryTime(neo4jProperties.getMaxTransactionRetryTimeDuration().toMillis(), TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(neo4jProperties.getEncrypted())) {
            builder.withEncryption();
            applyTrustStrategy(builder);
        } else {
            builder.withoutEncryption();
        }

        return builder.build();
    }

    private void applyTrustStrategy(Config.ConfigBuilder builder) {
        String strategy = neo4jProperties.getTrustStrategy();
        if (strategy == null) {
            return;
        }
        switch (strategy.toUpperCase()) {
            case "TRUST_ALL_CERTIFICATES" -> builder.withTrustStrategy(TrustStrategy.trustAllCertificates());
            case "TRUST_SYSTEM_CA_SIGNED_CERTIFICATES" ->
                    builder.withTrustStrategy(TrustStrategy.trustSystemCertificates());
            default -> log.warn("未识别的 Neo4j 信任策略: {}，使用系统 CA 证书", strategy);
        }
    }

    private void verifyConnection(Driver driver) {
        try (Session session = driver.session()) {
            session.run("RETURN 1 AS num").single();
            log.info("Neo4j 连接验证成功");
        }
    }
}
