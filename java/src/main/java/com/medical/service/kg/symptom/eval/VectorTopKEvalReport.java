package com.medical.service.kg.symptom.eval;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class VectorTopKEvalReport {

    private int k;
    private String datasetVersion;
    private int totalCases;
    private int totalLabels;
    private boolean vectorIndexReady;
    private String embeddingModel;

    private double macroPrecisionAtK;
    private double macroRecallAtK;
    private double macroHitAt1;
    private double macroMrr;
    private double stdPrecisionAtK;
    private double stdRecallAtK;

    private double microPrecisionAtK;
    private double microRecallAtK;

    private Map<String, BucketSummary> byBucket;
    private List<VectorCaseMetric> caseDetails;
    private long evalTimeMs;

    @Data
    @Builder
    public static class BucketSummary {
        private int caseCount;
        private double macroRecallAtK;
        private double macroPrecisionAtK;
        private double macroHitAt1;
    }
}
