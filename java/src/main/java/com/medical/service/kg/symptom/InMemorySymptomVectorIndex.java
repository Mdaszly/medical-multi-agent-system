package com.medical.service.kg.symptom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存症状向量索引
 * 
 * 功能：将症状词汇表与向量数据构建为内存索引，支持快速向量相似度检索
 * 
 * 工作流程：
 * 1. rebuild() - 构建索引：将词汇表+向量组合为可检索的数据结构
 * 2. search() - 向量检索：计算查询向量与所有症状向量的余弦相似度，返回TopK结果
 * 
 * 应用场景：
 * - 用户输入"头疼" → 转为向量 → 在索引中找最相似的症状
 * - 返回结果：头痛(0.998)、头晕(0.456)、恶心(0.123)
 * 
 * 设计要点：
 * - 使用AtomicReference实现无锁线程安全
 * - 全内存索引，适合小规模数据（<10万条）
 * - 快照模式，重建时不影响正在进行的查询
 */
@Slf4j
@Component
public class InMemorySymptomVectorIndex {

    /** 索引快照，使用AtomicReference保证线程安全 */
    private final AtomicReference<IndexSnapshot> snapshot = new AtomicReference<>(IndexSnapshot.empty());

    /**
     * 检查索引是否就绪
     * 
     * @return true表示索引已构建完成，可执行查询
     */
    public boolean isReady() {
        return snapshot.get().ready();
    }

    /**
     * 获取索引中的症状数量
     * 
     * @return 已索引的症状条目数
     */
    public int size() {
        return snapshot.get().entries().size();
    }

    /**
     * 重建向量索引
     * 
     * 流程：
     * 1. 验证词汇表与向量数量是否匹配
     * 2. 将词汇条目+向量打包为IndexedSymptom
     * 3. 替换快照（原子操作，查询线程无感知）
     * 
     * @param vocabulary 症状词汇表
     * @param vectors    对应的向量列表（与词汇表一一对应）
     */
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

    /**
     * 清空索引
     * 
     * 场景：系统重启或数据刷新时调用
     */
    public void clear() {
        snapshot.set(IndexSnapshot.empty());
    }

    /**
     * 向量相似度检索
     * 
     * 流程：
     * 1. 获取当前索引快照
     * 2. 遍历所有症状，计算查询向量与各症状向量的余弦相似度
     * 3. 按相似度降序排序
     * 4. 返回TopK结果
     * 
     * @param query 查询向量（用户描述的向量表示）
     * @param topK  返回结果数量
     * @return 按相似度排序的症状候选列表
     */
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

    /** 已索引的症状条目（词汇+向量） */
    private record IndexedSymptom(SymptomVocabularyEntry entry, float[] vector) {
    }

    /**
     * 索引快照（不可变）
     * 
     * 设计意图：
     * - 使用record保证不可变性
     * - 重建索引时创建新快照，避免读写冲突
     * - ready标志区分空索引和有效索引
     */
    private record IndexSnapshot(List<IndexedSymptom> entries, boolean ready) {
        static IndexSnapshot empty() {
            return new IndexSnapshot(List.of(), false);
        }
    }
}
