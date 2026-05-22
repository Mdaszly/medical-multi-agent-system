package com.medical.service.kg.symptom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoredSymptomCandidate {

    private SymptomVocabularyEntry entry;
    private double score;
}
