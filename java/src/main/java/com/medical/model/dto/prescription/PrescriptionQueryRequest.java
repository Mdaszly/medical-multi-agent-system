package com.medical.model.dto.prescription;

import lombok.Data;

@Data
public class PrescriptionQueryRequest {

    private Long id;

    private String prescriptionNo;

    private Long userId;

    private Long doctorId;

    private String department;

    private Integer status;

    private Long appointmentId;

    private Integer current;

    private Integer pageSize;
}