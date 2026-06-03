package com.medical.messaging.appointment.consumer;

import com.medical.common.RedisCacheUtil;
import com.medical.constant.RedisKeyConstant;
import com.medical.mapper.DoctorMapper;
import com.medical.messaging.appointment.AppointmentEventEnvelope;
import com.medical.messaging.appointment.AppointmentEventType;
import com.medical.messaging.config.RabbitMqTopology;
import com.medical.messaging.support.AppointmentMessageAckHelper;
import com.medical.messaging.support.IdempotentConsumerExecutor;
import com.medical.messaging.support.IdempotentMessageHandler;
import com.medical.model.entity.Doctor;
import com.medical.service.UserNotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 消费 {@link com.medical.messaging.config.RabbitMqTopology#QUEUE_NOTIFICATION}：
 * 将 7 类预约事件转为 {@code user_notification} 站内信。
 *
 * <p>CHECKED_IN 会同时通知患者与医生（候诊），整段在 {@link TransactionTemplate} 内执行保证原子性。
 * REMINDER 成功写库后写入 {@link com.medical.constant.RedisKeyConstant#MQ_REMINDER_SENT}，与 Scheduler 侧 hasKey 配合降噪。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
public class AppointmentNotificationConsumer {

    private final UserNotificationService userNotificationService;
    private final DoctorMapper doctorMapper;
    private final IdempotentMessageHandler idempotentMessageHandler;
    private final TransactionTemplate transactionTemplate;
    private final RedisCacheUtil redisCacheUtil;

    @RabbitListener(queues = RabbitMqTopology.QUEUE_NOTIFICATION)
    public void handle(AppointmentEventEnvelope envelope, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (envelope == null || envelope.getEventId() == null) {
            AppointmentMessageAckHelper.ack(channel, deliveryTag);
            return;
        }
        try {
            IdempotentConsumerExecutor.execute(
                    idempotentMessageHandler,
                    envelope.getEventId(),
                    () -> transactionTemplate.executeWithoutResult(status -> dispatchNotifications(envelope)),
                    channel,
                    deliveryTag);
        } catch (Exception e) {
            log.error("Notification consumer failed: eventId={}, error={}",
                    envelope.getEventId(), e.getMessage(), e);
            AppointmentMessageAckHelper.nackToDlq(channel, deliveryTag);
        }
    }

    private void dispatchNotifications(AppointmentEventEnvelope envelope) {
        if (envelope.getEventType() == null) {
            return;
        }
        String detail = formatAppointmentDetail(envelope);
        AppointmentEventType type = envelope.getEventType();

        switch (type) {
            case APPOINTMENT_CREATED -> userNotificationService.saveAppointmentNotification(
                    envelope.getUserId(),
                    "预约成功",
                    "您已成功预约 " + detail,
                    envelope.getAppointmentId(),
                    type.name());
            case APPOINTMENT_CANCELLED -> userNotificationService.saveAppointmentNotification(
                    envelope.getUserId(),
                    "预约已取消",
                    "您的预约已取消：" + detail,
                    envelope.getAppointmentId(),
                    type.name());
            case APPOINTMENT_CHECKED_IN -> {
                userNotificationService.saveAppointmentNotification(
                        envelope.getUserId(),
                        "签到成功",
                        "您已完成签到：" + detail,
                        envelope.getAppointmentId(),
                        type.name());
                notifyDoctorCheckedIn(envelope, detail);
            }
            case APPOINTMENT_EXPIRED -> userNotificationService.saveAppointmentNotification(
                    envelope.getUserId(),
                    "预约已过期",
                    "您的预约已过期（未到院就诊）：" + detail,
                    envelope.getAppointmentId(),
                    type.name());
            case APPOINTMENT_REMINDER -> {
                userNotificationService.saveAppointmentNotification(
                        envelope.getUserId(),
                        "就诊提醒",
                        "您有预约即将开始，请按时到院：" + detail,
                        envelope.getAppointmentId(),
                        type.name());
                // 通知落库成功后再标记，避免 Scheduler 侧 hasKey 与 MQ 发送顺序不一致
                markReminderSent(envelope.getAppointmentId());
            }
            case APPOINTMENT_SETTLED -> userNotificationService.saveAppointmentNotification(
                    envelope.getUserId(),
                    "就诊完成",
                    "本次就诊已结算完成，感谢就医：" + detail,
                    envelope.getAppointmentId(),
                    type.name());
            case APPOINTMENT_BILL_UNPAID -> userNotificationService.saveBillNotification(
                    envelope.getUserId(),
                    "待支付账单",
                    formatBillUnpaidContent(envelope, detail),
                    extractBillId(envelope),
                    envelope.getAppointmentId(),
                    type.name());
            default -> log.debug("Skip notification for event type: {}", type);
        }
    }

    private void markReminderSent(Long appointmentId) {
        if (appointmentId == null) {
            return;
        }
        String key = String.format(RedisKeyConstant.MQ_REMINDER_SENT, appointmentId);
        redisCacheUtil.setIfAbsent(key, "1", RedisKeyConstant.MQ_REMINDER_SENT_TTL);
    }

    private void notifyDoctorCheckedIn(AppointmentEventEnvelope envelope, String detail) {
        if (envelope.getDoctorId() == null) {
            return;
        }
        Doctor doctor = doctorMapper.selectById(envelope.getDoctorId());
        if (doctor == null || doctor.getUserId() == null) {
            return;
        }
        userNotificationService.saveAppointmentNotification(
                doctor.getUserId(),
                "新患者候诊",
                "患者已到院签到，请准备接诊：" + detail,
                envelope.getAppointmentId(),
                envelope.getEventType().name());
    }

    private static String formatAppointmentDetail(AppointmentEventEnvelope envelope) {
        StringBuilder sb = new StringBuilder();
        if (envelope.getDepartment() != null) {
            sb.append(envelope.getDepartment()).append(" ");
        }
        if (envelope.getScheduleDate() != null) {
            sb.append(envelope.getScheduleDate()).append(" ");
        }
        if (envelope.getTimeSlot() != null) {
            sb.append(envelope.getTimeSlot());
        }
        return sb.toString().trim();
    }

    private static String formatBillUnpaidContent(AppointmentEventEnvelope envelope, String detail) {
        BigDecimal amount = extractSelfPayAmount(envelope);
        String amountText = amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "—";
        return "您有一笔 ¥" + amountText + " 待支付，请前往我的账单完成缴费：" + detail;
    }

    private static BigDecimal extractSelfPayAmount(AppointmentEventEnvelope envelope) {
        Map<String, Object> payload = envelope.getPayload();
        if (payload == null) {
            return null;
        }
        Object value = payload.get("selfPayAmount");
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private static Long extractBillId(AppointmentEventEnvelope envelope) {
        Map<String, Object> payload = envelope.getPayload();
        if (payload == null) {
            return null;
        }
        Object value = payload.get("billId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
