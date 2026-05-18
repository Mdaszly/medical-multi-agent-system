package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.constant.ConsultConstant;
import com.medical.model.ClinicalState;
import com.medical.service.AppointmentGuideService;
import com.medical.service.LlmService;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AppointmentGuideAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位智能导诊助手。根据症状推荐科室，并结合提供的可预约医生信息给出挂号引导。
        可选科室：%s
        
        只返回 JSON：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "推荐科室",
          "conclusion": "导诊结论",
          "reasoning": "依据",
          "red_flags": ["需急诊信号"],
          "next_questions": ["需补充症状"],
          "care_advice": ["就诊前准备"],
          "evidence_summary": "证据摘要",
          "booking_tips": ["预约步骤提示"]
        }
        """.formatted(String.join("、", ConsultConstant.DEPARTMENTS));

    private final AppointmentGuideService appointmentGuideService;

    public AppointmentGuideAgent(LlmService llmService, ObjectMapper objectMapper,
                                 AppointmentGuideService appointmentGuideService) {
        super(llmService, objectMapper);
        this.appointmentGuideService = appointmentGuideService;
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.APPOINTMENT;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected void enrichContext(ClinicalState state) {
        String dept = guessDepartment(state.getRawInput());
        String guide = appointmentGuideService.suggestDoctors(dept);
        state.getExtensions().put("toolContext", guide);
        appendTrace(state, "Tool", "queryAppointmentSlots", "查询科室可预约医生");
    }

    private String guessDepartment(String text) {
        return Arrays.stream(ConsultConstant.DEPARTMENTS)
                .filter(text::contains)
                .findFirst()
                .orElse("内科");
    }
}
