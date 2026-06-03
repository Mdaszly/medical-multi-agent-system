package com.medical.messaging.appointment.consumer;

import cn.hutool.json.JSONUtil;
import com.medical.mapper.AppointmentEventAuditMapper;
import com.medical.messaging.appointment.AppointmentEventEnvelope;
import com.medical.messaging.config.MessagingProperties;
import com.medical.messaging.config.RabbitMqTopology;
import com.medical.messaging.support.AppointmentMessageAckHelper;
import com.medical.messaging.support.IdempotentConsumerExecutor;
import com.medical.messaging.support.IdempotentMessageHandler;
import com.medical.model.entity.AppointmentEventAudit;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消费 {@link com.medical.messaging.config.RabbitMqTopology#QUEUE_AUDIT}（绑定 {@code appointment.#}）。
 *
 * <p>表 {@code appointment_event_audit.event_id} 有唯一索引；幂等键使用 {@code audit:} 前缀与 notification 区分。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
public class AppointmentAuditConsumer {

    private final AppointmentEventAuditMapper appointmentEventAuditMapper;
    private final MessagingProperties messagingProperties;
    private final IdempotentMessageHandler idempotentMessageHandler;

    @RabbitListener(queues = RabbitMqTopology.QUEUE_AUDIT)
    public void handle(AppointmentEventEnvelope envelope, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (!messagingProperties.getAppointment().isAuditEnabled()) {
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
            return;
        }
        if (envelope == null || envelope.getEventId() == null) {
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
            return;
        }
        String idempotencyKey = "audit:" + envelope.getEventId();
        try {
            IdempotentConsumerExecutor.execute(
                    idempotentMessageHandler,
                    idempotencyKey,
                    () -> persistAudit(envelope),
                    channel,
                    deliveryTag);
        } catch (Exception e) {
            log.error("Audit consumer failed: eventId={}, error={}",
                    envelope.getEventId(), e.getMessage(), e);
            AppointmentMessageAckHelper.nackToDlq(channel, deliveryTag);
        }
    }

    private void persistAudit(AppointmentEventEnvelope envelope) {
        AppointmentEventAudit audit = new AppointmentEventAudit();
        audit.setEventId(envelope.getEventId());
        audit.setEventType(envelope.getEventType() != null ? envelope.getEventType().name() : null);
        audit.setAppointmentId(envelope.getAppointmentId());
        audit.setUserId(envelope.getUserId());
        audit.setDoctorId(envelope.getDoctorId());
        audit.setPreviousStatus(envelope.getPreviousStatus());
        audit.setCurrentStatus(envelope.getCurrentStatus());
        audit.setSource(envelope.getSource());
        if (envelope.getPayload() != null && !envelope.getPayload().isEmpty()) {
            audit.setPayloadJson(JSONUtil.toJsonStr(envelope.getPayload()));
        }
        audit.setCreateTime(LocalDateTime.now());
        appointmentEventAuditMapper.insert(audit);
        log.debug("Appointment event audited: eventId={}, type={}", envelope.getEventId(), audit.getEventType());
    }
}
