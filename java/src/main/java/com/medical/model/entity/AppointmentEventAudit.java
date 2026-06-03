package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_event_audit")
public class AppointmentEventAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;

    private String eventType;

    private Long appointmentId;

    private Long userId;

    private Long doctorId;

    private Integer previousStatus;

    private Integer currentStatus;

    private String source;

    private String payloadJson;

    private LocalDateTime createTime;
}
