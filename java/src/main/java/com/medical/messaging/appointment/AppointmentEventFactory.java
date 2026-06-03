package com.medical.messaging.appointment;

import com.medical.constant.AppointmentConstant;
import com.medical.model.entity.Appointment;
import com.medical.model.vo.BillVO;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 构造 {@link AppointmentEventEnvelope}，统一 eventId 规则与 payload 填充。
 *
 * <p><b>eventId 策略</b>
 * <ul>
 *   <li>创建/取消/签到：{@code TYPE:appointmentId:时间戳} —— 允许同预约多次业务动作各发一条</li>
 *   <li>过期/提醒/结算/待支付：{@code TYPE:appointmentId} 或 {@code TYPE:billId} —— 稳定 id，
 *       配合 Redis 幂等，防止多实例 Scheduler 或重复消费产生 duplicate 副作用</li>
 * </ul>
 */
public final class AppointmentEventFactory {

    private static final DateTimeFormatter EVENT_ID_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private AppointmentEventFactory() {
    }

    public static AppointmentEventEnvelope created(Appointment appointment) {
        return build(appointment, AppointmentEventType.APPOINTMENT_CREATED, null,
                appointment.getStatus(), "API", null);
    }

    public static AppointmentEventEnvelope cancelled(Appointment appointment, Integer previousStatus,
                                                     String cancelReason) {
        Map<String, Object> payload = new HashMap<>();
        if (StringUtils.hasText(cancelReason)) {
            payload.put("cancelReason", cancelReason);
        }
        return build(appointment, AppointmentEventType.APPOINTMENT_CANCELLED, previousStatus,
                appointment.getStatus(), "API", payload);
    }

    public static AppointmentEventEnvelope checkedIn(Appointment appointment, Integer previousStatus) {
        return build(appointment, AppointmentEventType.APPOINTMENT_CHECKED_IN, previousStatus,
                appointment.getStatus(), "API", null);
    }

    /**
     * 稳定 eventId，避免多实例过期任务各发一条导致 slot-restore 重复 +1。
     */
    public static AppointmentEventEnvelope expired(Appointment appointment, Integer previousStatus) {
        String eventId = AppointmentEventType.APPOINTMENT_EXPIRED.name() + ":" + appointment.getId();
        return buildWithEventId(
                appointment,
                AppointmentEventType.APPOINTMENT_EXPIRED,
                previousStatus,
                AppointmentConstant.APPOINTMENT_STATUS_EXPIRED,
                "SCHEDULER",
                null,
                eventId);
    }

    public static AppointmentEventEnvelope reminder(Appointment appointment) {
        return buildWithEventId(
                appointment,
                AppointmentEventType.APPOINTMENT_REMINDER,
                appointment.getStatus(),
                appointment.getStatus(),
                "SCHEDULER",
                null,
                AppointmentEventType.APPOINTMENT_REMINDER.name() + ":" + appointment.getId());
    }

    public static AppointmentEventEnvelope settled(Appointment appointment, Integer previousStatus, String source) {
        return buildWithEventId(
                appointment,
                AppointmentEventType.APPOINTMENT_SETTLED,
                previousStatus,
                appointment.getStatus(),
                source,
                null,
                AppointmentEventType.APPOINTMENT_SETTLED.name() + ":" + appointment.getId());
    }

    public static AppointmentEventEnvelope billUnpaid(Appointment appointment, BillVO bill) {
        Map<String, Object> payload = new HashMap<>();
        if (bill != null) {
            payload.put("billId", bill.getId());
            payload.put("billNo", bill.getBillNo());
            if (bill.getSelfPayAmount() != null) {
                payload.put("selfPayAmount", bill.getSelfPayAmount());
            }
        }
        String eventId = AppointmentEventType.APPOINTMENT_BILL_UNPAID.name() + ":"
                + (bill != null && bill.getId() != null ? bill.getId() : appointment.getId());
        return buildWithEventId(
                appointment,
                AppointmentEventType.APPOINTMENT_BILL_UNPAID,
                appointment.getStatus(),
                appointment.getStatus(),
                "BILL",
                payload,
                eventId);
    }

    private static AppointmentEventEnvelope build(Appointment appointment,
                                                  AppointmentEventType eventType,
                                                  Integer previousStatus,
                                                  Integer currentStatus,
                                                  String source,
                                                  Map<String, Object> payload) {
        String eventId = eventType.name() + ":" + appointment.getId() + ":"
                + EVENT_ID_TIME.format(LocalDateTime.now());
        return buildWithEventId(appointment, eventType, previousStatus, currentStatus, source, payload, eventId);
    }

    private static AppointmentEventEnvelope buildWithEventId(Appointment appointment,
                                                  AppointmentEventType eventType,
                                                  Integer previousStatus,
                                                  Integer currentStatus,
                                                  String source,
                                                  Map<String, Object> payload,
                                                  String eventId) {
        return AppointmentEventEnvelope.builder()
                .eventId(eventId)
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .appointmentId(appointment.getId())
                .appointmentNo(appointment.getAppointmentNo())
                .userId(appointment.getUserId())
                .doctorId(appointment.getDoctorId())
                .department(appointment.getDepartment())
                .scheduleId(appointment.getScheduleId())
                .scheduleDate(appointment.getScheduleDate())
                .timeSlot(appointment.getTimeSlot())
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .source(source)
                .payload(payload)
                .build();
    }
}
