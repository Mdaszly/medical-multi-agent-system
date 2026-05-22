package com.medical.service.kg.symptom.eval;

import com.medical.service.kg.symptom.ScoredSymptomCandidate;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VectorCaseMetric {

    private String caseId;
    private String bucket;
    private List<String> queryPhrases;
    private List<String> expected;
    private List<String> topKNames;
    private List<Double> topKScores;

    private int hitCount;
    private double precisionAtK;
    private double recallAtK;
    private double hitAt1;
    private double reciprocalRank;

    @Builder.Default
    private List<ScoredSymptomCandidate> rawCandidates = new ArrayList<>();
}
