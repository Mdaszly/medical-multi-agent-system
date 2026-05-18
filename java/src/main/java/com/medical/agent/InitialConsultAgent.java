package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.constant.ConsultConstant;
import com.medical.service.LlmService;
import org.springframework.stereotype.Component;

@Component
public class InitialConsultAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位专业的内科初诊医生。根据患者描述给出预问诊建议。
        可选科室：%s
        
        只返回 JSON，字段如下：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "从科室列表中选择一个",
          "conclusion": "一句话结论",
          "reasoning": "推理依据",
          "red_flags": ["需立即就医的信号"],
          "next_questions": ["需补充追问的问题"],
          "care_advice": ["健康科普建议"],
          "evidence_summary": "证据摘要"
        }
        """.formatted(String.join("、", ConsultConstant.DEPARTMENTS));

    public InitialConsultAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.INITIAL;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
