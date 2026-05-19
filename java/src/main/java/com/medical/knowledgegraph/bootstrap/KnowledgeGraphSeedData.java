package com.medical.knowledgegraph.bootstrap;

import com.medical.knowledgegraph.model.entity.Disease;
import com.medical.knowledgegraph.model.entity.IcdCode;
import com.medical.knowledgegraph.model.entity.KnowledgeRelation;
import com.medical.knowledgegraph.model.entity.Symptom;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 与 docs/sql/seed_symptom_icd.sql 对齐的 Neo4j 权威种子数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphSeedData {

    private final KnowledgeGraphService knowledgeGraphService;

    public void seedIfNeeded() {
        if (!knowledgeGraphService.isEmptyGraph()) {
            log.info("Neo4j 图谱已有数据，跳过种子导入");
            return;
        }
        log.info("Neo4j 图谱为空，开始导入种子数据...");
        createIndexes();
        seedIcdCodes();
        seedSymptoms();
        seedRelations();
        log.info("Neo4j 种子数据导入完成");
    }

    private void createIndexes() {
        knowledgeGraphService.createIndex("Symptom", "name");
        knowledgeGraphService.createIndex("Disease", "name");
        knowledgeGraphService.createIndex("ICD10", "code");
    }

    private void seedIcdCodes() {
        for (IcdSeed icd : ICD_SEEDS) {
            IcdCode node = IcdCode.builder()
                    .code(icd.code)
                    .descriptionEn(icd.descriptionEn)
                    .descriptionCn(icd.descriptionCn)
                    .chapterCode(icd.chapterCode)
                    .chapterName(icd.chapterName)
                    .build();
            node.setName(icd.descriptionCn);
            knowledgeGraphService.upsertNode(node, "code");
        }
    }

    private void seedSymptoms() {
        for (SymptomSeed s : SYMPTOM_SEEDS) {
            Symptom symptom = Symptom.builder()
                    .name(s.name)
                    .pinyin(s.pinyin)
                    .categoryCode(s.categoryCode)
                    .categoryName(s.categoryName)
                    .frequency(s.frequency)
                    .build();
            knowledgeGraphService.upsertNode(symptom, "name");
        }
    }

    private void seedRelations() {
        Map<String, Disease> diseaseByIcd = new LinkedHashMap<>();
        List<KnowledgeRelation> relations = new ArrayList<>();

        for (RelSeed rel : RELATION_SEEDS) {
            IcdSeed icd = ICD_BY_CODE.get(rel.icdCode);
            if (icd == null) {
                continue;
            }
            String diseaseName = icd.diseaseName;
            Disease disease = diseaseByIcd.computeIfAbsent(rel.icdCode, k -> {
                Disease d = Disease.builder()
                        .name(diseaseName)
                        .diseaseCode("D_" + rel.icdCode.replace(".", "_"))
                        .icd10Code(rel.icdCode)
                        .build();
                knowledgeGraphService.upsertNode(d, "name");
                return d;
            });

            IcdCode icdNode = IcdCode.builder()
                    .code(icd.code)
                    .descriptionCn(icd.descriptionCn)
                    .build();
            icdNode.setName(icd.descriptionCn);
            knowledgeGraphService.upsertNode(icdNode, "code");

            Symptom symptomNode = Symptom.builder().name(rel.symptom).build();
            KnowledgeRelation indicates = KnowledgeRelation.symptomIndicatesDisease(symptomNode, disease);
            indicates.setPriority(rel.priority);
            indicates.setWeight(1.0 / rel.priority);
            knowledgeGraphService.createRelationship(indicates);

            KnowledgeRelation classified = KnowledgeRelation.builder()
                    .sourceName(disease.getName())
                    .sourceLabel("Disease")
                    .targetName(icd.descriptionCn)
                    .targetLabel("ICD10")
                    .type(KnowledgeRelation.REL_CLASSIFIED_AS)
                    .build();
            relations.add(classified);
        }

        knowledgeGraphService.createRelationships(relations);
    }

    private record IcdSeed(String code, String descriptionEn, String descriptionCn,
                           String chapterCode, String chapterName, String diseaseName) {}

    private record SymptomSeed(String name, String pinyin, String categoryCode,
                               String categoryName, int frequency) {}

    private record RelSeed(String symptom, String icdCode, int priority) {}

    private static final List<IcdSeed> ICD_SEEDS = List.of(
            new IcdSeed("I10", "Essential (primary) hypertension", "原发性高血压", "I", "循环系统疾病", "原发性高血压"),
            new IcdSeed("I21.9", "Acute myocardial infarction, unspecified", "急性心肌梗死，未特指", "I", "循环系统疾病", "急性心肌梗死"),
            new IcdSeed("I50.9", "Heart failure, unspecified", "心力衰竭，未特指", "I", "循环系统疾病", "心力衰竭"),
            new IcdSeed("J06.9", "Acute upper respiratory infection, unspecified", "急性上呼吸道感染，未特指", "J", "呼吸系统疾病", "急性上呼吸道感染"),
            new IcdSeed("J18.9", "Pneumonia, unspecified organism", "肺炎，未特指病原体", "J", "呼吸系统疾病", "肺炎"),
            new IcdSeed("J44.1", "COPD with acute exacerbation", "慢性阻塞性肺病伴急性加重", "J", "呼吸系统疾病", "慢性阻塞性肺病急性加重"),
            new IcdSeed("E11.9", "Type 2 diabetes mellitus without complications", "2型糖尿病，无并发症", "E", "内分泌营养代谢疾病", "2型糖尿病"),
            new IcdSeed("K21.0", "GERD with esophagitis", "胃食管反流病伴食管炎", "K", "消化系统疾病", "胃食管反流病"),
            new IcdSeed("K35.80", "Unspecified acute appendicitis", "急性阑尾炎，未特指", "K", "消化系统疾病", "急性阑尾炎"),
            new IcdSeed("A41.9", "Sepsis, unspecified organism", "脓毒症，未特指病原体", "A", "某些传染病和寄生虫病", "脓毒症"),
            new IcdSeed("N39.0", "Urinary tract infection, site not specified", "泌尿道感染，部位未特指", "N", "泌尿生殖系统疾病", "泌尿道感染"),
            new IcdSeed("G43.909", "Migraine, unspecified, not intractable", "偏头痛，未特指，非顽固性", "G", "神经系统疾病", "偏头痛"),
            new IcdSeed("J11.1", "Influenza with other respiratory manifestations", "流感伴其他呼吸道表现", "J", "呼吸系统疾病", "流感"),
            new IcdSeed("J45.909", "Unspecified asthma, uncomplicated", "哮喘，未特指，无并发症", "J", "呼吸系统疾病", "哮喘"),
            new IcdSeed("F32.9", "Major depressive disorder, single episode, unspecified", "抑郁症，单次发作，未特指", "F", "精神行为障碍", "抑郁症"),
            new IcdSeed("D64.9", "Anemia, unspecified", "贫血，未特指", "D", "血液造血器官疾病", "贫血"),
            new IcdSeed("M25.50", "Pain in unspecified joint", "未特指关节疼痛", "M", "肌肉骨骼系统", "关节疼痛")
    );

    private static final Map<String, IcdSeed> ICD_BY_CODE = ICD_SEEDS.stream()
            .collect(java.util.stream.Collectors.toMap(IcdSeed::code, i -> i, (a, b) -> a, LinkedHashMap::new));

    private static final List<SymptomSeed> SYMPTOM_SEEDS = List.of(
            new SymptomSeed("发热", "fare", "R", "症状体征", 100),
            new SymptomSeed("咳嗽", "kesou", "R", "症状体征", 95),
            new SymptomSeed("头痛", "toutong", "N", "神经系统", 90),
            new SymptomSeed("胸痛", "xiongtong", "I", "循环系统", 85),
            new SymptomSeed("腹痛", "futong", "K", "消化系统", 80),
            new SymptomSeed("呼吸困难", "huxikunnan", "J", "呼吸系统", 75),
            new SymptomSeed("乏力", "fali", "R", "症状体征", 70),
            new SymptomSeed("恶心", "exin", "K", "消化系统", 65),
            new SymptomSeed("眩晕", "xuanyun", "N", "神经系统", 60),
            new SymptomSeed("关节痛", "guanjietong", "M", "肌肉骨骼", 55)
    );

    private static final List<RelSeed> RELATION_SEEDS = List.of(
            new RelSeed("发热", "J06.9", 1), new RelSeed("发热", "J18.9", 2), new RelSeed("发热", "J11.1", 3),
            new RelSeed("咳嗽", "J18.9", 1), new RelSeed("咳嗽", "J44.1", 2), new RelSeed("咳嗽", "J06.9", 3),
            new RelSeed("头痛", "G43.909", 1), new RelSeed("头痛", "I10", 2),
            new RelSeed("胸痛", "I21.9", 1), new RelSeed("胸痛", "I50.9", 2),
            new RelSeed("腹痛", "K35.80", 1), new RelSeed("腹痛", "K21.0", 2),
            new RelSeed("呼吸困难", "J44.1", 1), new RelSeed("呼吸困难", "J18.9", 2), new RelSeed("呼吸困难", "I50.9", 3),
            new RelSeed("乏力", "D64.9", 1), new RelSeed("乏力", "E11.9", 2),
            new RelSeed("恶心", "K21.0", 1),
            new RelSeed("眩晕", "I10", 1), new RelSeed("眩晕", "G43.909", 2),
            new RelSeed("关节痛", "M25.50", 1)
    );
}
