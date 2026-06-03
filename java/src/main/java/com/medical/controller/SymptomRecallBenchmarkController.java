package com.medical.controller;

import com.medical.model.dto.kg.VectorTopKEvalRequest;
import com.medical.service.kg.symptom.benchmark.SymptomEvalComparisonReport;
import com.medical.service.kg.symptom.benchmark.SymptomEvalComparisonService;
import com.medical.service.kg.symptom.benchmark.SymptomSynonymOnlyEvaluator;
import com.medical.service.kg.symptom.benchmark.SymptomVectorTopKEvaluator;
import com.medical.service.kg.symptom.benchmark.VectorEvalDataset;
import com.medical.service.kg.symptom.benchmark.VectorEvalDatasetLoader;
import com.medical.service.kg.symptom.benchmark.VectorTopKEvalReport;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kg/symptom")
@RequiredArgsConstructor
@Tag(name = "症状召回评测", description = "gold 集上对比同义词 baseline 与向量 Top-K 召回率")
public class SymptomRecallBenchmarkController {

    private final SymptomVectorTopKEvaluator vectorTopKEvaluator;
    private final SymptomSynonymOnlyEvaluator synonymOnlyEvaluator;
    private final SymptomEvalComparisonService evalComparisonService;
    private final VectorEvalDatasetLoader datasetLoader;

    @PostMapping("/eval/vector-topk")
    @Operation(summary = "运行向量 Top-K 评测集并输出召回率等指标")
    public ResponseEntity<Object> evalVectorTopK(@RequestBody(required = false) VectorTopKEvalRequest request) {
        VectorTopKEvalRequest req = request != null ? request : new VectorTopKEvalRequest();
        VectorEvalDataset dataset = resolveDataset(req);

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

    @PostMapping("/eval/synonym-only")
    @Operation(summary = "运行同义词-only baseline 评测（无向量/LLM，用于对比召回提升）")
    public ResponseEntity<VectorTopKEvalReport> evalSynonymOnly(@RequestBody(required = false) VectorTopKEvalRequest request) {
        VectorTopKEvalRequest req = request != null ? request : new VectorTopKEvalRequest();
        VectorEvalDataset dataset = resolveDataset(req);
        int k = req.getK() != null ? req.getK() : 5;
        boolean details = Boolean.TRUE.equals(req.getIncludeCaseDetails());
        return ResponseEntity.ok(synonymOnlyEvaluator.evaluate(k, dataset, details));
    }

    @GetMapping("/eval/synonym-only")
    @Operation(summary = "GET 方式运行同义词-only baseline 评测（默认 K=5）")
    public ResponseEntity<VectorTopKEvalReport> evalSynonymOnlyGet(
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(defaultValue = "false") boolean includeCaseDetails) {
        return ResponseEntity.ok(synonymOnlyEvaluator.evaluate(k, datasetLoader.loadDefault(), includeCaseDetails));
    }

    @PostMapping("/eval/compare")
    @Operation(summary = "并排对比同义词 baseline 与向量召回，输出 Recall 提升幅度")
    public ResponseEntity<SymptomEvalComparisonReport> evalCompare(@RequestBody(required = false) VectorTopKEvalRequest request) {
        VectorTopKEvalRequest req = request != null ? request : new VectorTopKEvalRequest();
        VectorEvalDataset dataset = resolveDataset(req);
        int k = req.getK() != null ? req.getK() : 5;
        boolean details = Boolean.TRUE.equals(req.getIncludeCaseDetails());
        return ResponseEntity.ok(evalComparisonService.compare(k, dataset, details));
    }

    @GetMapping("/eval/compare")
    @Operation(summary = "GET 方式并排对比同义词 baseline 与向量召回")
    public ResponseEntity<SymptomEvalComparisonReport> evalCompareGet(
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(defaultValue = "false") boolean includeCaseDetails) {
        return ResponseEntity.ok(evalComparisonService.compare(k, includeCaseDetails));
    }

    private VectorEvalDataset resolveDataset(VectorTopKEvalRequest req) {
        return req.getDatasetPath() != null && !req.getDatasetPath().isBlank()
                ? datasetLoader.load(req.getDatasetPath())
                : datasetLoader.loadDefault();
    }
}
