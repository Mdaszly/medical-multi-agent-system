package com.medical.service.kg.symptom.benchmark;

import com.medical.config.MedicalGraphProperties;
import com.medical.service.kg.symptom.ScoredSymptomCandidate;
import com.medical.service.kg.symptom.SymptomVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 向量 Top-K 召回评测：在 gold 集上仅走 {@link SymptomVectorSearchService}，不经过同义词 / LLM。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomVectorTopKEvaluator {

    private final VectorEvalDatasetLoader datasetLoader;
    private final SymptomVectorSearchService vectorSearchService;
    private final MedicalGraphProperties graphProperties;

    public VectorTopKEvalReport evaluate(int k) {
        return evaluate(k, datasetLoader.loadDefault(), true);
    }

    public VectorTopKEvalReport evaluate(int k, VectorEvalDataset dataset, boolean includeCaseDetails) {
        long start = System.currentTimeMillis();
        int safeK = Math.max(1, k);
        List<VectorCaseMetric> caseMetrics = new ArrayList<>();
        int totalLabels = 0;

        for (VectorEvalCase evalCase : dataset.getCases()) {
            VectorCaseMetric metric = evaluateCase(evalCase, safeK);
            caseMetrics.add(metric);
            totalLabels += evalCase.getExpected() == null ? 0 : evalCase.getExpected().size();
        }

        VectorTopKEvalReport report = EvalMetricsSupport.aggregate(
                safeK, dataset, caseMetrics, totalLabels, includeCaseDetails);
        report.setEvalMode("VECTOR");
        report.setEvalTimeMs(System.currentTimeMillis() - start);
        report.setVectorIndexReady(vectorSearchService.isIndexReady());
        report.setEmbeddingModel(graphProperties.getSymptomResolver().getEmbeddingModel());
        return report;
    }

    public Map<Integer, VectorTopKEvalReport> evaluateKGrid(List<Integer> kValues) {
        Map<Integer, VectorTopKEvalReport> reports = new LinkedHashMap<>();
        VectorEvalDataset dataset = datasetLoader.loadDefault();
        for (Integer k : kValues) {
            if (k != null && k > 0) {
                reports.put(k, evaluate(k, dataset, false));
            }
        }
        return reports;
    }

    private VectorCaseMetric evaluateCase(VectorEvalCase evalCase, int k) {
        List<String> phrases = evalCase.resolveQueryPhrases();
        Set<String> expected = evalCase.getExpected() == null
                ? Set.of()
                : new LinkedHashSet<>(evalCase.getExpected());

        List<ScoredSymptomCandidate> merged = new ArrayList<>();
        for (String phrase : phrases) {
            merged.addAll(vectorSearchService.searchTopK(phrase, k));
        }
        List<ScoredSymptomCandidate> topK = mergeAndRank(merged, k);
        List<String> topKNames = topK.stream()
                .map(c -> c.getEntry().getName())
                .collect(Collectors.toList());
        List<Double> topKScores = topK.stream()
                .map(ScoredSymptomCandidate::getScore)
                .collect(Collectors.toList());

        double scoreThreshold = graphProperties.getSymptomResolver().getVectorMinScore();
        return EvalMetricsSupport.buildCaseMetric(
                evalCase.getId(),
                evalCase.getBucket(),
                phrases,
                expected,
                topKNames,
                topKScores,
                k,
                scoreThreshold);
    }

    private List<ScoredSymptomCandidate> mergeAndRank(List<ScoredSymptomCandidate> candidates, int k) {
        Map<String, ScoredSymptomCandidate> best = new LinkedHashMap<>();
        for (ScoredSymptomCandidate c : candidates) {
            String name = c.getEntry().getName();
            if (name == null) {
                continue;
            }
            best.merge(name, c, (a, b) -> a.getScore() >= b.getScore() ? a : b);
        }
        return best.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(k)
                .collect(Collectors.toList());
    }
}
