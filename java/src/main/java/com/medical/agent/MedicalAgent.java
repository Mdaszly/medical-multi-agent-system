package com.medical.agent;

import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;

public interface MedicalAgent {

    MedicalAgentType getAgentType();

    ClinicalState process(ClinicalState state);

    String getSystemMessage();
}
