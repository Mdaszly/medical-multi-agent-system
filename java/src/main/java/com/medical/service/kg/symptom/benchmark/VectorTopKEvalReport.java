package com.medical.service.kg.symptom.benchmark;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class VectorTopKEvalReport {

    private int k;
    private String datasetVersion;
    private int totalCases;
    private int totalLabels;

    /** ?????VECTOR??????? SYNONYM_ONLY???? baseline? */
    private String evalMode;

    private boolean vectorIndexReady;
    private String embeddingModel;

    /** ???-only ??????symptom-synonyms.json ??? */
    private Integer synonymTableSize;

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
