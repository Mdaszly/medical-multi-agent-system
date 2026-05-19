package com.medical.model.vo;

import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import lombok.Data;

@Data
public class SymptomDiagnosisRowVO {

    private String symptom;
    private String disease;
    private String diseaseCode;
    private String icdCode;
    private String icdDescription;
    private Double weight;

    public static SymptomDiagnosisRowVO from(SymptomDiagnosisRow row) {
        SymptomDiagnosisRowVO vo = new SymptomDiagnosisRowVO();
        vo.setSymptom(row.getSymptom());
        vo.setDisease(row.getDisease());
        vo.setDiseaseCode(row.getDiseaseCode());
        vo.setIcdCode(row.getIcdCode());
        vo.setIcdDescription(row.getIcdDescription());
        vo.setWeight(row.getWeight());
        return vo;
    }
}
