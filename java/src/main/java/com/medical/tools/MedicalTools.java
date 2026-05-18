package com.medical.tools;

import com.medical.service.AppointmentGuideService;
import com.medical.service.DrugInteractionService;
import com.medical.service.Icd10Service;
import com.medical.service.MedicalKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class MedicalTools {

    private final Icd10Service icd10Service;
    private final DrugInteractionService drugInteractionService;
    private final MedicalKnowledgeService medicalKnowledgeService;
    private final AppointmentGuideService appointmentGuideService;

    @Tool(description = "根据疾病名称查询 ICD-10 编码")
    public String queryIcd10(String diseaseName) {
        return icd10Service.searchAsText(diseaseName);
    }

    @Tool(description = "检查新药与当前用药的药物相互作用")
    public String checkDrugInteraction(String newDrugs, String currentDrugs) {
        List<String> newList = splitDrugs(newDrugs);
        List<String> currentList = splitDrugs(currentDrugs);
        return drugInteractionService.checkAsText(newList, currentList);
    }

    @Tool(description = "检索医学知识库")
    public String searchMedicalKnowledge(String query) {
        return medicalKnowledgeService.search(query);
    }

    @Tool(description = "查询科室可预约医生")
    public String queryAppointmentSlots(String department) {
        return appointmentGuideService.suggestDoctors(department);
    }

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
