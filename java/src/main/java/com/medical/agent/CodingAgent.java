package com.medical.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

//1. Coding Agent - 医学编码器（角色类比：医学编码员）
//2. 职责：将诊断和治疗信息映射为ICD-10-CM编码和DRGs分组
//3. 读取：state.diagnosis, state.treatmentPlan；写入：state.codingResult
@Slf4j
@Component
public class CodingAgent {

    //4. 依赖注入LLM服务和JSON解析器
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    //5. System Prompt - 告诉LLM扮演认证编码专家，生成ICD-10编码JSON
    private static final String SYSTEM_PROMPT = """
        You are a certified medical coding specialist (CCS). Given diagnosis and treatment
        information, provide ICD-10-CM codes and DRGs grouping as JSON with: primary_icd10
        (code, description, confidence), secondary_icd10_codes (array), drg_group (drg_code,
        description, weight, mean_los), coding_notes, coding_confidence. Return ONLY valid JSON.
        """;

    public CodingAgent(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    //6. 核心处理方法
    public ClinicalState process(ClinicalState state) {
        log.info("CodingAgent processing");
        state.setCurrentAgent("coding");

        //7. 依赖检查：必须有Diagnosis Agent生成的diagnosis
        if (state.getDiagnosis() == null) {
            state.getErrors().add("No diagnosis available for coding");
            return state;
        }

        try {
            //8. 构建上下文：诊断结果 + 治疗方案
            Map<String, Object> context = Map.of(
                    "diagnosis", state.getDiagnosis(),
                    "treatment_plan", state.getTreatmentPlan() != null ? state.getTreatmentPlan() : Map.of()
            );
            String contextJson = objectMapper.writeValueAsString(context);

            //9. 调用LLM生成ICD-10编码
            String response = llmService.generate(SYSTEM_PROMPT, 
                    "Clinical information:\n\n" + contextJson + "\n\nProvide ICD-10 coding.");

            //10. 清理并解析响应
            String content = cleanJsonResponse(response);
            Map<String, Object> coding = objectMapper.readValue(content, new TypeReference<>() {});
            state.setCodingResult(coding);

            log.info("CodingAgent success");
        } catch (Exception e) {
            log.error("CodingAgent error: {}", e.getMessage());
            state.getErrors().add("Coding error: " + e.getMessage());
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
