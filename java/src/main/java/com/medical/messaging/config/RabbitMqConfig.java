package com.medical.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ Bean 定义：交换机、队列、绑定、{@link RabbitTemplate}。
 *
 * <p>队列与 routing 关系见 {@link RabbitMqTopology}；仅在 {@code medical.messaging.enabled=true} 时装配。
 */
@Configuration
@ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
public class RabbitMqConfig {

    /** JSON 序列化信封对象，与 Consumer 反序列化一致 */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && correlationData != null) {
                org.slf4j.LoggerFactory.getLogger(RabbitMqConfig.class)
                        .warn("RabbitMQ publish not acknowledged: id={}, cause={}",
                                correlationData.getId(), cause);
            }
        });
        template.setReturnsCallback(returned ->
                org.slf4j.LoggerFactory.getLogger(RabbitMqConfig.class)
                        .warn("RabbitMQ returned message: exchange={}, routingKey={}, reply={}",
                                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }

    @Bean
    public TopicExchange appointmentExchange(MessagingProperties properties) {
        return new TopicExchange(properties.getAppointment().getExchange(), true, false);
    }

    @Bean
    public TopicExchange appointmentDlxExchange(MessagingProperties properties) {
        return new TopicExchange(properties.getAppointment().getDlxExchange(), true, false);
    }

    @Bean
    public Queue appointmentNotificationQueue(MessagingProperties properties) {
        return durableQueueWithDlx(
                RabbitMqTopology.QUEUE_NOTIFICATION,
                properties.getAppointment().getDlxExchange(),
                RabbitMqTopology.RK_DLQ);
    }

    @Bean
    public Queue appointmentSlotRestoreQueue(MessagingProperties properties) {
        return durableQueueWithDlx(
                RabbitMqTopology.QUEUE_SLOT_RESTORE,
                properties.getAppointment().getDlxExchange(),
                RabbitMqTopology.RK_DLQ);
    }

    @Bean
    public Queue appointmentAuditQueue(MessagingProperties properties) {
        return durableQueueWithDlx(
                RabbitMqTopology.QUEUE_AUDIT,
                properties.getAppointment().getDlxExchange(),
                RabbitMqTopology.RK_DLQ);
    }

    @Bean
    public Queue appointmentDlqQueue() {
        return QueueBuilder.durable(RabbitMqTopology.QUEUE_DLQ).build();
    }

    @Bean
    public Binding appointmentNotificationCreatedBinding(Queue appointmentNotificationQueue,
                                                         TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_CREATED);
    }

    @Bean
    public Binding appointmentNotificationCancelledBinding(Queue appointmentNotificationQueue,
                                                           TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_CANCELLED);
    }

    @Bean
    public Binding appointmentNotificationCheckedInBinding(Queue appointmentNotificationQueue,
                                                             TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_CHECKED_IN);
    }

    @Bean
    public Binding appointmentNotificationExpiredBinding(Queue appointmentNotificationQueue,
                                                         TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_EXPIRED);
    }

    @Bean
    public Binding appointmentNotificationReminderBinding(Queue appointmentNotificationQueue,
                                                          TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_REMINDER);
    }

    @Bean
    public Binding appointmentNotificationSettledBinding(Queue appointmentNotificationQueue,
                                                         TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_SETTLED);
    }

    @Bean
    public Binding appointmentNotificationBillUnpaidBinding(Queue appointmentNotificationQueue,
                                                            TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentNotificationQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_BILL_UNPAID);
    }

    @Bean
    public Binding appointmentSlotRestoreExpiredBinding(Queue appointmentSlotRestoreQueue,
                                                        TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentSlotRestoreQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_EXPIRED);
    }

    @Bean
    public Binding appointmentAuditAllBinding(Queue appointmentAuditQueue, TopicExchange appointmentExchange) {
        return BindingBuilder.bind(appointmentAuditQueue)
                .to(appointmentExchange)
                .with(RabbitMqTopology.RK_AUDIT_ALL);
    }

    @Bean
    public Binding appointmentDlqBinding(Queue appointmentDlqQueue, TopicExchange appointmentDlxExchange) {
        return BindingBuilder.bind(appointmentDlqQueue)
                .to(appointmentDlxExchange)
                .with(RabbitMqTopology.RK_DLQ);
    }

    private static Queue durableQueueWithDlx(String queueName, String dlxExchange, String dlqRoutingKey) {
        // 消费 nack(requeue=false) 时消息进入 DLX，最终落到 appointment.dlq 便于排查
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchange);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return QueueBuilder.durable(queueName).withArguments(args).build();
    }
}
