package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;
import com.medical.service.DrugInteractionService;
import com.medical.service.LlmService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class MedicationAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位临床药师。回答用药咨询，关注用法用量、注意事项与药物相互作用。
        结合提供的药物相互作用检查结果给出建议。
        
        只返回 JSON：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "药学门诊或相关科室",
          "conclusion": "用药建议摘要",
          "reasoning": "依据",
          "red_flags": ["用药风险"],
          "next_questions": ["需确认的信息"],
          "care_advice": ["用药注意事项"],
          "evidence_summary": "证据摘要",
          "medication_notes": ["具体用药提示"]
        }
        """;

    private final DrugInteractionService drugInteractionService;

    public MedicationAgent(LlmService llmService, ObjectMapper objectMapper,
                           DrugInteractionService drugInteractionService) {
        super(llmService, objectMapper);
        this.drugInteractionService = drugInteractionService;
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.MEDICATION;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected void enrichContext(ClinicalState state) {
        List<String> newDrugs = extractDrugNames(state.getRawInput());
        List<String> current = extractCurrentMeds(state);
        String ddi = drugInteractionService.checkAsText(newDrugs, current);
        state.getExtensions().put("toolContext", "药物相互作用检查：\n" + ddi);
        appendTrace(state, "Tool", "checkDrugInteraction", "完成 DDI 规则检查");
    }

    private List<String> extractDrugNames(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> known = List.of("布洛芬", "阿司匹林", "二甲双胍", "华法林", "奥美拉唑",
                "阿莫西林", "头孢", "降压药", "胰岛素", "ibuprofen", "aspirin", "metformin", "warfarin");
        List<String> found = new ArrayList<>();
        for (String drug : known) {
            if (text.toLowerCase().contains(drug.toLowerCase())) {
                found.add(drug);
            }
        }
        if (found.isEmpty()) {
            found.add(text.length() > 30 ? text.substring(0, 30) : text);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCurrentMeds(ClinicalState state) {
        Object ctx = state.getExtensions().get("patientContext");
        if (ctx instanceof Map<?, ?> map) {
            Object meds = map.get("medication_history");
            if (meds == null) {
                meds = map.get("medicationHistory");
            }
            if (meds instanceof String s && StringUtils.hasText(s)) {
                return Arrays.stream(s.split("[,，、;；]"))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
            }
        }
        return List.of();
    }
}
