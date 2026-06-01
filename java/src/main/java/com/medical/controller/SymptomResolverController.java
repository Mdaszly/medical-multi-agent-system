package com.medical.controller;

import com.medical.model.dto.kg.VectorTopKEvalRequest;
import com.medical.service.kg.clinical.ClinicalSpanExtractionResult;
import com.medical.service.kg.clinical.ClinicalSpanExtractor;
import com.medical.service.kg.symptom.*;
import com.medical.service.kg.symptom.eval.SymptomVectorTopKEvaluator;
import com.medical.service.kg.symptom.eval.VectorEvalDataset;
import com.medical.service.kg.symptom.eval.VectorEvalDatasetLoader;
import com.medical.service.kg.symptom.eval.VectorTopKEvalReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kg/symptom")
@RequiredArgsConstructor
@Tag(name = "症状语义解析", description = "向量召回 + LLM 封闭词表消歧（AI 工程化）")
public class SymptomResolverController {

    private final SymptomResolver symptomResolver;
    private final ClinicalSpanExtractor clinicalSpanExtractor;
    private final SymptomVectorIndexBootstrap vectorIndexBootstrap;
    private final InMemorySymptomVectorIndex vectorIndex;
    private final SymptomVectorSearchService vectorSearchService;
    private final SymptomVectorTopKEvaluator vectorTopKEvaluator;
    private final VectorEvalDatasetLoader datasetLoader;
    private final SymptomVocabularyService vocabularyService;

    @PostMapping("/resolve")
    @Operation(summary = "解析用户症状表述为标准症状")
    public ResponseEntity<Object> resolve(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        String structuredSymptom = body == null ? null : body.get("symptom");
        return ResponseEntity.ok(resolveWithClinicalGate(structuredSymptom, text));
    }

    @GetMapping("/resolve")
    @Operation(summary = "GET 方式解析（便于调试）")
    public ResponseEntity<Object> resolveGet(
            @RequestParam String text,
            @RequestParam(required = false) String symptom) {
        return ResponseEntity.ok(resolveWithClinicalGate(symptom, text));
    }

    private Object resolveWithClinicalGate(String structuredSymptom, String rawText) {
        ClinicalSpanExtractionResult span = clinicalSpanExtractor.extract(structuredSymptom, rawText);
        if (!span.isHasClinicalText()) {
            return Map.of(
                    "skipped", true,
                    "skipReason", span.getSkipReason(),
                    "traceSummary", span.getTraceSummary(),
                    "matches", List.of(),
                    "canonicalSymptomNames", List.of()
            );
        }
        SymptomResolutionResult result = symptomResolver.resolve(span.getClinicalText());
        return Map.of(
                "skipped", false,
                "clinicalTextUsed", span.getClinicalText(),
                "clinicalSpanSource", span.getSource(),
                "spanTrace", span.getTraceSummary(),
                "resolution", result
        );
    }

    @PostMapping("/index/rebuild")
    @Operation(summary = "重建症状向量内存索引")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        vectorIndexBootstrap.rebuild();
        return ResponseEntity.ok(Map.of(
                "message", "rebuild triggered",
                "vectorIndexReady", vectorIndex.isReady(),
                "vectorIndexSize", vectorIndex.size()
        ));
    }

    @GetMapping("/vocabulary/debug")
    @Operation(summary = "调试：查看当前症状词表详情")
    public ResponseEntity<Map<String, Object>> debugVocabulary() {
        List<SymptomVocabularyEntry> vocabulary = vocabularyService.getCachedVocabulary();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", vocabulary.size());
        result.put("entries", vocabulary.stream().map(e -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", e.getName());
            info.put("indexText", e.indexText());
            info.put("aliases", e.getAliases());
            return info;
        }).toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vector/search")
    @Operation(summary = "仅向量 Top-K 检索（评测用，不经过同义词/LLM）")
    public ResponseEntity<Map<String, Object>> vectorSearch(
            @RequestParam String phrase,
            @RequestParam(defaultValue = "5") int topK) {
        List<ScoredSymptomCandidate> candidates = vectorSearchService.searchTopK(phrase, topK);
        List<Map<String, Object>> items = candidates.stream()
                .map(c -> Map.<String, Object>of(
                        "name", c.getEntry().getName(),
                        "score", c.getScore(),
                        "code", c.getEntry().getCode() != null ? c.getEntry().getCode() : ""))
                .toList();
        return ResponseEntity.ok(Map.of(
                "phrase", phrase,
                "topK", topK,
                "vectorIndexReady", vectorSearchService.isIndexReady(),
                "candidates", items));
    }

    @PostMapping("/eval/vector-topk")
    @Operation(summary = "运行向量 Top-K 评测集并输出召回率等指标")
    public ResponseEntity<Object> evalVectorTopK(@RequestBody(required = false) VectorTopKEvalRequest request) {
        VectorTopKEvalRequest req = request != null ? request : new VectorTopKEvalRequest();
        VectorEvalDataset dataset = req.getDatasetPath() != null && !req.getDatasetPath().isBlank()
                ? datasetLoader.load(req.getDatasetPath())
                : datasetLoader.loadDefault();

        boolean details = Boolean.TRUE.equals(req.getIncludeCaseDetails());
        if (req.getKValues() != null && !req.getKValues().isEmpty()) {
            Map<Integer, VectorTopKEvalReport> grid = new LinkedHashMap<>();
            for (Integer k : req.getKValues()) {
                grid.put(k, vectorTopKEvaluator.evaluate(k, dataset, false));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mode", "k_grid");
            body.put("reports", grid);
            return ResponseEntity.ok(body);
        }
        int k = req.getK() != null ? req.getK() : 5;
        return ResponseEntity.ok(vectorTopKEvaluator.evaluate(k, dataset, details));
    }

    @GetMapping("/eval/vector-topk")
    @Operation(summary = "GET 方式运行向量 Top-K 评测（默认 K=5）")
    public ResponseEntity<VectorTopKEvalReport> evalVectorTopKGet(
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(defaultValue = "false") boolean includeCaseDetails) {
        return ResponseEntity.ok(vectorTopKEvaluator.evaluate(k, datasetLoader.loadDefault(), includeCaseDetails));
    }
}
