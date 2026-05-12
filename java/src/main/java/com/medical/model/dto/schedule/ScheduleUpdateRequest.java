package com.medical.model.dto.schedule;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleUpdateRequest {

    private Long id;

    private Long doctorId;

    private String doctorName;

    private String department;

    private LocalDate scheduleDate;

    private String shiftType;

    private Integer maxAppointments;

    private Integer status;

    private String description;
}
