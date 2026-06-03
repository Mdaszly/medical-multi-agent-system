package com.medical.service.kg.symptom.benchmark;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ?? / ??? baseline ????????????
 *
 * <p>????????? gold ???????? Recall@K?Hit@1?MRR ????
 */
final class EvalMetricsSupport {

    private EvalMetricsSupport() {
    }

    static VectorCaseMetric buildCaseMetric(String caseId,
                                            String bucket,
                                            List<String> queryPhrases,
                                            Set<String> expected,
                                            List<String> topKNames,
                                            List<Double> topKScores,
                                            int k,
                                            double scoreThreshold) {
        int hitCount = (int) topKNames.stream().filter(expected::contains).count();
        double maxScore = topKScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        double precision;
        double recall;
        if (expected.isEmpty()) {
            // ????????????????????? maxScore < threshold
            boolean abstained = topKNames.isEmpty()
                    || (scoreThreshold > 0 && maxScore < scoreThreshold);
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
            hitAt1 = topKNames.isEmpty() || maxScore < scoreThreshold ? 1.0 : 0.0;
            rr = hitAt1;
        }

        return VectorCaseMetric.builder()
                .caseId(caseId)
                .bucket(bucket)
                .queryPhrases(queryPhrases)
                .expected(new ArrayList<>(expected))
                .topKNames(topKNames)
                .topKScores(topKScores)
                .hitCount(hitCount)
                .precisionAtK(precision)
                .recallAtK(recall)
                .hitAt1(hitAt1)
                .reciprocalRank(rr)
                .rawCandidates(List.of())
                .build();
    }

    static VectorTopKEvalReport aggregate(int k,
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
