package com.medical.model.dto.drug;

import lombok.Data;

@Data
public class DrugQueryRequest {

    private String drugCode;

    private String drugName;

    private String categoryCode;

    private Integer status;

    private Integer current;

    private Integer pageSize;
}