package com.medical.model.dto.appointment;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentAddRequest {

    private Long scheduleId;

    private String timeSlot;

    private String remark;
}
