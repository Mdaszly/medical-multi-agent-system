package com.medical.messaging.appointment;

import com.medical.messaging.config.MessagingProperties;
import com.medical.model.entity.Appointment;
import com.medical.model.vo.BillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 预约领域事件门面：业务层只表达「发生了什么」，不直接操作 RabbitMQ。
 *
 * <p>此处发布的是 Spring {@link org.springframework.context.ApplicationEvent}，
 * 真正投递 MQ 由 {@link AppointmentEventAfterCommitListener} 在<strong>数据库事务提交后</strong>完成，
 * 避免「事务回滚但消息已发出」。
 *
 * <p>调用方：{@code AppointmentServiceImpl}、{@code BillServiceImpl}、
 * {@code PrescriptionServiceImpl}、过期/提醒定时任务等。
 */
@Component
@RequiredArgsConstructor
public class AppointmentEventBridge {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final MessagingProperties messagingProperties;

    public void publishCreated(Appointment appointment) {
        publish(AppointmentEventFactory.created(appointment));
    }

    public void publishCancelled(Appointment appointment, Integer previousStatus, String cancelReason) {
        publish(AppointmentEventFactory.cancelled(appointment, previousStatus, cancelReason));
    }

    public void publishCheckedIn(Appointment appointment, Integer previousStatus) {
        publish(AppointmentEventFactory.checkedIn(appointment, previousStatus));
    }

    public void publishExpired(Appointment appointment, Integer previousStatus) {
        publish(AppointmentEventFactory.expired(appointment, previousStatus));
    }

    public void publishReminder(Appointment appointment) {
        publish(AppointmentEventFactory.reminder(appointment));
    }

    public void publishSettled(Appointment appointment, Integer previousStatus, String source) {
        publish(AppointmentEventFactory.settled(appointment, previousStatus, source));
    }

    public void publishBillUnpaid(Appointment appointment, BillVO bill) {
        publish(AppointmentEventFactory.billUnpaid(appointment, bill));
    }

    /**
     * messaging 关闭时静默跳过（NoOp 由 AutoConfiguration 注入到 Listener 下游）。
     */
    private void publish(AppointmentEventEnvelope envelope) {
        if (!messagingProperties.isEnabled() || envelope == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new AppointmentDomainEvent(envelope));
    }
}
