package com.medical.messaging.appointment;

/**
 * 预约领域事件 MQ 发布接口。
 *
 * <p>实现：
 * <ul>
 *   <li>{@link RabbitAppointmentEventPublisher} — messaging 开启</li>
 *   <li>{@link NoOpAppointmentEventPublisher} — messaging 关闭</li>
 * </ul>
 * 由 {@link AppointmentEventAfterCommitListener} 在事务提交后调用，业务 Service 请使用 {@link AppointmentEventBridge}。
 */
public interface AppointmentEventPublisher {

    void publish(AppointmentEventEnvelope envelope);
}
