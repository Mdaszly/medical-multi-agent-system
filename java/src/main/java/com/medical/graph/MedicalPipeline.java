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

/**
 * 增强版（{@code medical.ai.chat-type=enhanced}）问诊编排管线：路由 → 记忆优化 → 专科 Agent。
 * <p>
 * 由 {@link com.medical.service.ConsultOrchestrationService} 调用；
 * 同步模式走 {@link #invoke}，流式模式仅走 {@link #routeOnly} 再由编排层直连 LLM。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class MedicalPipeline {

    private final MedicalRouterAgent routerAgent;
    private final MedicalAgentRegistry agentRegistry;
    private final ConsultMemoryService consultMemoryService;

    /**
     * ClinicalState invoke(String rawInput)
     * <p>最简入口：无患者上下文、无会话。</p>
     */
    public ClinicalState invoke(String rawInput) {
        return invoke(rawInput, null, null, null);
    }

    /**
     * ClinicalState invoke(String rawInput, Object patientContext)
     * <p>带患者背景、无会话。</p>
     */
    public ClinicalState invoke(String rawInput, Object patientContext) {
        return invoke(rawInput, patientContext, null, null);
    }

    /**
     * ClinicalState invoke(String rawInput, Object patientContext, String sessionId, Long userId)
     * <p>完整同步链路：构建状态 → Router 分诊 → 记忆优化 → 目标 Agent 推理。</p>
     */
    public ClinicalState invoke(String rawInput, Object patientContext, String sessionId, Long userId) {
        ClinicalState state = buildState(rawInput, patientContext, sessionId, userId);
        state = routerAgent.process(state);
        if (StringUtils.hasText(sessionId)) {
            consultMemoryService.optimizeAfterRoute(sessionId);
        }
        return dispatchToAgent(state);
    }

    /**
     * ClinicalState routeOnly(String rawInput, Object patientContext, String sessionId, Long userId)
     * <p>仅执行路由与记忆优化，不调用专科 Agent（供 SSE 流式问诊使用）。</p>
     */
    public ClinicalState routeOnly(String rawInput, Object patientContext, String sessionId, Long userId) {
        ClinicalState state = buildState(rawInput, patientContext, sessionId, userId);
        state = routerAgent.process(state);
        if (StringUtils.hasText(sessionId)) {
            consultMemoryService.optimizeAfterRoute(sessionId);
        }
        return state;
    }

    /**
     * ClinicalState buildState(String rawInput, Object patientContext, String sessionId, Long userId)
     * <p>组装 {@link ClinicalState}，并向 extensions 注入 patientContext、sessionId、chatHistory。</p>
     */
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

    /**
     * ClinicalState dispatchToAgent(ClinicalState state)
     * <p>根据 extensions 中的 {@code targetAgent} 选择并执行对应 {@link MedicalAgent}。</p>
     */
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

    /**
     * MedicalAgentType resolveTargetAgent(ClinicalState state)
     * <p>解析 Router 写入的 {@code targetAgent}（支持枚举或字符串 code）。</p>
     */
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
