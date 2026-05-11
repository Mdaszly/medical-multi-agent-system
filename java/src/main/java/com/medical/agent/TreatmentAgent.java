package com.medical.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

//1. Treatment Agent - 治疗方案推荐器（角色类比：临床药剂师）
//2. 职责：根据诊断和患者信息生成循证治疗方案，检查药物交互
//3. 读取：state.patientInfo, state.diagnosis；写入：state.treatmentPlan
@Slf4j
@Component
public class TreatmentAgent {

    //4. 依赖注入LLM服务和JSON解析器
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    //5. System Prompt - 告诉LLM扮演临床药剂师，生成治疗方案JSON
    private static final String SYSTEM_PROMPT = """
        You are an expert clinical pharmacologist. Given diagnosis and patient data, provide
        a treatment plan as JSON with: diagnosis_addressed, medications (array with drug_name,
        generic_name, dosage, route, frequency, duration, contraindications, side_effects),
        drug_interactions (array with drug_a, drug_b, severity, description, recommendation),
        non_drug_treatments, lifestyle_recommendations, follow_up_plan, warnings,
        evidence_references. Check current medications for interactions. Return ONLY valid JSON.
        """;

    public TreatmentAgent(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    //6. 核心处理方法
    public ClinicalState process(ClinicalState state) {
        log.info("TreatmentAgent processing");
        state.setCurrentAgent("treatment");

        //7. 依赖检查：必须有Diagnosis Agent生成的diagnosis
        if (state.getDiagnosis() == null) {
            state.getErrors().add("No diagnosis available for treatment planning");
            return state;
        }

        try {
            //8. 构建上下文：患者信息 + 诊断结果
            Map<String, Object> context = Map.of(
                    "patient_info", state.getPatientInfo() != null ? state.getPatientInfo() : Map.of(),
                    "diagnosis", state.getDiagnosis()
            );
            String contextJson = objectMapper.writeValueAsString(context);

            //9. 调用LLM生成治疗方案
            String response = llmService.generate(SYSTEM_PROMPT, 
                    "Clinical context:\n\n" + contextJson + "\n\nProvide treatment plan.");

            //10. 清理并解析响应
            String content = cleanJsonResponse(response);
            Map<String, Object> treatment = objectMapper.readValue(content, new TypeReference<>() {});
            state.setTreatmentPlan(treatment);

            log.info("TreatmentAgent success");
        } catch (Exception e) {
            log.error("TreatmentAgent error: {}", e.getMessage());
            state.getErrors().add("Treatment error: " + e.getMessage());
        }

        return state;
    }

    //11. 清理LLM响应中的markdown代码块
    private String cleanJsonResponse(String response) {
        String content = response.trim();
        if (content.startsWith("```")) {
            content = content.substring(content.indexOf('\n') + 1);
            int lastFence = content.lastIndexOf("```");
            if (lastFence >= 0) content = content.substring(0, lastFence).trim();
        }
        return content;
    }
}
