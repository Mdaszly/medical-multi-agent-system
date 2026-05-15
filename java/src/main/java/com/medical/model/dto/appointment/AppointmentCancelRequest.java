package com.medical.model.dto.appointment;

import lombok.Data;

@Data
public class AppointmentCancelRequest {

    private Long appointmentId;

    private String cancelReason;
}
