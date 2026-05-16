package com.medical.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DrugWithPriceVO {

    private Long id;

    private String drugCode;

    private String drugName;

    private String specification;

    private String unit;

    private BigDecimal retailPrice;

    private BigDecimal insurancePrice;
}