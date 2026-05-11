package com.medical.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

//1. Intake Agent - 患者信息采集器（角色类比：问诊护士）
//2. 职责：将自然语言患者描述转换为结构化JSON数据
//3. 读取：state.rawInput；写入：state.patientInfo
@Slf4j
@Component
public class IntakeAgent {

    //4. 依赖注入LLM服务和JSON解析器
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    //5. System Prompt - 告诉LLM扮演医学问诊专家，输出指定格式的JSON
    private static final String SYSTEM_PROMPT = """
        You are an expert medical intake specialist. Extract structured patient information
        from the clinical narrative as a JSON object with fields: name, age, gender,
        chief_complaint, symptoms (array), medical_history (array), allergies (array),
        current_medications (array), vital_signs (object), lab_results (array).
        Return ONLY valid JSON, no markdown fences.
        """;

    public IntakeAgent(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    //6. 核心处理方法：接收ClinicalState，处理后返回更新的状态
    public ClinicalState process(ClinicalState state) {
        log.info("IntakeAgent processing, input length: {}", state.getRawInput().length());
        state.setCurrentAgent("intake");  //7. 标记当前执行的Agent

        //8. 输入验证：检查原始输入是否为空
        if (state.getRawInput() == null || state.getRawInput().isBlank()) {
            state.getErrors().add("No raw input provided to Intake Agent");
            return state;
        }

        try {
            //9. 调用LLM生成结构化患者信息
            String response = llmService.generate(SYSTEM_PROMPT, "Patient narrative:\n\n" + state.getRawInput());

            //10. 清理LLM响应（移除markdown代码块围栏）
            String content = cleanJsonResponse(response);
            
            //11. 解析JSON为Map结构
            Map<String, Object> patientInfo = objectMapper.readValue(content, new TypeReference<>() {});

            //12. 将结果写入状态
            state.setPatientInfo(patientInfo);
            log.info("IntakeAgent success, patient: {}", patientInfo.getOrDefault("name", "Unknown"));
        } catch (Exception e) {
            //13. 错误处理：记录错误但不中断Pipeline（优雅降级）
            log.error("IntakeAgent error: {}", e.getMessage());
            state.getErrors().add("Intake error: " + e.getMessage());
        }

        return state;
    }

    //14. 清理LLM返回的JSON响应，处理markdown代码块格式
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
