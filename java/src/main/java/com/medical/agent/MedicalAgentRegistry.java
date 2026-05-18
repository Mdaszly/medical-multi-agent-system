package com.medical.agent;

import com.medical.agent.enums.MedicalAgentType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class MedicalAgentRegistry {

    private final InitialConsultAgent initialConsultAgent;
    private final FollowupConsultAgent followupConsultAgent;
    private final MedicationAgent medicationAgent;
    private final ReportInterpretAgent reportInterpretAgent;
    private final HealthConsultAgent healthConsultAgent;
    private final AppointmentGuideAgent appointmentGuideAgent;

    public MedicalAgent getAgent(MedicalAgentType type) {
        if (type == null) {
            return initialConsultAgent;
        }
        return switch (type) {
            case INITIAL -> initialConsultAgent;
            case FOLLOWUP -> followupConsultAgent;
            case MEDICATION -> medicationAgent;
            case REPORT -> reportInterpretAgent;
            case HEALTH -> healthConsultAgent;
            case APPOINTMENT -> appointmentGuideAgent;
            default -> initialConsultAgent;
        };
    }

    public AbstractStructuredConsultAgent getStructuredAgent(MedicalAgentType type) {
        MedicalAgent agent = getAgent(type);
        if (agent instanceof AbstractStructuredConsultAgent structured) {
            return structured;
        }
        return initialConsultAgent;
    }
}
