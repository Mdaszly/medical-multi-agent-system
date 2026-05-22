package com.medical.service.kg.symptom;

import com.medical.config.MedicalGraphProperties;
import com.medical.service.DashScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomEmbeddingService {

    private final DashScopeService dashScopeService;
    private final MedicalGraphProperties graphProperties;

    public boolean isAvailable() {
        return dashScopeService.isConfigured();
    }

    public float[] embed(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return new float[0];
        }
        try {
            return dashScopeService.embed(text, embeddingModel());
        } catch (Exception e) {
            log.warn("单条症状向量生成失败: {}", e.getMessage());
            return new float[0];
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (!isAvailable() || texts == null || texts.isEmpty()) {
            return List.of();
        }
        int batchSize = Math.max(1, graphProperties.getSymptomResolver().getEmbeddingBatchSize());
        List<float[]> all = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> chunk = texts.subList(i, end);
            try {
                all.addAll(dashScopeService.embedBatch(chunk, embeddingModel()));
            } catch (Exception e) {
                log.warn("批量症状向量生成失败 [{}-{}]: {}", i, end, e.getMessage());
                for (int j = 0; j < chunk.size(); j++) {
                    all.add(new float[0]);
                }
            }
        }
        return all;
    }

    private String embeddingModel() {
        return graphProperties.getSymptomResolver().getEmbeddingModel();
    }
}
