package com.medical.model.dto.drug;

import lombok.Data;

@Data
public class DrugUpdateRequest {

    private Long id;

    private String drugName;

    private String genericName;

    private String tradeName;

    private String specification;

    private String unit;

    private String manufacturer;

    private String category;

    private String categoryCode;

    private String dosageForm;

    private Boolean prescriptionFlag;

    private Integer status;

    private String remark;
}