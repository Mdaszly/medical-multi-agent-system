package com.medical.messaging.support;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * 消费端统一模板：幂等占位 → 执行业务 → ack；失败则 release 幂等键并向上抛出让调用方 nack。
 *
 * <p>三种 ack 语义：
 * <ul>
 *   <li>ALREADY_PROCESSED → ack（重复投递，安全跳过）</li>
 *   <li>REDIS_UNAVAILABLE → nack requeue（短暂故障，稍后重试）</li>
 *   <li>业务异常 → release + 由外层 nack 进 DLQ</li>
 * </ul>
 */
@Slf4j
public final class IdempotentConsumerExecutor {

    private IdempotentConsumerExecutor() {
    }

    @FunctionalInterface
    public interface MessageWork {
        void run() throws Exception;
    }

    public static void execute(IdempotentMessageHandler handler,
                               String idempotencyKey,
                               MessageWork work,
                               Channel channel,
                               long deliveryTag) throws Exception {
        IdempotentMessageHandler.AcquireResult acquireResult = handler.tryAcquireResult(idempotencyKey);
        if (acquireResult == IdempotentMessageHandler.AcquireResult.ALREADY_PROCESSED) {
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
            return;
        }
        if (acquireResult == IdempotentMessageHandler.AcquireResult.REDIS_UNAVAILABLE) {
            log.warn("Idempotency Redis unavailable, nack requeue: key={}", idempotencyKey);
            AppointmentMessageAckHelper.nackRequeue(channel, deliveryTag);
            return;
        }
        try {
            work.run();
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
        } catch (Exception e) {
            handler.release(idempotencyKey);
            throw e;
        }
    }
}
