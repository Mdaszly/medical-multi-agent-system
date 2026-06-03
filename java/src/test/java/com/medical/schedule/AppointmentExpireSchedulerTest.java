package com.medical.schedule;

import com.medical.constant.AppointmentConstant;
import com.medical.mapper.AppointmentMapper;
import com.medical.messaging.appointment.AppointmentEventBridge;
import com.medical.model.entity.Appointment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentExpireSchedulerTest {

    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentEventBridge appointmentEventBridge;

    @InjectMocks
    private AppointmentExpireScheduler scheduler;

    @Test
    void expirePastPendingAppointments_publishesExpiredEventWhenCasSucceeds() {
        Appointment appointment = new Appointment();
        appointment.setId(6L);
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
        appointment.setScheduleDate(LocalDate.now().minusDays(1));
        appointment.setScheduleId(1L);
        appointment.setTimeSlot("14:00-14:30");

        when(appointmentMapper.selectList(any())).thenReturn(List.of(appointment));
        when(appointmentMapper.updateStatusIf(
                eq(6L),
                eq(AppointmentConstant.APPOINTMENT_STATUS_PENDING),
                eq(AppointmentConstant.APPOINTMENT_STATUS_EXPIRED))).thenReturn(1);

        scheduler.expirePastPendingAppointments();

        verify(appointmentEventBridge).publishExpired(any(Appointment.class),
                eq(AppointmentConstant.APPOINTMENT_STATUS_PENDING));
    }

    @Test
    void expirePastPendingAppointments_skipsPublishWhenCasFails() {
        Appointment appointment = new Appointment();
        appointment.setId(6L);
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
        appointment.setScheduleDate(LocalDate.now().minusDays(1));

        when(appointmentMapper.selectList(any())).thenReturn(List.of(appointment));
        when(appointmentMapper.updateStatusIf(any(), any(), any())).thenReturn(0);

        scheduler.expirePastPendingAppointments();

        verify(appointmentEventBridge, never()).publishExpired(any(), any());
    }

    @Test
    void expirePastPendingAppointments_skipsWhenEmpty() {
        when(appointmentMapper.selectList(any())).thenReturn(List.of());

        scheduler.expirePastPendingAppointments();

        verify(appointmentEventBridge, never()).publishExpired(any(), any());
    }
}
