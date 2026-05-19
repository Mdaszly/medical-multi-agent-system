package com.medical.knowledgegraph.service.extraction;

import com.medical.knowledgegraph.model.entity.*;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实体关系抽取服务
 * 基于规则的实体识别和关系抽取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractionService {

    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 症状关键词
     */
    private static final Set<String> SYMPTOM_KEYWORDS = new HashSet<>(Arrays.asList(
            "疼痛", "发烧", "发热", "咳嗽", "头痛", "胸痛", "腹痛", "背痛", "关节痛", "肌肉痛",
            "恶心", "呕吐", "腹泻", "便秘", "腹胀", "消化不良", "食欲不振", "乏力", "疲劳",
            "头晕", "眩晕", "心悸", "气短", "呼吸困难", "胸闷", "失眠", "嗜睡", "焦虑", "抑郁",
            "皮疹", "瘙痒", "红肿", "出血", "水肿", "黄疸", "脱水", "昏迷", "抽搐", "麻木"
    ));

    /**
     * 疾病关键词
     */
    private static final Set<String> DISEASE_KEYWORDS = new HashSet<>(Arrays.asList(
            "炎", "癌", "瘤", "病", "症", "综合征", "感染", "衰竭", "梗死", "出血",
            "肺炎", "胃炎", "肠炎", "肝炎", "心肌炎", "脑炎", "肾炎", "关节炎", "糖尿病",
            "高血压", "冠心病", "心绞痛", "心梗", "脑卒中", "癫痫", "帕金森", "阿尔茨海默病"
    ));

    /**
     * 从文本中提取症状
     */
    public List<Symptom> extractSymptoms(String text) {
        List<Symptom> symptoms = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return symptoms;
        }

        for (String keyword : SYMPTOM_KEYWORDS) {
            if (text.contains(keyword)) {
                Symptom symptom = Symptom.builder()
                        .id(UUID.randomUUID().toString())
                        .name(keyword)
                        .code("EXT_" + keyword.hashCode())
                        .categoryName(categorizeSymptom(keyword))
                        .build();
                symptoms.add(symptom);
            }
        }

        return symptoms;
    }

    /**
     * 从文本中提取疾病
     */
    public List<Disease> extractDiseases(String text) {
        List<Disease> diseases = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return diseases;
        }

        // 匹配疾病模式
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,10}(?:炎|癌|瘤|病|综合征|感染)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String diseaseName = matcher.group();
            Disease disease = Disease.builder()
                    .id(UUID.randomUUID().toString())
                    .name(diseaseName)
                    .diseaseCode("EXT_" + diseaseName.hashCode())
                    .category(categorizeDisease(diseaseName))
                    .build();
            diseases.add(disease);
        }

        return diseases;
    }

    /**
     * 建立症状-疾病关系
     */
    public void buildSymptomDiseaseRelations(List<Symptom> symptoms, List<Disease> diseases) {
        for (Symptom symptom : symptoms) {
            for (Disease disease : diseases) {
                // 创建双向关系
                KnowledgeRelation indicates = KnowledgeRelation.symptomIndicatesDisease(symptom, disease);
                KnowledgeRelation hasSymptom = KnowledgeRelation.diseaseHasSymptom(disease, symptom);

                try {
                    knowledgeGraphService.createNode(symptom);
                    knowledgeGraphService.createNode(disease);
                    knowledgeGraphService.createRelationship(indicates);
                    knowledgeGraphService.createRelationship(hasSymptom);
                } catch (Exception e) {
                    log.debug("关系已存在: {} -> {}", symptom.getName(), disease.getName());
                }
            }
        }
    }

    /**
     * 建立症状-ICD关联
     */
    public void buildSymptomIcdRelation(Symptom symptom, IcdCode icdCode, double weight) {
        try {
            knowledgeGraphService.createNode(symptom);
            knowledgeGraphService.createNode(icdCode);

            KnowledgeRelation relation = KnowledgeRelation.builder()
                    .sourceId(symptom.getId())
                    .sourceName(symptom.getName())
                    .sourceLabel("Symptom")
                    .targetId(icdCode.getCode())
                    .targetName(icdCode.getDescriptionCn())
                    .targetLabel("ICD10")
                    .type(KnowledgeRelation.REL_ASSOCIATED_WITH)
                    .weight(weight)
                    .build();

            knowledgeGraphService.createRelationship(relation);
        } catch (Exception e) {
            log.error("建立症状-ICD关系失败", e);
        }
    }

    /**
     * 建立药品-疾病关系
     */
    public void buildDrugDiseaseRelation(Drug drug, Disease disease, double efficacy) {
        try {
            knowledgeGraphService.createNode(drug);
            knowledgeGraphService.createNode(disease);

            KnowledgeRelation relation = KnowledgeRelation.drugTreatsDisease(drug, disease, efficacy);
            knowledgeGraphService.createRelationship(relation);
        } catch (Exception e) {
            log.error("建立药品-疾病关系失败", e);
        }
    }

    /**
     * 建立药品-药效关系
     */
    public void buildDrugEffectRelation(Drug drug, DrugEffect effect) {
        try {
            knowledgeGraphService.createNode(drug);
            knowledgeGraphService.createNode(effect);

            KnowledgeRelation relation = KnowledgeRelation.drugHasEffect(drug, effect);
            knowledgeGraphService.createRelationship(relation);
        } catch (Exception e) {
            log.error("建立药品-药效关系失败", e);
        }
    }

    /**
     * 建立药效-疾病关联
     */
    public void buildEffectDiseaseRelation(DrugEffect effect, Disease disease) {
        try {
            knowledgeGraphService.createNode(effect);
            knowledgeGraphService.createNode(disease);

            KnowledgeRelation relation = KnowledgeRelation.builder()
                    .sourceId(effect.getId())
                    .sourceName(effect.getName())
                    .sourceLabel("DrugEffect")
                    .targetId(disease.getId())
                    .targetName(disease.getName())
                    .targetLabel("Disease")
                    .type(KnowledgeRelation.REL_APPLIES_TO)
                    .build();

            knowledgeGraphService.createRelationship(relation);
        } catch (Exception e) {
            log.error("建立药效-疾病关系失败", e);
        }
    }

    /**
     * 症状分类
     */
    private String categorizeSymptom(String symptom) {
        if (symptom.contains("痛")) return "疼痛类";
        if (symptom.contains("热") || symptom.contains("烧")) return "发热类";
        if (symptom.contains("泻") || symptom.contains("秘")) return "消化系统";
        if (symptom.contains("晕") || symptom.contains("眩")) return "神经系统";
        if (symptom.contains("疹") || symptom.contains("痒")) return "皮肤症状";
        return "其他";
    }

    /**
     * 疾病分类
     */
    private String categorizeDisease(String disease) {
        if (disease.contains("炎")) return "炎症性疾病";
        if (disease.contains("癌") || disease.contains("瘤")) return "肿瘤性疾病";
        if (disease.contains("心") || disease.contains("血管")) return "心血管疾病";
        if (disease.contains("肺") || disease.contains("呼吸")) return "呼吸系统疾病";
        if (disease.contains("肝") || disease.contains("胃") || disease.contains("肠")) return "消化系统疾病";
        if (disease.contains("脑") || disease.contains("神")) return "神经系统疾病";
        return "其他疾病";
    }

    /**
     * 从病历文本中提取完整知识图谱
     */
    public void extractFromMedicalRecord(String recordText) {
        log.info("从病历文本中提取知识图谱...");
        
        // 提取症状
        List<Symptom> symptoms = extractSymptoms(recordText);
        log.info("提取到 {} 个症状", symptoms.size());
        
        // 提取疾病
        List<Disease> diseases = extractDiseases(recordText);
        log.info("提取到 {} 个疾病", diseases.size());
        
        // 建立关系
        buildSymptomDiseaseRelations(symptoms, diseases);
        log.info("建立症状-疾病关系完成");
    }

    /**
     * 批量建立关系
     */
    public void batchBuildRelations(List<RelationTemplate> templates) {
        for (RelationTemplate template : templates) {
            try {
                switch (template.getRelationType()) {
                    case "SYMPTOM_DISEASE":
                        buildSymptomDiseaseRelations(
                                Collections.singletonList(template.getSymptom()),
                                Collections.singletonList(template.getDisease())
                        );
                        break;
                    case "SYMPTOM_ICD":
                        buildSymptomIcdRelation(
                                template.getSymptom(),
                                template.getIcdCode(),
                                template.getWeight()
                        );
                        break;
                    case "DRUG_DISEASE":
                        buildDrugDiseaseRelation(
                                template.getDrug(),
                                template.getDisease(),
                                template.getWeight()
                        );
                        break;
                    case "DRUG_EFFECT":
                        buildDrugEffectRelation(
                                template.getDrug(),
                                template.getDrugEffect()
                        );
                        break;
                }
            } catch (Exception e) {
                log.error("建立关系失败: {}", template, e);
            }
        }
    }

    /**
     * 关系模板
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RelationTemplate {
        private String relationType;
        private Symptom symptom;
        private Disease disease;
        private IcdCode icdCode;
        private Drug drug;
        private DrugEffect drugEffect;
        private Double weight;
    }
}
