package com.medical.service.kg.symptom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import com.medical.mapper.SymptomMapper;
import com.medical.model.entity.SymptomEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 症状词汇服务
 * 
 * 功能：从知识图谱(Neo4j)或数据库加载症状标准词汇表，并结合同义词映射
 * 供症状解析器使用，将用户输入的症状名称映射到标准术语
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomVocabularyService {

    private final KnowledgeGraphService knowledgeGraphService;
    private final SymptomMapper symptomMapper;
    private final SymptomSynonymRegistry synonymRegistry;
    private final MedicalGraphProperties graphProperties;

    /** 词汇表缓存，使用volatile确保多线程环境下的可见性 */
    private volatile List<SymptomVocabularyEntry> cached = List.of();

    /**
     * 加载症状词汇表
     * 
     * 流程：
     * 1. 优先从Neo4j知识图谱加载
     * 2. 若图谱无数据，则从关系数据库加载
     * 3. 加载后结合同义词表，为每个标准症状补充别名信息
     * 
     * @return 包含标准症状和别名的完整词汇表
     */
    public List<SymptomVocabularyEntry> loadVocabulary() {
        List<SymptomVocabularyEntry> fromGraph = loadFromNeo4j();
        if (!fromGraph.isEmpty()) {
            cached = enrichWithAliases(fromGraph);
            return cached;
        }
        List<SymptomVocabularyEntry> fromDb = loadFromRdb();
        cached = enrichWithAliases(fromDb);
        return cached;
    }

    /**
     * 获取缓存的词汇表
     * 
     * 设计意图：实现懒加载，缓存为空时自动触发加载
     * 
     * @return 症状词汇表缓存
     */
    public List<SymptomVocabularyEntry> getCachedVocabulary() {
        if (cached.isEmpty()) {
            return loadVocabulary();
        }
        return cached;
    }

    /**
     * 刷新词汇表缓存
     * 
     * 场景：当同义词配置文件更新后，调用此方法重新加载
     * 流程：清空缓存 → 重载同义词表 → 重新加载词汇表
     */
    public void refresh() {
        synonymRegistry.reload();
        cached = List.of();
        loadVocabulary();
    }

    /**
     * 从Neo4j知识图谱加载症状词汇
     * 
     * @return 症状词条列表，若图谱未启用或加载失败则返回空列表
     */
    private List<SymptomVocabularyEntry> loadFromNeo4j() {
        if (graphProperties.isEnabled()) {
            try {
                List<Map<String, Object>> rows = knowledgeGraphService.listSymptomVocabulary();
                List<SymptomVocabularyEntry> entries = rows.stream()
                        .map(row -> SymptomVocabularyEntry.builder()
                                .name(str(row.get("name")))
                                .code(str(row.get("code")))
                                .description(str(row.get("description")))
                                .pinyin(str(row.get("pinyin")))
                                .categoryName(str(row.get("categoryName")))
                                .build())
                        .filter(e -> e.getName() != null && !e.getName().isBlank())
                        .collect(Collectors.toList());
                log.info("从 Neo4j 加载症状词: {} 个 - {}", entries.size(), entries.stream().map(SymptomVocabularyEntry::getName).toList());
                return entries;
            } catch (Exception e) {
                log.warn("从 Neo4j 加载症状词表失败: {}", e.getMessage());
            }
        } else {
            log.info("Neo4j 图谱未启用，跳过从 Neo4j 加载症状词");
        }
        return List.of();
    }

    /**
     * 从关系型数据库加载症状词汇
     * 
     * 查询条件：仅加载status=1(启用)的症状，按频次降序排列
     * 
     * @return 症状词条列表，若加载失败则返回空列表
     */
    private List<SymptomVocabularyEntry> loadFromRdb() {
        try {
            List<SymptomEntity> entities = symptomMapper.selectList(
                    new LambdaQueryWrapper<SymptomEntity>()
                            .eq(SymptomEntity::getStatus, 1)
                            .orderByDesc(SymptomEntity::getFrequency));
            List<SymptomVocabularyEntry> entries = entities.stream()
                    .map(e -> SymptomVocabularyEntry.builder()
                            .name(e.getName())
                            .code(e.getCategoryCode())
                            .pinyin(e.getNamePinyin())
                            .categoryName(e.getCategoryName())
                            .build())
                    .collect(Collectors.toList());
            log.info("从数据库加载症状词: {} 个 - {}", entries.size(), entries.stream().map(SymptomVocabularyEntry::getName).toList());
            return entries;
        } catch (Exception e) {
            log.warn("从 RDB 加载症状词表失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 为症状词条补充别名信息
     * 
     * 逻辑：
     * 1. 从同义词注册表构建「标准名→别名列表」的映射
     * 2. 为每个症状词条添加对应的别名集合
     * 
     * @param entries 原始症状词条列表
     * @return 补充别名后的完整词条列表
     */
    private List<SymptomVocabularyEntry> enrichWithAliases(List<SymptomVocabularyEntry> entries) {
        Map<String, List<String>> canonicalToAliases = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : synonymRegistry.getAliasToCanonical().entrySet()) {
            canonicalToAliases.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        return entries.stream()
                .map(entry -> {
                    List<String> aliases = new ArrayList<>(
                            canonicalToAliases.getOrDefault(entry.getName(), List.of()));
                    return SymptomVocabularyEntry.builder()
                            .name(entry.getName())
                            .code(entry.getCode())
                            .description(entry.getDescription())
                            .pinyin(entry.getPinyin())
                            .categoryName(entry.getCategoryName())
                            .aliases(aliases)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 安全地将对象转为字符串
     * 
     * @param o 任意对象
     * @return 字符串表示，若为null则返回null
     */
    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
