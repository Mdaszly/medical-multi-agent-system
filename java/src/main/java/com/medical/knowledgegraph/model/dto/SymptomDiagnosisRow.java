package com.medical.knowledgegraph.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 症状-疾病-ICD 表格化查询结果行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomDiagnosisRow {

    private String symptom;
    private String disease;
    private String diseaseCode;
    private String icdCode;
    private String icdDescription;
    private Double weight;
    private Integer priority;

}
