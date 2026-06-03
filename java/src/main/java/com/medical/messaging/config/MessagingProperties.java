package com.medical.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息模块开关与预约事件参数，前缀 {@code medical.messaging}（见 application.yml）。
 */
@Data
@ConfigurationProperties(prefix = "medical.messaging")
public class MessagingProperties {

    /**
     * 是否启用 RabbitMQ。false 时使用 {@link com.medical.messaging.appointment.NoOpAppointmentEventPublisher}，
     * 且不注册 {@code @RabbitListener}（需配合 {@code spring.rabbitmq.listener.simple.auto-startup}）。
     */
    private boolean enabled = false;

    private Appointment appointment = new Appointment();

    @Data
    public static class Appointment {
        /** Topic 交换机名，默认 {@link com.medical.messaging.config.RabbitMqTopology#APPOINTMENT_EXCHANGE} */
        private String exchange = RabbitMqTopology.APPOINTMENT_EXCHANGE;
        private String dlxExchange = RabbitMqTopology.APPOINTMENT_DLX;
        /** 就诊开始前多少小时发 REMINDER 事件 */
        private int reminderHoursBefore = 2;
        /** 为 false 时 audit 队列仍消费但直接 ack，不落库 */
        private boolean auditEnabled = true;
    }
}
