package com.medical.model.vo;

import lombok.Data;

@Data
public class Icd10CodeVO {

    private String code;
    private String description;
    private String category;
}
