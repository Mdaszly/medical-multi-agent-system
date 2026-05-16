package com.medical.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DrugPriceVO {

    private Long id;

    private Long drugId;

    private String priceType;

    private BigDecimal price;

    private LocalDate effectiveDate;

    private LocalDate expireDate;
}