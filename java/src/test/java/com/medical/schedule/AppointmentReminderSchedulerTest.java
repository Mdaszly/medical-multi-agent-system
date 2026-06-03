package com.medical.schedule;

import com.medical.constant.AppointmentConstant;
import com.medical.model.entity.Appointment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentReminderSchedulerTest {

    @Test
    void shouldSendReminder_whenWithinWindow() {
        Appointment appointment = new Appointment();
        appointment.setScheduleDate(LocalDate.of(2026, 6, 3));
        appointment.setTimeSlot("14:00-14:30");

        LocalDateTime now = LocalDateTime.of(2026, 6, 3, 12, 30);
        assertTrue(AppointmentReminderScheduler.shouldSendReminder(appointment, now, 2));
    }

    @Test
    void shouldNotSendReminder_whenTooEarly() {
        Appointment appointment = new Appointment();
        appointment.setScheduleDate(LocalDate.of(2026, 6, 3));
        appointment.setTimeSlot("14:00-14:30");

        LocalDateTime now = LocalDateTime.of(2026, 6, 3, 11, 0);
        assertFalse(AppointmentReminderScheduler.shouldSendReminder(appointment, now, 2));
    }

    @Test
    void shouldNotSendReminder_whenAppointmentAlreadyStarted() {
        Appointment appointment = new Appointment();
        appointment.setScheduleDate(LocalDate.of(2026, 6, 3));
        appointment.setTimeSlot("14:00-14:30");

        LocalDateTime now = LocalDateTime.of(2026, 6, 3, 14, 5);
        assertFalse(AppointmentReminderScheduler.shouldSendReminder(appointment, now, 2));
    }

    @Test
    void parseAppointmentStart_parsesTimeSlotPrefix() {
        Appointment appointment = new Appointment();
        appointment.setScheduleDate(LocalDate.of(2026, 6, 3));
        appointment.setTimeSlot("09:30-10:00");
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);

        LocalDateTime start = AppointmentReminderScheduler.parseAppointmentStart(appointment);
        assertTrue(start != null && start.getHour() == 9 && start.getMinute() == 30);
    }
}
