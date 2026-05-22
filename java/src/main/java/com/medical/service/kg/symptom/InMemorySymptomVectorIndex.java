package com.medical.service.kg.symptom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class InMemorySymptomVectorIndex {

    private final AtomicReference<IndexSnapshot> snapshot = new AtomicReference<>(IndexSnapshot.empty());

    public boolean isReady() {
        return snapshot.get().ready();
    }

    public int size() {
        return snapshot.get().entries().size();
    }

    public void rebuild(List<SymptomVocabularyEntry> vocabulary, List<float[]> vectors) {
        if (vocabulary == null || vectors == null || vocabulary.size() != vectors.size()) {
            log.warn("向量索引构建跳过: 词表与向量数量不一致 vocab={} vectors={}",
                    vocabulary == null ? 0 : vocabulary.size(),
                    vectors == null ? 0 : (vectors == null ? 0 : vectors.size()));
            snapshot.set(IndexSnapshot.empty());
            return;
        }
        List<IndexedSymptom> indexed = new ArrayList<>();
        for (int i = 0; i < vocabulary.size(); i++) {
            float[] vector = vectors.get(i);
            if (vector != null && vector.length > 0) {
                indexed.add(new IndexedSymptom(vocabulary.get(i), vector));
            }
        }
        snapshot.set(new IndexSnapshot(indexed, true));
        log.info("症状向量索引构建完成: {} 条", indexed.size());
    }

    public void clear() {
        snapshot.set(IndexSnapshot.empty());
    }

    public List<ScoredSymptomCandidate> search(float[] query, int topK) {
        if (query == null || query.length == 0 || topK <= 0) {
            return List.of();
        }
        IndexSnapshot current = snapshot.get();
        if (!current.ready()) {
            return List.of();
        }
        List<ScoredSymptomCandidate> ranked = new ArrayList<>();
        for (IndexedSymptom item : current.entries()) {
            double score = VectorMath.cosine(query, item.vector());
            ranked.add(ScoredSymptomCandidate.builder()
                    .entry(item.entry())
                    .score(score)
                    .build());
        }
        ranked.sort(Comparator.comparingDouble(ScoredSymptomCandidate::getScore).reversed());
        return ranked.size() <= topK ? ranked : ranked.subList(0, topK);
    }

    private record IndexedSymptom(SymptomVocabularyEntry entry, float[] vector) {
    }

    private record IndexSnapshot(List<IndexedSymptom> entries, boolean ready) {
        static IndexSnapshot empty() {
            return new IndexSnapshot(List.of(), false);
        }
    }
}
