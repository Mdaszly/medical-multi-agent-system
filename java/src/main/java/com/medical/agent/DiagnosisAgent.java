package com.medical.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

//1. Diagnosis Agent - 鉴别诊断器（角色类比：诊断医生）
//2. 职责：基于结构化患者信息生成带置信度的鉴别诊断列表
//3. 读取：state.patientInfo；写入：state.diagnosis, state.needsMoreInfo
@Slf4j
@Component
public class DiagnosisAgent {

    //4. 依赖注入LLM服务和JSON解析器
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    //5. System Prompt - 告诉LLM扮演诊断专家，输出鉴别诊断JSON
    private static final String SYSTEM_PROMPT = """
        You are an expert diagnostician. Given structured patient information, provide a
        comprehensive differential diagnosis as JSON with: primary_diagnosis (disease_name,
        icd10_hint, confidence, evidence array, reasoning), differential_list (array),
        recommended_tests (array), clinical_notes, knowledge_sources, needs_more_info (boolean).
        Confidence scores 0-1. Provide at least 2-3 differentials. Return ONLY valid JSON.
        """;

    public DiagnosisAgent(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    //6. 核心处理方法
    public ClinicalState process(ClinicalState state) {
        log.info("DiagnosisAgent processing");
        state.setCurrentAgent("diagnosis");

        //7. 依赖检查：必须有Intake Agent生成的patientInfo
        if (state.getPatientInfo() == null) {
            state.getErrors().add("No patient info available for diagnosis");
            state.setNeedsMoreInfo(true);  //8. 标记需要补充信息
            return state;
        }

        try {
            //9. 将患者信息序列化为JSON字符串
            String patientJson = objectMapper.writeValueAsString(state.getPatientInfo());

            //10. 调用LLM生成鉴别诊断
            String response = llmService.generate(SYSTEM_PROMPT, 
                    "Patient information:\n\n" + patientJson + "\n\nProvide differential diagnosis.");

            //11. 清理并解析响应
            String content = cleanJsonResponse(response);
            Map<String, Object> diagnosis = objectMapper.readValue(content, new TypeReference<>() {});

            //12. 提取needs_more_info标志（用于Pipeline条件路由）
            Boolean needsMore = (Boolean) diagnosis.remove("needs_more_info");
            state.setDiagnosis(diagnosis);
            state.setNeedsMoreInfo(needsMore != null && needsMore);

            // needs_more_info 是LLM判断是否需要更多患者信息
            // 如果为true，Pipeline会路由到人工问诊节点
            // remove() 既获取值又从Map中删除这个字段（不保存到最终结果）

            //13. 日志输出主诊断结果
            log.info("DiagnosisAgent success, primary: {}",
                    getNestedValue(diagnosis, "primary_diagnosis", "disease_name"));
        } catch (Exception e) {
            log.error("DiagnosisAgent error: {}", e.getMessage());
            state.getErrors().add("Diagnosis error: " + e.getMessage());
            state.setNeedsMoreInfo(false);
        }

        return state;
    }

    // 14. 获取嵌套Map中的值（支持多级key）
    // getNestedValue 防御性编程：避免 NullPointerException，当LLM输出格式不规范时安全降级
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) current = ((Map<String, Object>) current).get(key);
            else return null;
        }
        return current;
    }

    //15. 清理LLM响应中的markdown代码块
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
