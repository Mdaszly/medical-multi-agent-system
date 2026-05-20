package com.medical.graph;



import com.medical.agent.MedicalAgent;

import com.medical.agent.MedicalAgentRegistry;

import com.medical.agent.MedicalRouterAgent;

import com.medical.agent.enums.MedicalAgentType;

import com.medical.model.ClinicalState;

import com.medical.service.ConsultMemoryService;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;



@Slf4j

@Component

@RequiredArgsConstructor

@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")

public class MedicalPipeline {



    private final MedicalRouterAgent routerAgent;

    private final MedicalAgentRegistry agentRegistry;

    private final ConsultMemoryService consultMemoryService;



    public ClinicalState invoke(String rawInput) {

        return invoke(rawInput, null, null, null);

    }



    public ClinicalState invoke(String rawInput, Object patientContext) {

        return invoke(rawInput, patientContext, null, null);

    }



    public ClinicalState invoke(String rawInput, Object patientContext, String sessionId, Long userId) {

        ClinicalState state = buildState(rawInput, patientContext, sessionId, userId);

        state = routerAgent.process(state);

        if (StringUtils.hasText(sessionId)) {

            consultMemoryService.optimizeAfterRoute(sessionId);

        }

        return dispatchToAgent(state);

    }



    public ClinicalState routeOnly(String rawInput, Object patientContext, String sessionId, Long userId) {

        ClinicalState state = buildState(rawInput, patientContext, sessionId, userId);

        state = routerAgent.process(state);

        if (StringUtils.hasText(sessionId)) {

            consultMemoryService.optimizeAfterRoute(sessionId);

        }

        return state;

    }



    private ClinicalState buildState(String rawInput, Object patientContext, String sessionId, Long userId) {

        ClinicalState state = ClinicalState.builder()

                .rawInput(rawInput)

                .build();

        if (patientContext != null) {

            state.getExtensions().put("patientContext", patientContext);

        }

        if (StringUtils.hasText(sessionId)) {

            state.getExtensions().put("sessionId", sessionId);

            if (userId != null) {

                state.getExtensions().put("chatHistory", consultMemoryService.loadHistoryPrompt(sessionId, userId));

            }

        }

        return state;

    }



    private ClinicalState dispatchToAgent(ClinicalState state) {

        MedicalAgentType targetType = resolveTargetAgent(state);

        MedicalAgent targetAgent = agentRegistry.getAgent(targetType);

        if (targetAgent != null) {

            state = targetAgent.process(state);

        } else {

            log.warn("Unknown agent type {}, fallback to initial", targetType);

            state = agentRegistry.getAgent(MedicalAgentType.INITIAL).process(state);

        }

        return state;

    }



    public MedicalAgentType resolveTargetAgent(ClinicalState state) {

        Object target = state.getExtensions().get("targetAgent");

        if (target instanceof MedicalAgentType type) {

            return type;

        }

        if (target instanceof String code) {

            MedicalAgentType parsed = MedicalAgentType.fromCode(code);

            return parsed != null ? parsed : MedicalAgentType.INITIAL;

        }

        return MedicalAgentType.INITIAL;

    }

}


