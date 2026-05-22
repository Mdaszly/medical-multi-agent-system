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

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomVocabularyService {

    private final KnowledgeGraphService knowledgeGraphService;
    private final SymptomMapper symptomMapper;
    private final SymptomSynonymRegistry synonymRegistry;
    private final MedicalGraphProperties graphProperties;

    private volatile List<SymptomVocabularyEntry> cached = List.of();

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

    public List<SymptomVocabularyEntry> getCachedVocabulary() {
        if (cached.isEmpty()) {
            return loadVocabulary();
        }
        return cached;
    }

    public void refresh() {
        synonymRegistry.reload();
        cached = List.of();
        loadVocabulary();
    }

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

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
