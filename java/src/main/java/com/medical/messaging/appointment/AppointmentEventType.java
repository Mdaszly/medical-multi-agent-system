package com.medical.messaging.appointment;

import com.medical.messaging.config.RabbitMqTopology;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预约领域事件类型，与 Topic routing key 一一对应。
 *
 * <p>发布方概览：
 * <ul>
 *   <li>API：CREATED / CANCELLED / CHECKED_IN</li>
 *   <li>Scheduler：EXPIRED / REMINDER</li>
 *   <li>Bill：SETTLED / BILL_UNPAID</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AppointmentEventType {

    APPOINTMENT_CREATED(RabbitMqTopology.RK_CREATED),
    APPOINTMENT_CANCELLED(RabbitMqTopology.RK_CANCELLED),
    APPOINTMENT_CHECKED_IN(RabbitMqTopology.RK_CHECKED_IN),
    APPOINTMENT_EXPIRED(RabbitMqTopology.RK_EXPIRED),
    APPOINTMENT_REMINDER(RabbitMqTopology.RK_REMINDER),
    APPOINTMENT_SETTLED(RabbitMqTopology.RK_SETTLED),
    APPOINTMENT_BILL_UNPAID(RabbitMqTopology.RK_BILL_UNPAID);

    private final String routingKey;
}
