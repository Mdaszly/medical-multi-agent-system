package com.medical.model.dto.drug;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PriceAddRequest {

    private Long drugId;

    private String priceType;

    private BigDecimal price;

    private LocalDate effectiveDate;
}