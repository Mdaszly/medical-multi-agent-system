package com.medical.graph;

import com.medical.agent.MedicalAgent;
import com.medical.agent.MedicalAgentRegistry;
import com.medical.agent.MedicalRouterAgent;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;
import com.medical.service.ConsultMemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalPipelineTest {

    @Mock
    private MedicalRouterAgent routerAgent;

    @Mock
    private MedicalAgentRegistry agentRegistry;

    @Mock
    private MedicalAgent targetAgent;

    @Mock
    private ConsultMemoryService consultMemoryService;

    @InjectMocks
    private MedicalPipeline medicalPipeline;

    @Test
    void invoke_routesToSpecialtyAgent() {
        when(routerAgent.process(any())).thenAnswer(invocation -> {
            ClinicalState state = invocation.getArgument(0);
            state.getExtensions().put("targetAgent", MedicalAgentType.MEDICATION.getCode());
            return state;
        });
        when(agentRegistry.getAgent(MedicalAgentType.MEDICATION)).thenReturn(targetAgent);
        when(targetAgent.process(any())).thenAnswer(invocation -> {
            ClinicalState state = invocation.getArgument(0);
            state.getExtensions().put("answer", "用药建议");
            return state;
        });

        ClinicalState result = medicalPipeline.invoke("布洛芬和阿司匹林能一起吃吗");

        assertEquals("用药建议", result.getExtensions().get("answer"));
    }
}
