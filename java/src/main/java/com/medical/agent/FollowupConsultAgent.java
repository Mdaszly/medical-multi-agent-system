package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.constant.ConsultConstant;
import com.medical.model.ClinicalState;
import com.medical.service.Icd10Service;
import com.medical.service.LlmService;
import org.springframework.stereotype.Component;

@Component
public class FollowupConsultAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位复诊随访医生。结合患者既往病史与当前描述，评估病情变化与用药调整方向。
        可选科室：%s
        
        只返回 JSON：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "科室",
          "conclusion": "复诊结论",
          "reasoning": "与既往对比的推理",
          "red_flags": ["需急诊信号"],
          "next_questions": ["需追问"],
          "care_advice": ["随访建议"],
          "evidence_summary": "证据摘要",
          "followup_plan": ["复查项目或复诊时间建议"]
        }
        """.formatted(String.join("、", ConsultConstant.DEPARTMENTS));

    private final Icd10Service icd10Service;

    public FollowupConsultAgent(LlmService llmService, ObjectMapper objectMapper, Icd10Service icd10Service) {
        super(llmService, objectMapper);
        this.icd10Service = icd10Service;
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.FOLLOWUP;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected void enrichContext(ClinicalState state) {
        String icd = icd10Service.searchAsText(state.getRawInput());
        appendToolContext(state, "ICD-10 参考：\n" + icd);
        appendTrace(state, "Tool", "queryIcd10", "检索疾病编码参考");
    }

    private void appendToolContext(ClinicalState state, String text) {
        Object existing = state.getExtensions().get("toolContext");
        String merged = existing instanceof String s ? s + "\n" + text : text;
        state.getExtensions().put("toolContext", merged);
    }
}
