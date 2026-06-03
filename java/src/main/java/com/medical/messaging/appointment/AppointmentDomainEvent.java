package com.medical.messaging.appointment;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring 应用内领域事件载体，供 {@link AppointmentEventAfterCommitListener} 在事务提交后转发 MQ。
 */
@Getter
public class AppointmentDomainEvent extends ApplicationEvent {

    private final AppointmentEventEnvelope envelope;

    public AppointmentDomainEvent(AppointmentEventEnvelope envelope) {
        super(envelope);
        this.envelope = envelope;
    }
}
