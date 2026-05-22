package com.medical.service.kg.symptom;

import com.medical.config.MedicalGraphProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SymptomVectorIndexBootstrap {

    private final MedicalGraphProperties graphProperties;
    private final SymptomVocabularyService vocabularyService;
    private final SymptomEmbeddingService embeddingService;
    private final InMemorySymptomVectorIndex vectorIndex;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        MedicalGraphProperties.SymptomResolver cfg = graphProperties.getSymptomResolver();
        if (!graphProperties.isEnabled() || !cfg.isEnabled() || !cfg.isBuildIndexOnStartup()) {
            return;
        }
        rebuild();
    }

    public void rebuild() {
        if (!embeddingService.isAvailable()) {
            log.info("Embedding API 未配置，跳过症状向量索引构建（仍可使用同义词/LLM 路径）");
            vectorIndex.clear();
            return;
        }
        vocabularyService.refresh();
        List<SymptomVocabularyEntry> vocabulary = vocabularyService.loadVocabulary();
        if (vocabulary.isEmpty()) {
            log.warn("症状词表为空，无法构建向量索引");
            return;
        }
        log.info("开始构建向量索引，共 {} 个症状词", vocabulary.size());
        List<String> texts = vocabulary.stream().map(SymptomVocabularyEntry::indexText).toList();
        List<float[]> vectors = embeddingService.embedBatch(texts);
        vectorIndex.rebuild(vocabulary, vectors);
        log.info("向量索引构建完成，索引大小: {}", vectorIndex.size());
    }
}
