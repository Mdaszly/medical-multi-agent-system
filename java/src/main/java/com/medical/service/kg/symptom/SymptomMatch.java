package com.medical.service.kg.symptom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SymptomMatch {

    private String userPhrase;
    private String canonicalName;
    private String symptomCode;
    private double confidence;
    /** SYNONYM | VECTOR | LLM | RULE */
    private String method;
    private String rationale;
}
