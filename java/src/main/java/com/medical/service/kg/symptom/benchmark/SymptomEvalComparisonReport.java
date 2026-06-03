package com.medical.service.kg.symptom.benchmark;

import lombok.Builder;
import lombok.Data;

/**
 * ???-only baseline ???????????
 *
 * <p>??????????????? Recall / Hit@1 ??????????????
 * ????????????
 */
@Data
@Builder
public class SymptomEvalComparisonReport {

    private int k;
    private String datasetVersion;
    private int totalCases;

    /** ???-only baseline?????? LLM? */
    private VectorTopKEvalReport synonymOnlyBaseline;

    /** ?? Top-K ???????? */
    private VectorTopKEvalReport vectorRecall;

    /** ???? baseline ????? */
    private ImprovementSummary improvement;

    private long evalTimeMs;

    @Data
    @Builder
    public static class ImprovementSummary {

        /** macroRecallAtK ?????vector - baseline */
        private double macroRecallAtKDelta;

        /**
         * macroRecallAtK ????????(vector - baseline) / baseline × 100?
         * baseline ? 0 ?? null????????????????? delta??
         */
        private Double macroRecallAtKRelativeGainPct;

        private double macroHitAt1Delta;
        private Double macroHitAt1RelativeGainPct;

        private double macroMrrDelta;
        private Double macroMrrRelativeGainPct;

        private double microRecallAtKDelta;
        private Double microRecallAtKRelativeGainPct;
    }

    static ImprovementSummary computeImprovement(VectorTopKEvalReport baseline, VectorTopKEvalReport vector) {
        return ImprovementSummary.builder()
                .macroRecallAtKDelta(vector.getMacroRecallAtK() - baseline.getMacroRecallAtK())
                .macroRecallAtKRelativeGainPct(relativeGainPct(baseline.getMacroRecallAtK(), vector.getMacroRecallAtK()))
                .macroHitAt1Delta(vector.getMacroHitAt1() - baseline.getMacroHitAt1())
                .macroHitAt1RelativeGainPct(relativeGainPct(baseline.getMacroHitAt1(), vector.getMacroHitAt1()))
                .macroMrrDelta(vector.getMacroMrr() - baseline.getMacroMrr())
                .macroMrrRelativeGainPct(relativeGainPct(baseline.getMacroMrr(), vector.getMacroMrr()))
                .microRecallAtKDelta(vector.getMicroRecallAtK() - baseline.getMicroRecallAtK())
                .microRecallAtKRelativeGainPct(relativeGainPct(baseline.getMicroRecallAtK(), vector.getMicroRecallAtK()))
                .build();
    }

    private static Double relativeGainPct(double baseline, double current) {
        if (baseline == 0.0) {
            return null;
        }
        return (current - baseline) / baseline * 100.0;
    }
}
