package com.medical.messaging.appointment;

import com.medical.messaging.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 将 {@link AppointmentEventEnvelope} 投递到 Topic 交换机，routing key 取自 {@link AppointmentEventType}。
 *
 * <p>correlationData.id = eventId，配合 RabbitTemplate confirm/return 回调便于排查不可路由或 broker 未 ack 的发送。
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitAppointmentEventPublisher implements AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    @Override
    public void publish(AppointmentEventEnvelope envelope) {
        if (envelope == null || envelope.getEventType() == null) {
            return;
        }
        String exchange = messagingProperties.getAppointment().getExchange();
        String routingKey = envelope.getEventType().getRoutingKey();
        CorrelationData correlationData = new CorrelationData(envelope.getEventId());
        rabbitTemplate.convertAndSend(exchange, routingKey, envelope, correlationData);
        log.debug("Published appointment event: type={}, appointmentId={}, eventId={}",
                envelope.getEventType(), envelope.getAppointmentId(), envelope.getEventId());
    }
}
