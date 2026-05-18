package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MedicalRouterAgent extends BaseMedicalAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位专业的医疗问诊分诊助手。请根据用户的问诊内容，判断最适合的处理方向。
        
        请分析用户输入，返回以下类型之一（只返回类型代码，不要其他内容）：
        - INITIAL: 初次就诊咨询，症状描述，疾病询问
        - FOLLOWUP: 复诊咨询，病史回顾，治疗方案调整
        - MEDICATION: 用药咨询，药物信息，用法用量，副作用
        - REPORT: 报告解读，检验/检查结果分析
        - HEALTH: 健康咨询，生活方式，预防保健
        - APPOINTMENT: 挂号咨询，科室推荐，医生推荐
        
        仅返回类型代码，不要任何解释。
        """;

    public MedicalRouterAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    public MedicalAgentType getAgentType() {
        return MedicalAgentType.ROUTER;
    }

    @Override
    public String getSystemMessage() {
        return SYSTEM_PROMPT;
    }

    @Override
    public ClinicalState process(ClinicalState state) {
        log.info("MedicalRouterAgent processing");
        state.setCurrentAgent(MedicalAgentType.ROUTER.getCode());
        appendTrace(state, "RouterAgent", "route", "识别问诊意图");

        try {
            String response = generate(SYSTEM_PROMPT, "用户问诊内容：\n\n" + state.getRawInput());
            MedicalAgentType agentType = MedicalAgentType.fromCode(response);
            if (agentType == null) {
                agentType = MedicalAgentType.INITIAL;
            }
            state.getExtensions().put("targetAgent", agentType.getCode());
            appendTrace(state, "RouterAgent", "route", "路由到 " + agentType.getCode());
            log.info("Routing decision: {}", agentType);
        } catch (Exception e) {
            log.error("Router error", e);
            state.getErrors().add("Router error: " + e.getMessage());
            state.getExtensions().put("targetAgent", MedicalAgentType.INITIAL.getCode());
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private void appendTrace(ClinicalState state, String agent, String action, String detail) {
        List<Map<String, String>> trace = (List<Map<String, String>>) state.getExtensions()
                .computeIfAbsent("agentTrace", k -> new ArrayList<>());
        Map<String, String> item = new HashMap<>();
        item.put("agent", agent);
        item.put("action", action);
        item.put("detail", detail);
        trace.add(item);
    }
}
