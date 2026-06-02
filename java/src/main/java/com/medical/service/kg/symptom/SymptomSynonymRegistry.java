package com.medical.service.kg.symptom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 症状同义词注册表
 * 
 * 功能：维护医学症状的「别名→标准名」映射关系
 * 例如："头疼"→"头痛"，"拉肚子"→"腹泻"，"脑壳疼"→"头痛"
 * 
 * 数据来源：kg/symptom-synonyms.json 配置文件
 * 用途：将用户口语化表达快速映射到标准医学症状术语
 */
@Slf4j
@Component
public class SymptomSynonymRegistry {

    private final ObjectMapper objectMapper;
    /** 别名→标准名映射表，使用volatile确保多线程可见性 */
    private volatile Map<String, String> aliasToCanonical = Map.of();

    public SymptomSynonymRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 应用启动时自动加载同义词表 */
    @PostConstruct
    public void load() {
        reload();
    }

    /**
     * 重新加载同义词表
     * 
     * 场景：同义词配置文件更新后调用此方法刷新映射表
     * 失败处理：加载失败时使用空表，系统降级为依赖向量检索/LLM解析
     */
    public void reload() {
        try (InputStream in = new ClassPathResource("kg/symptom-synonyms.json").getInputStream()) {
            Map<String, String> loaded = objectMapper.readValue(in, new TypeReference<>() {});
            // 1.LinkedHashMap保持JSON文件中的顺序 - 读取symptom-synonyms.json时，保持原始配置顺序不变
            // 2. 便于调试和日志查看 - 遍历时按插入顺序输出，与配置文件一致
            aliasToCanonical = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
            log.info("症状同义词表加载完成: {} 条", aliasToCanonical.size());
        } catch (Exception e) {
            log.warn("症状同义词表加载失败，将仅依赖向量/LLM: {}", e.getMessage());
            aliasToCanonical = Map.of();
        }
    }

    /**
     * 精确匹配同义词
     * 
     * @param phrase 用户输入的短语（如"头疼"）
     * @return 若找到对应标准名，返回匹配结果（confidence=0.98）；否则返回空
     */
    public Optional<SymptomMatch> resolveExact(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return Optional.empty();
        }
        String trimmed = phrase.trim();
        String canonical = aliasToCanonical.get(trimmed);
        if (canonical != null) {
            return Optional.of(SymptomMatch.builder()
                    .userPhrase(trimmed)
                    .canonicalName(canonical)
                    .confidence(0.98)
                    .method("SYNONYM")
                    .rationale("同义词表映射")
                    .build());
        }
        return Optional.empty();
    }

    /**
     * 从文本中找出所有匹配的同义词别名
     * 
     * 逻辑：
     * 1. 将别名按长度降序排列（优先匹配长词，避免短词误匹配）
     * 2. 扫描文本，找出所有包含的别名
     * 
     * 示例：text="我头疼且肚子不舒服" → 找到["头疼", "肚子疼"]
     * 
     * @param text 用户原始文本
     * @return 匹配到的别名列表
     */
    public List<String> findAliasesInText(String text) {
        if (text == null || text.isBlank() || aliasToCanonical.isEmpty()) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        List<String> keys = new ArrayList<>(aliasToCanonical.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String alias : keys) {
            if (text.contains(alias)) {
                found.add(alias);
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * 获取完整的别名→标准名映射表
     * 
     * @return 不可变Map，供词汇表服务构建别名信息
     */
    public Map<String, String> getAliasToCanonical() {
        return aliasToCanonical;
    }
}
