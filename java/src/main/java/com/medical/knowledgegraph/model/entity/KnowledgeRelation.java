package com.medical.knowledgegraph.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识图谱关系类
 * 表示两个实体之间的关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelation {

    /**
     * 关系类型常量
     */
    public static final String REL_HAS_SYMPTOM = "HAS_SYMPTOM";           // 疾病-症状: 疾病具有某症状
    public static final String REL_INDICATES = "INDICATES";               // 症状-疾病: 症状指向某疾病
    public static final String REL_CAUSES = "CAUSES";                    // 病因关系
    public static final String REL_TREATS = "TREATS";                    // 药品-疾病: 药品治疗疾病
    public static final String REL_HAS_EFFECT = "HAS_EFFECT";            // 药品-药效: 药品具有某药效
    public static final String REL_APPLIES_TO = "APPLIES_TO";             // 药效-疾病: 药效适用于疾病
    public static final String REL_ASSOCIATED_WITH = "ASSOCIATED_WITH";  // 一般关联关系
    public static final String REL_SIMILAR_TO = "SIMILAR_TO";            // 相似关系
    public static final String REL_COMPLICATION_OF = "COMPLICATION_OF";  // 并发关系
    public static final String REL_CONTRAINDICATED = "CONTRAINDICATED";  // 禁忌关系
    public static final String REL_INTERACTS_WITH = "INTERACTS_WITH";    // 相互作用
    public static final String REL_CLASSIFIED_AS = "CLASSIFIED_AS";      // 分类关系 (ICD)
    public static final String REL_REQUIRES = "REQUIRES";                // 需要检查/治疗

    /**
     * 起始节点ID
     */
    private String sourceId;

    /**
     * 起始节点名称
     */
    private String sourceName;

    /**
     * 起始节点标签
     */
    private String sourceLabel;

    /**
     * 目标节点ID
     */
    private String targetId;

    /**
     * 目标节点名称
     */
    private String targetName;

    /**
     * 目标节点标签
     */
    private String targetLabel;

    /**
     * 关系类型
     */
    private String type;

    /**
     * 关系描述
     */
    private String description;

    /**
     * 权重/置信度 (0.0-1.0)
     */
    private Double weight;

    /**
     * 优先级 (数值越小优先级越高)
     */
    private Integer priority;

    /**
     * 是否紧急
     */
    private Boolean urgent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 关系属性
     */
    private Map<String, Object> properties;

    /**
     * 添加关系属性
     */
    public void addProperty(String key, Object value) {
        if (properties == null) {
            properties = Map.of(key, value);
        }
        properties.put(key, value);
    }

    /**
     * 生成Cypher关系创建语句
     */
    public String toCypherRelationship() {
        StringBuilder cypher = new StringBuilder();
        cypher.append("(").append(sanitizeName(sourceName)).append(":").append(sourceLabel).append(")");
        cypher.append("-[r:").append(type);
        
        if (properties != null && !properties.isEmpty()) {
            cypher.append(" {");
            int count = 0;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (count > 0) cypher.append(", ");
                cypher.append(entry.getKey()).append(": ");
                if (entry.getValue() instanceof String) {
                    cypher.append("'").append(escapeString(entry.getValue().toString())).append("'");
                } else {
                    cypher.append(entry.getValue());
                }
                count++;
            }
            cypher.append("}]");
        } else {
            cypher.append("]");
        }
        
        cypher.append("->(").append(sanitizeName(targetName)).append(":").append(targetLabel).append(")");
        
        return cypher.toString();
    }

    /**
     * 转大写
     */
    public String toUpperCase() {
        return type.toUpperCase();
    }

    /**
     * 转小写
     */
    public String toLowerCase() {
        return type.toLowerCase();
    }

    /**
     * Sanitize name for Cypher
     */
    private String sanitizeName(String name) {
        return name != null ? name.replaceAll("[^a-zA-Z0-9_]", "_") : "node";
    }

    /**
     * Escape string for Cypher
     */
    private String escapeString(String str) {
        return str != null ? str.replace("'", "\\'").replace("\n", "\\n") : "";
    }

    /**
     * 创建便捷方法：症状指示疾病
     */
    public static KnowledgeRelation symptomIndicatesDisease(Symptom symptom, Disease disease) {
        return KnowledgeRelation.builder()
                .sourceId(symptom.getId())
                .sourceName(symptom.getName())
                .sourceLabel(symptom.getLabel())
                .targetId(disease.getId())
                .targetName(disease.getName())
                .targetLabel(disease.getLabel())
                .type(REL_INDICATES)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建便捷方法：疾病具有症状
     */
    public static KnowledgeRelation diseaseHasSymptom(Disease disease, Symptom symptom) {
        return KnowledgeRelation.builder()
                .sourceId(disease.getId())
                .sourceName(disease.getName())
                .sourceLabel(disease.getLabel())
                .targetId(symptom.getId())
                .targetName(symptom.getName())
                .targetLabel(symptom.getLabel())
                .type(REL_HAS_SYMPTOM)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建便捷方法：药品治疗疾病
     */
    public static KnowledgeRelation drugTreatsDisease(Drug drug, Disease disease, Double weight) {
        return KnowledgeRelation.builder()
                .sourceId(drug.getId())
                .sourceName(drug.getName())
                .sourceLabel(drug.getLabel())
                .targetId(disease.getId())
                .targetName(disease.getName())
                .targetLabel(disease.getLabel())
                .type(REL_TREATS)
                .weight(weight)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建便捷方法：药品具有药效
     */
    public static KnowledgeRelation drugHasEffect(Drug drug, DrugEffect effect) {
        return KnowledgeRelation.builder()
                .sourceId(drug.getId())
                .sourceName(drug.getName())
                .sourceLabel(drug.getLabel())
                .targetId(effect.getId())
                .targetName(effect.getName())
                .targetLabel(effect.getLabel())
                .type(REL_HAS_EFFECT)
                .createTime(LocalDateTime.now())
                .build();
    }
}
