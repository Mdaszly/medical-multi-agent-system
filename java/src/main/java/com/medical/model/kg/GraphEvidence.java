package com.medical.model.kg;

import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class GraphEvidence {

    @Builder.Default
    private List<SymptomDiagnosisRow> rows = new ArrayList<>();

    @Builder.Default
    private Set<String> extractedSymptoms = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> icdCandidateCodes = new LinkedHashSet<>();

    private boolean graphHit;

    private long queryTimeMs;

    private String formattedText;

    /** 语义解析轨迹（症状表述 → 标准名） */
    private String symptomResolutionTrace;

    @Builder.Default
    private List<com.medical.service.kg.symptom.SymptomMatch> symptomMatches = new ArrayList<>();

}
