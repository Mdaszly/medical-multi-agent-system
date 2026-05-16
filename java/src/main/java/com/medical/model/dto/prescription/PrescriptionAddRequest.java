package com.medical.model.dto.prescription;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PrescriptionAddRequest {

    private Long appointmentId;

    private String diagnosis;

    private String remark;

    private List<PrescriptionDrugItem> drugs;

    @Data
    public static class PrescriptionDrugItem {
        private String drugCode;
        private String drugName;
        private String specification;
        private String dosage;
        private String usage;
        private String frequency;
        private String duration;
        private BigDecimal quantity;
    }
}