package com.medical.service.kg.symptom;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 仅向量 Top-K 检索（不经过同义词表与 LLM），供评测与调试使用。
 */
@Service
@RequiredArgsConstructor
public class SymptomVectorSearchService {

    private final SymptomEmbeddingService embeddingService;
    private final InMemorySymptomVectorIndex vectorIndex;

    public boolean isIndexReady() {
        return vectorIndex.isReady() && embeddingService.isAvailable();
    }

    public List<ScoredSymptomCandidate> searchTopK(String phrase, int topK) {
        if (!StringUtils.hasText(phrase) || topK <= 0) {
            return List.of();
        }
        if (!vectorIndex.isReady()) {
            throw new IllegalStateException("向量索引未就绪，请先配置 DASHSCOPE_API_KEY 并调用 POST /api/v1/kg/symptom/index/rebuild");
        }
        float[] queryVector = embeddingService.embed(phrase.trim());
        if (queryVector.length == 0) {
            return List.of();
        }
        return vectorIndex.search(queryVector, topK);
    }
}
