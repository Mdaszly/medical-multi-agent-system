package com.medical.messaging.appointment.consumer;

import com.medical.constant.AppointmentConstant;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.AppointmentSlotMapper;
import com.medical.messaging.appointment.AppointmentEventEnvelope;
import com.medical.messaging.config.RabbitMqTopology;
import com.medical.messaging.support.AppointmentMessageAckHelper;
import com.medical.messaging.support.IdempotentConsumerExecutor;
import com.medical.messaging.support.IdempotentMessageHandler;
import com.medical.model.entity.Appointment;
import com.medical.model.entity.AppointmentSlot;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 消费 {@link com.medical.messaging.config.RabbitMqTopology#QUEUE_SLOT_RESTORE}（仅 routing key expired）。
 *
 * <p>与用户取消预约时 Service 内同步 {@code increaseAvailableSlots} 不同，过期回补 intentionally 异步，
 * 减轻 {@link com.medical.schedule.AppointmentExpireScheduler} 批量事务耗时。
 * 消费前会校验预约仍为「已过期」，防止重复消息误加号源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
public class AppointmentSlotRestoreConsumer {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentSlotMapper appointmentSlotMapper;
    private final IdempotentMessageHandler idempotentMessageHandler;

    @RabbitListener(queues = RabbitMqTopology.QUEUE_SLOT_RESTORE)
    public void handle(AppointmentEventEnvelope envelope, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (envelope == null || envelope.getEventId() == null) {
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
            return;
        }
        String idempotencyKey = "slot:" + envelope.getEventId();
        try {
            IdempotentConsumerExecutor.execute(
                    idempotentMessageHandler,
                    idempotencyKey,
                    () -> restoreSlotIfExpired(envelope),
                    channel,
                    deliveryTag);
        } catch (Exception e) {
            log.error("Slot restore consumer failed: eventId={}, error={}",
                    envelope.getEventId(), e.getMessage(), e);
            AppointmentMessageAckHelper.nackToDlq(channel, deliveryTag);
        }
    }

    private void restoreSlotIfExpired(AppointmentEventEnvelope envelope) {
        if (envelope.getAppointmentId() == null) {
            return;
        }
        Appointment appointment = appointmentMapper.selectById(envelope.getAppointmentId());
        if (appointment == null
                || !AppointmentConstant.APPOINTMENT_STATUS_EXPIRED.equals(appointment.getStatus())) {
            log.debug("Skip slot restore, appointment not expired: appointmentId={}", envelope.getAppointmentId());
            return;
        }
        if (envelope.getScheduleId() == null || envelope.getTimeSlot() == null) {
            throw new IllegalStateException("Missing scheduleId/timeSlot for slot restore");
        }
        AppointmentSlot slot = appointmentSlotMapper.selectByScheduleId(envelope.getScheduleId()).stream()
                .filter(s -> envelope.getTimeSlot().equals(s.getTimeSlot()))
                .findFirst()
                .orElse(null);
        if (slot == null) {
            throw new IllegalStateException("Slot not found for restore: scheduleId=" + envelope.getScheduleId()
                    + ", timeSlot=" + envelope.getTimeSlot());
        }
        int updated = appointmentSlotMapper.increaseAvailableSlots(slot.getId());
        if (updated > 0) {
            log.info("Restored slot after expire: appointmentId={}, slotId={}",
                    envelope.getAppointmentId(), slot.getId());
        } else {
            log.debug("Slot already at max capacity, skip restore: appointmentId={}, slotId={}",
                    envelope.getAppointmentId(), slot.getId());
        }
    }
}
