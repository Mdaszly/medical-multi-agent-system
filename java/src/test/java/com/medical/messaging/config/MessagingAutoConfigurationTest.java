package com.medical.messaging.config;

import com.medical.messaging.appointment.AppointmentEventPublisher;
import com.medical.messaging.appointment.NoOpAppointmentEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MessagingAutoConfiguration.class)
@EnableConfigurationProperties(MessagingProperties.class)
@TestPropertySource(properties = "medical.messaging.enabled=false")
class MessagingAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AppointmentEventPublisher appointmentEventPublisher;

    @Autowired
    private MessagingProperties messagingProperties;

    @Test
    void messagingDisabled_usesNoOpPublisher() {
        assertFalse(messagingProperties.isEnabled());
        assertInstanceOf(NoOpAppointmentEventPublisher.class, appointmentEventPublisher);
        assertFalse(applicationContext.containsBean("rabbitTemplate"));
    }
}
