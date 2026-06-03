package com.medical.service.kg.symptom.benchmark;

import com.medical.service.kg.symptom.ScoredSymptomCandidate;
import com.medical.service.kg.symptom.SymptomMatch;
import com.medical.service.kg.symptom.SymptomSynonymRegistry;
import com.medical.service.kg.symptom.SymptomVocabularyEntry;
import com.medical.service.kg.symptom.SymptomVocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ???-only ?????baseline??
 *
 * <p>?? {@link SymptomVectorTopKEvaluator} ??? gold ?????
 * ?????????? + ??????????????? embedding / LLM?
 * ????????????? Recall ???
 *
 * <p>????? {@link com.medical.service.kg.symptom.SymptomResolver#resolvePhrase}
 * ???????????????? baseline ??????????????????
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomSynonymOnlyEvaluator {

    private final VectorEvalDatasetLoader datasetLoader;
    private final SymptomSynonymRegistry synonymRegistry;
    private final SymptomVocabularyService vocabularyService;

    public VectorTopKEvalReport evaluate(int k) {
        return evaluate(k, datasetLoader.loadDefault(), true);
    }

    public VectorTopKEvalReport evaluate(int k, VectorEvalDataset dataset, boolean includeCaseDetails) {
        long start = System.currentTimeMillis();
        int safeK = Math.max(1, k);
        Map<String, SymptomVocabularyEntry> byName = buildNameIndex();

        List<VectorCaseMetric> caseMetrics = new ArrayList<>();
        int totalLabels = 0;
        for (VectorEvalCase evalCase : dataset.getCases()) {
            VectorCaseMetric metric = evaluateCase(evalCase, safeK, byName);
            caseMetrics.add(metric);
            totalLabels += evalCase.getExpected() == null ? 0 : evalCase.getExpected().size();
        }

        VectorTopKEvalReport report = EvalMetricsSupport.aggregate(
                safeK, dataset, caseMetrics, totalLabels, includeCaseDetails);
        report.setEvalMode("SYNONYM_ONLY");
        report.setVectorIndexReady(false);
        report.setEmbeddingModel(null);
        report.setSynonymTableSize(synonymRegistry.getAliasToCanonical().size());
        report.setEvalTimeMs(System.currentTimeMillis() - start);
        return report;
    }

    private Map<String, SymptomVocabularyEntry> buildNameIndex() {
        Map<String, SymptomVocabularyEntry> byName = new LinkedHashMap<>();
        for (SymptomVocabularyEntry entry : vocabularyService.getCachedVocabulary()) {
            byName.put(entry.getName(), entry);
        }
        return byName;
    }

    private VectorCaseMetric evaluateCase(VectorEvalCase evalCase,
                                        int k,
                                        Map<String, SymptomVocabularyEntry> byName) {
        List<String> phrases = evalCase.resolveQueryPhrases();
        Set<String> expected = evalCase.getExpected() == null
                ? Set.of()
                : new LinkedHashSet<>(evalCase.getExpected());

        List<ScoredSymptomCandidate> merged = new ArrayList<>();
        for (String phrase : phrases) {
            merged.addAll(searchSynonymOnly(phrase, k, byName));
        }
        List<ScoredSymptomCandidate> topK = mergeAndRank(merged, k);
        List<String> topKNames = topK.stream()
                .map(c -> c.getEntry().getName())
                .collect(Collectors.toList());
        List<Double> topKScores = topK.stream()
                .map(ScoredSymptomCandidate::getScore)
                .collect(Collectors.toList());

        return EvalMetricsSupport.buildCaseMetric(
                evalCase.getId(),
                evalCase.getBucket(),
                phrases,
                expected,
                topKNames,
                topKScores,
                k,
                0.0);
    }

    private List<ScoredSymptomCandidate> searchSynonymOnly(String phrase,
                                                           int k,
                                                           Map<String, SymptomVocabularyEntry> byName) {
        if (phrase == null || phrase.isBlank()) {
            return List.of();
        }
        String trimmed = phrase.trim();
        List<ScoredSymptomCandidate> candidates = new ArrayList<>();

        if (byName.containsKey(trimmed)) {
            candidates.add(ScoredSymptomCandidate.builder()
                    .entry(byName.get(trimmed))
                    .score(1.0)
                    .build());
        }

        Optional<SymptomMatch> synonym = synonymRegistry.resolveExact(trimmed);
        if (synonym.isPresent()) {
            String canonical = synonym.get().getCanonicalName();
            boolean alreadyPresent = candidates.stream()
                    .anyMatch(c -> canonical.equals(c.getEntry().getName()));
            if (!alreadyPresent) {
                SymptomVocabularyEntry entry = byName.getOrDefault(
                        canonical,
                        SymptomVocabularyEntry.builder().name(canonical).build());
                candidates.add(ScoredSymptomCandidate.builder()
                        .entry(entry)
                        .score(0.98)
                        .build());
            }
        }

        return candidates.stream().limit(k).collect(Collectors.toList());
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
