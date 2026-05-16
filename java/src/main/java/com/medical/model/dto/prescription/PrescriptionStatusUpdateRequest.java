package com.medical.model.dto.prescription;

import lombok.Data;

@Data
public class PrescriptionStatusUpdateRequest {

    private Long prescriptionId;

    private Integer status;

    private String remark;
}