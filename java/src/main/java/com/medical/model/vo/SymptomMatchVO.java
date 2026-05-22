package com.medical.model.vo;

import com.medical.service.kg.symptom.SymptomMatch;
import lombok.Data;

@Data
public class SymptomMatchVO {

    private String userPhrase;
    private String canonicalName;
    private String symptomCode;
    private Double confidence;
    private String method;
    private String rationale;

    public static SymptomMatchVO from(SymptomMatch match) {
        if (match == null) {
            return null;
        }
        SymptomMatchVO vo = new SymptomMatchVO();
        vo.setUserPhrase(match.getUserPhrase());
        vo.setCanonicalName(match.getCanonicalName());
        vo.setSymptomCode(match.getSymptomCode());
        vo.setConfidence(match.getConfidence());
        vo.setMethod(match.getMethod());
        vo.setRationale(match.getRationale());
        return vo;
    }
}
