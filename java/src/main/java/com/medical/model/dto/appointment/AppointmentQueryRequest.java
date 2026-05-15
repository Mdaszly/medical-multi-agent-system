package com.medical.model.dto.appointment;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentQueryRequest {

    private Long userId;

    private Long doctorId;

    private String department;

    private LocalDate scheduleDate;

    private Integer status;

    private Long current;

    private Long pageSize;
}
