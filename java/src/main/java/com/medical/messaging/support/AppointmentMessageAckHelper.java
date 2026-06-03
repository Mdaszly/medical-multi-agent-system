package com.medical.messaging.support;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 手动 ack 工具（application.yml 中 {@code spring.rabbitmq.listener.simple.acknowledge-mode=manual}）。
 *
 * <p>业务队列均配置 x-dead-letter-exchange，{@link #nackToDlq} 不重入队，失败消息进入 {@code appointment.dlq}。
 */
@Slf4j
public final class AppointmentMessageAckHelper {

    private AppointmentMessageAckHelper() {
    }

    public static void ack(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("RabbitMQ ack failed: deliveryTag={}, error={}", deliveryTag, e.getMessage());
        }
    }

    /** requeue=false，配合队列 DLX 进入死信队列 */
    public static void nackToDlq(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("RabbitMQ nack failed: deliveryTag={}, error={}", deliveryTag, e.getMessage());
        }
    }

    /** requeue=true，用于 Redis 幂等组件短暂不可用等可恢复场景 */
    public static void nackRequeue(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, true);
        } catch (IOException e) {
            log.error("RabbitMQ nack requeue failed: deliveryTag={}, error={}", deliveryTag, e.getMessage());
        }
    }
}
