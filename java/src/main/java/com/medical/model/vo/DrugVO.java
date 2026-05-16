package com.medical.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DrugVO {

    private Long id;

    private String drugCode;

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

    private LocalDateTime createTime;

    private String remark;
}