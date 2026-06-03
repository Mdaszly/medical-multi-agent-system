package com.medical.messaging.support;

import com.medical.common.RedisCacheUtil;
import com.medical.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MQ 消费幂等：基于 Redis SET NX，键格式 {@link RedisKeyConstant#MQ_PROCESSED}。
 *
 * <p>与 {@link IdempotentConsumerExecutor} 配合：先占位 → 业务成功保留键 / 业务失败 {@link #release}，
 * 避免「第一次处理失败进 DLQ，重放却被当成已处理而 ack 跳过」。
 */
@Component
@RequiredArgsConstructor
public class IdempotentMessageHandler {

    public enum AcquireResult {
        /** 首次获得处理权 */
        ACQUIRED,
        /** 已成功处理过，可 ack 跳过 */
        ALREADY_PROCESSED,
        /** Redis 不可用，应 nack 重入队而非当作已处理 */
        REDIS_UNAVAILABLE
    }

    private final RedisCacheUtil redisCacheUtil;

    /**
     * @return true 表示首次处理，false 表示已处理过（应 ack 跳过）
     * @deprecated 优先使用 {@link #tryAcquireResult(String)}，以区分 Redis 故障
     */
    @Deprecated
    public boolean tryAcquire(String eventId) {
        return tryAcquireResult(eventId) == AcquireResult.ACQUIRED;
    }

    public AcquireResult tryAcquireResult(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return AcquireResult.ACQUIRED;
        }
        String key = formatKey(eventId);
        Boolean acquired = redisCacheUtil.setIfAbsent(key, "1", RedisKeyConstant.MQ_PROCESSED_TTL);
        if (acquired == null) {
            return AcquireResult.REDIS_UNAVAILABLE;
        }
        return Boolean.TRUE.equals(acquired) ? AcquireResult.ACQUIRED : AcquireResult.ALREADY_PROCESSED;
    }

    /**
     * 业务失败时释放幂等占位，允许 MQ 重试或 DLQ 人工重放再次处理。
     */
    public void release(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return;
        }
        redisCacheUtil.delete(formatKey(eventId));
    }

    private static String formatKey(String eventId) {
        return String.format(RedisKeyConstant.MQ_PROCESSED, eventId);
    }
}
