package com.medical.service.kg.symptom;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class SymptomResolutionResult {

    @Builder.Default
    private List<SymptomMatch> matches = new ArrayList<>();

    @Builder.Default
    private Set<String> canonicalSymptomNames = new LinkedHashSet<>();

    private long resolveTimeMs;
    private boolean vectorIndexReady;
    private String traceSummary;

    public boolean hasAcceptedMatches() {
        return !canonicalSymptomNames.isEmpty();
    }
}
