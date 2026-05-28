package com.medical.tools;

import com.medical.service.AppointmentGuideService;
import com.medical.service.DrugInteractionService;
import com.medical.service.Icd10Service;
import com.medical.service.MedicalKnowledgeService;
import com.medical.service.kg.KnowledgeGraphFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Spring AI 工具集：在 LLM 推理过程中按需调用 ICD、知识图谱、药物相互作用、本地知识库、挂号等能力。
 * <p>由 {@link com.medical.config.SpringAIConfig} 注册到 {@code consultChatClient}。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class MedicalTools {

    private final Icd10Service icd10Service;
    private final DrugInteractionService drugInteractionService;
    private final MedicalKnowledgeService medicalKnowledgeService;
    private final AppointmentGuideService appointmentGuideService;
    private final KnowledgeGraphFacade knowledgeGraphFacade;

    /**
     * String querySymptomDiagnosis(String symptomName)
     * <p>Neo4j 知识图谱：症状 → 可能疾病与 ICD-10。</p>
     */
    @Tool(description = "根据症状名称查询知识图谱中的可能疾病与 ICD-10 编码")
    public String querySymptomDiagnosis(String symptomName) {
        return knowledgeGraphFacade.querySymptomDiagnosisAsText(symptomName);
    }

    /**
     * String suggestSymptoms(String prefix)
     * <p>根据前缀联想标准症状名（图谱侧）。</p>
     */
    @Tool(description = "根据前缀联想标准症状名称")
    public String suggestSymptoms(String prefix) {
        return knowledgeGraphFacade.suggestSymptomsAsText(prefix);
    }

    /**
     * String lookupIcd10ByCode(String code)
     * <p>ICD 编码反查：优先图谱，未命中则回落到本地 ICD-10 表。</p>
     */
    @Tool(description = "根据 ICD-10 编码反查疾病信息（优先知识图谱）")
    public String lookupIcd10ByCode(String code) {
        String graph = knowledgeGraphFacade.lookupIcdAsText(code);
        if (graph != null && !graph.startsWith("图谱中未找到")) {
            return graph;
        }
        var vo = icd10Service.lookupByCode(code);
        if (vo != null) {
            return vo.getCode() + " - " + vo.getDescription();
        }
        return graph;
    }

    /**
     * String queryIcd10(String diseaseName)
     * <p>疾病名查 ICD：优先图谱症状-诊断链，否则本地 ICD 检索。</p>
     */
    @Tool(description = "根据疾病名称查询 ICD-10 编码（优先知识图谱）")
    public String queryIcd10(String diseaseName) {
        String graphText = knowledgeGraphFacade.querySymptomDiagnosisAsText(diseaseName);
        if (graphText != null && graphText.contains("ICD:")) {
            return graphText;
        }
        return icd10Service.searchAsText(diseaseName);
    }

    /**
     * String checkDrugInteraction(String newDrugs, String currentDrugs)
     * <p>解析逗号/顿号分隔的药名列表并检查相互作用。</p>
     */
    @Tool(description = "检查新药与当前用药的药物相互作用")
    public String checkDrugInteraction(String newDrugs, String currentDrugs) {
        List<String> newList = splitDrugs(newDrugs);
        List<String> currentList = splitDrugs(currentDrugs);
        return drugInteractionService.checkAsText(newList, currentList);
    }

    /**
     * String searchMedicalKnowledge(String query)
     * <p>检索内置静态知识库（{@link com.medical.service.impl.MedicalKnowledgeServiceImpl}）。</p>
     */
    @Tool(description = "检索医学知识库")
    public String searchMedicalKnowledge(String query) {
        return medicalKnowledgeService.search(query);
    }

    /**
     * String queryAppointmentSlots(String department)
     * <p>按科室推荐可预约医生（挂号引导）。</p>
     */
    @Tool(description = "查询科室可预约医生")
    public String queryAppointmentSlots(String department) {
        return appointmentGuideService.suggestDoctors(department);
    }

    /**
     * List&lt;String&gt; splitDrugs(String raw)
     * <p>将中英文分隔符拆分为药名列表。</p>
     */
    private List<String> splitDrugs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，、;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
