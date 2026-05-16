package com.medical.model.dto.drug;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeCalculationRequest {

    private String drugCode;

    private String drugName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;
}