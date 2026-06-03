package com.medical.messaging.appointment;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code medical.messaging.enabled=false} 时的空实现，避免单测/本地无 RabbitMQ 时强依赖 Broker。
 */
@Slf4j
public class NoOpAppointmentEventPublisher implements AppointmentEventPublisher {

    @Override
    public void publish(AppointmentEventEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        log.debug("Messaging disabled, skip appointment event: type={}, appointmentId={}",
                envelope.getEventType(), envelope.getAppointmentId());
    }
}
