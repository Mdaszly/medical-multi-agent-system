package com.medical.messaging.appointment;

import com.medical.constant.AppointmentConstant;
import com.medical.model.entity.Appointment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentEventFactoryTest {

    @Test
    void created_buildsEnvelopeWithRoutingMetadata() {
        Appointment appointment = baseAppointment();
        AppointmentEventEnvelope envelope = AppointmentEventFactory.created(appointment);

        assertEquals(AppointmentEventType.APPOINTMENT_CREATED, envelope.getEventType());
        assertEquals(appointment.getId(), envelope.getAppointmentId());
        assertEquals(AppointmentConstant.APPOINTMENT_STATUS_PENDING, envelope.getCurrentStatus());
        assertNotNull(envelope.getEventId());
        assertTrue(envelope.getEventId().startsWith("APPOINTMENT_CREATED:"));
        assertEquals("心内科", envelope.getDepartment());
    }

    @Test
    void settled_usesStableEventIdForIdempotency() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_SETTLED);

        AppointmentEventEnvelope envelope = AppointmentEventFactory.settled(
                appointment, AppointmentConstant.APPOINTMENT_STATUS_IN_CONSULTATION, "BILL_PAID");

        assertEquals(AppointmentEventType.APPOINTMENT_SETTLED, envelope.getEventType());
        assertEquals("APPOINTMENT_SETTLED:10", envelope.getEventId());
    }

    @Test
    void reminder_usesStableEventIdPerAppointment() {
        Appointment appointment = baseAppointment();
        AppointmentEventEnvelope envelope = AppointmentEventFactory.reminder(appointment);

        assertEquals(AppointmentEventType.APPOINTMENT_REMINDER, envelope.getEventType());
        assertEquals("APPOINTMENT_REMINDER:10", envelope.getEventId());
    }

    @Test
    void expired_usesStableEventIdPerAppointment() {
        Appointment appointment = baseAppointment();

        AppointmentEventEnvelope envelope = AppointmentEventFactory.expired(
                appointment, AppointmentConstant.APPOINTMENT_STATUS_PENDING);

        assertEquals(AppointmentEventType.APPOINTMENT_EXPIRED, envelope.getEventType());
        assertEquals("APPOINTMENT_EXPIRED:10", envelope.getEventId());
        assertEquals(AppointmentConstant.APPOINTMENT_STATUS_EXPIRED, envelope.getCurrentStatus());
    }

    @Test
    void billUnpaid_usesStableEventIdPerBill() {
        Appointment appointment = baseAppointment();
        com.medical.model.vo.BillVO bill = com.medical.model.vo.BillVO.builder()
                .id(99L)
                .billNo("BILL20260101000000")
                .selfPayAmount(new java.math.BigDecimal("75.00"))
                .status("UNPAID")
                .build();

        AppointmentEventEnvelope envelope = AppointmentEventFactory.billUnpaid(appointment, bill);

        assertEquals(AppointmentEventType.APPOINTMENT_BILL_UNPAID, envelope.getEventType());
        assertEquals("APPOINTMENT_BILL_UNPAID:99", envelope.getEventId());
        assertEquals(99L, envelope.getPayload().get("billId"));
    }

    private static Appointment baseAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setAppointmentNo("APT20260101000000ABCD");
        appointment.setUserId(6L);
        appointment.setDoctorId(1L);
        appointment.setDepartment("心内科");
        appointment.setScheduleId(2L);
        appointment.setScheduleDate(LocalDate.now().plusDays(1));
        appointment.setTimeSlot("08:00-08:30");
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
        return appointment;
    }
}
