package com.medical.model.dto.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleAddRequest {

    private Long doctorId;

    private String doctorName;

    private String department;

    private LocalDate scheduleDate;

    private String shiftType;

    private Integer maxAppointments;

    private String description;
}
