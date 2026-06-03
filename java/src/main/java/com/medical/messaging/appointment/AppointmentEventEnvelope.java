package com.medical.messaging.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 预约领域事件统一信封（JSON 序列化后经 RabbitMQ 投递）。
 *
 * <p>字段尽量冗余预约快照（科室、时段、前后状态），Consumer 无需再查库即可写通知/审计；
 * 号源回补等仍会以 DB 当前状态为准做二次校验。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEventEnvelope {

    /** 全局幂等键，规则见 {@link AppointmentEventFactory} */
    private String eventId;
    private AppointmentEventType eventType;
    private LocalDateTime occurredAt;
    private Long appointmentId;
    private String appointmentNo;
    private Long userId;
    private Long doctorId;
    private String department;
    private Long scheduleId;
    private LocalDate scheduleDate;
    private String timeSlot;
    private Integer previousStatus;
    private Integer currentStatus;
    private String source;
    private Map<String, Object> payload;
}
