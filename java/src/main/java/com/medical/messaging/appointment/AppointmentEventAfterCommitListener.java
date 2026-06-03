package com.medical.messaging.appointment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 将 Spring 领域事件转为 RabbitMQ 消息，且仅在<strong>外围事务 commit 成功</strong>后执行。
 *
 * <p>注意：调用 {@link AppointmentEventBridge} 的方法必须处于活跃事务中
 *（{@code @Transactional}），否则 {@code AFTER_COMMIT} 监听器默认不会触发。
 * 补偿逻辑见 {@link com.medical.service.impl.AppointmentServiceImpl#reconcileAppointmentData}。
 *
 * <p>局限：非 Transactional Outbox；若 commit 后 {@code rabbitTemplate.send} 失败，仅记录日志，
 * 消息可能丢失——生产可演进为 Outbox 表或发送失败重试队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.messaging", name = "enabled", havingValue = "true")
public class AppointmentEventAfterCommitListener {

    private final AppointmentEventPublisher appointmentEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentDomainEvent(AppointmentDomainEvent event) {
        if (event == null || event.getEnvelope() == null) {
            return;
        }
        try {
            appointmentEventPublisher.publish(event.getEnvelope());
        } catch (Exception e) {
            log.error("Failed to publish appointment event after commit: eventId={}, error={}",
                    event.getEnvelope().getEventId(), e.getMessage(), e);
        }
    }
}
