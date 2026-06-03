package com.medical.service.kg.symptom.benchmark;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ??? baseline ?????????????
 */
@Service
@RequiredArgsConstructor
public class SymptomEvalComparisonService {

    private final VectorEvalDatasetLoader datasetLoader;
    private final SymptomSynonymOnlyEvaluator synonymOnlyEvaluator;
    private final SymptomVectorTopKEvaluator vectorTopKEvaluator;

    public SymptomEvalComparisonReport compare(int k, VectorEvalDataset dataset, boolean includeCaseDetails) {
        long start = System.currentTimeMillis();
        int safeK = Math.max(1, k);

        VectorTopKEvalReport baseline = synonymOnlyEvaluator.evaluate(safeK, dataset, includeCaseDetails);
        VectorTopKEvalReport vector = vectorTopKEvaluator.evaluate(safeK, dataset, includeCaseDetails);

        return SymptomEvalComparisonReport.builder()
                .k(safeK)
                .datasetVersion(dataset.getVersion())
                .totalCases(dataset.getCases().size())
                .synonymOnlyBaseline(baseline)
                .vectorRecall(vector)
                .improvement(SymptomEvalComparisonReport.computeImprovement(baseline, vector))
                .evalTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    public SymptomEvalComparisonReport compare(int k, boolean includeCaseDetails) {
        return compare(k, datasetLoader.loadDefault(), includeCaseDetails);
    }
}
