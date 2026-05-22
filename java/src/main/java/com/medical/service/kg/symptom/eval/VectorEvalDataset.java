package com.medical.service.kg.symptom.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorEvalDataset {

    private String version;
    private String description;
    private Map<String, Object> labelRules;
    private Map<String, Integer> distribution;
    private List<VectorEvalCase> cases = new ArrayList<>();
}
