package com.medical.service.kg.symptom.eval;

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

        VectorTopKEvalReport report = aggregate(safeK, dataset, caseMetrics, totalLabels, includeCaseDetails);
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
        int hitCount = (int) topKNames.stream().filter(expected::contains).count();
        double maxScore = topKScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        double precision;
        double recall;
        if (expected.isEmpty()) {
            // 负例：Top1 相似度低于阈值视为「未误召」
            boolean abstained = topKScores.isEmpty() || maxScore < scoreThreshold;
            precision = abstained ? 1.0 : 0.0;
            recall = precision;
        } else {
            precision = (double) hitCount / k;
            recall = (double) hitCount / expected.size();
        }

        double hitAt1 = 0.0;
        double rr = 0.0;
        if (!expected.isEmpty()) {
            if (!topKNames.isEmpty() && expected.contains(topKNames.get(0))) {
                hitAt1 = 1.0;
            }
            for (int i = 0; i < topKNames.size(); i++) {
                if (expected.contains(topKNames.get(i))) {
                    rr = 1.0 / (i + 1);
                    break;
                }
            }
        } else {
            hitAt1 = maxScore < scoreThreshold ? 1.0 : 0.0;
            rr = hitAt1;
        }

        return VectorCaseMetric.builder()
                .caseId(evalCase.getId())
                .bucket(evalCase.getBucket())
                .queryPhrases(phrases)
                .expected(new ArrayList<>(expected))
                .topKNames(topKNames)
                .topKScores(topKScores)
                .hitCount(hitCount)
                .precisionAtK(precision)
                .recallAtK(recall)
                .hitAt1(hitAt1)
                .reciprocalRank(rr)
                .rawCandidates(includeRaw() ? topK : List.of())
                .build();
    }

    private boolean includeRaw() {
        return false;
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

    private VectorTopKEvalReport aggregate(int k,
                                           VectorEvalDataset dataset,
                                           List<VectorCaseMetric> caseMetrics,
                                           int totalLabels,
                                           boolean includeCaseDetails) {
        List<Double> precisions = caseMetrics.stream().map(VectorCaseMetric::getPrecisionAtK).toList();
        List<Double> recalls = caseMetrics.stream().map(VectorCaseMetric::getRecallAtK).toList();

        int totalHits = caseMetrics.stream().mapToInt(VectorCaseMetric::getHitCount).sum();
        int microDenomP = caseMetrics.size() * k;

        Map<String, List<VectorCaseMetric>> byBucketMap = caseMetrics.stream()
                .collect(Collectors.groupingBy(VectorCaseMetric::getBucket, LinkedHashMap::new, Collectors.toList()));

        Map<String, VectorTopKEvalReport.BucketSummary> byBucket = new LinkedHashMap<>();
        for (Map.Entry<String, List<VectorCaseMetric>> e : byBucketMap.entrySet()) {
            List<VectorCaseMetric> list = e.getValue();
            byBucket.put(e.getKey(), VectorTopKEvalReport.BucketSummary.builder()
                    .caseCount(list.size())
                    .macroPrecisionAtK(average(list.stream().map(VectorCaseMetric::getPrecisionAtK).toList()))
                    .macroRecallAtK(average(list.stream().map(VectorCaseMetric::getRecallAtK).toList()))
                    .macroHitAt1(average(list.stream().map(VectorCaseMetric::getHitAt1).toList()))
                    .build());
        }

        return VectorTopKEvalReport.builder()
                .k(k)
                .datasetVersion(dataset.getVersion())
                .totalCases(caseMetrics.size())
                .totalLabels(totalLabels)
                .macroPrecisionAtK(average(precisions))
                .macroRecallAtK(average(recalls))
                .macroHitAt1(average(caseMetrics.stream().map(VectorCaseMetric::getHitAt1).toList()))
                .macroMrr(average(caseMetrics.stream().map(VectorCaseMetric::getReciprocalRank).toList()))
                .stdPrecisionAtK(std(precisions))
                .stdRecallAtK(std(recalls))
                .microPrecisionAtK(microDenomP == 0 ? 0.0 : (double) totalHits / microDenomP)
                .microRecallAtK(totalLabels == 0 ? 0.0 : (double) totalHits / totalLabels)
                .byBucket(byBucket)
                .caseDetails(includeCaseDetails ? caseMetrics : List.of())
                .build();
    }

    private static double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double std(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double avg = average(values);
        double variance = values.stream()
                .mapToDouble(v -> (v - avg) * (v - avg))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}
