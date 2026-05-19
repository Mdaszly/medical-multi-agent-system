package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.AbstractStructuredConsultAgent;
import com.medical.agent.MedicalAgentRegistry;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.constant.ConsultConstant;
import com.medical.graph.MedicalPipeline;
import com.medical.model.ClinicalState;
import com.medical.model.dto.consult.ConsultRequest;
import com.medical.model.vo.ChatSessionVO;
import com.medical.model.vo.ConsultVO;
import com.medical.service.kg.IcdGroundingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class ConsultOrchestrationService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final MedicalPipeline medicalPipeline;
    private final ChatSessionService chatSessionService;
    private final ConsultMemoryService consultMemoryService;
    private final MedicalAgentRegistry medicalAgentRegistry;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final IcdGroundingValidator icdGroundingValidator;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ConsultVO consultSync(ConsultRequest request, Long userId) throws Exception {
        String sessionId = resolveSessionId(userId, request.getSessionId(), request.getScene());

        chatSessionService.saveMessage(
                sessionId, userId, ConsultConstant.ROLE_USER, request.getQuestion(), null, null, null);
        chatSessionService.updateAsync(sessionId, request.getQuestion(), userId);
        consultMemoryService.syncFromDatabase(sessionId, userId);

        ClinicalState state = medicalPipeline.invoke(
                request.getQuestion(), request.getPatientContext(), sessionId, userId);
        icdGroundingValidator.validate(state);
        ConsultVO vo = ConsultVO.fromClinicalState(state, sessionId);
        persistAssistantMessage(sessionId, userId, state, vo);
        return vo;
    }

    public SseEmitter consultStream(ConsultRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        streamExecutor.execute(() -> runStream(request, userId, emitter));
        return emitter;
    }

    private void runStream(ConsultRequest request, Long userId, SseEmitter emitter) {
        try {
            String sessionId = resolveSessionId(userId, request.getSessionId(), request.getScene());
            chatSessionService.saveMessage(
                    sessionId, userId, ConsultConstant.ROLE_USER, request.getQuestion(), null, null, null);
            chatSessionService.updateAsync(sessionId, request.getQuestion(), userId);
            consultMemoryService.syncFromDatabase(sessionId, userId);

            ClinicalState routed = medicalPipeline.routeOnly(
                    request.getQuestion(), request.getPatientContext(), sessionId, userId);

            MedicalAgentType targetType = medicalPipeline.resolveTargetAgent(routed);
            AbstractStructuredConsultAgent agent = medicalAgentRegistry.getStructuredAgent(targetType);
            agent.enrichContextForStream(routed);

            String systemPrompt = agent.getSystemMessage();
            String userPrompt = buildStreamUserPrompt(request, routed);
            StringBuilder buffer = new StringBuilder();

            if ("dashscope".equals(llmService.getProvider())) {
                llmService.generateStream(systemPrompt, userPrompt, chunk -> sendChunk(emitter, buffer, chunk));
            } else {
                llmService.generateStreamWithMemory(sessionId, systemPrompt, userPrompt,
                        chunk -> sendChunk(emitter, buffer, chunk));
            }

            ClinicalState state = ClinicalState.builder()
                    .rawInput(request.getQuestion())
                    .build();
            state.getExtensions().putAll(routed.getExtensions());
            agent.applyLlmResponse(state, buffer.toString());
            icdGroundingValidator.validate(state);

            ConsultVO vo = ConsultVO.fromClinicalState(state, sessionId);
            persistAssistantMessage(sessionId, userId, state, vo);

            emitter.send(SseEmitter.event().name("done").data(vo));
            emitter.complete();
        } catch (Exception e) {
            log.error("Consult stream failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (IOException ignored) {
                // ignore
            }
            emitter.completeWithError(e);
        }
    }

    private void sendChunk(SseEmitter emitter, StringBuilder buffer, String chunk) {
        buffer.append(chunk);
        try {
            emitter.send(SseEmitter.event().name("chunk").data(chunk));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildStreamUserPrompt(ConsultRequest request, ClinicalState routed) {
        StringBuilder sb = new StringBuilder();
        Object history = routed.getExtensions().get("chatHistory");
        if (history instanceof String historyText && StringUtils.hasText(historyText)) {
            sb.append(historyText).append("\n");
        }
        Object toolContext = routed.getExtensions().get("toolContext");
        if (toolContext instanceof String toolText && StringUtils.hasText(toolText)) {
            sb.append("工具检索结果：\n").append(toolText).append("\n");
        }
        sb.append("患者描述：\n").append(request.getQuestion());
        if (request.getPatientContext() != null && !request.getPatientContext().isEmpty()) {
            sb.append("\n患者背景：\n").append(request.getPatientContext());
        }
        return sb.toString();
    }

    private void persistAssistantMessage(String sessionId, Long userId, ClinicalState state, ConsultVO vo)
            throws Exception {
        String metadataJson = state.getExtensions().get("consultResult") != null
                ? objectMapper.writeValueAsString(state.getExtensions().get("consultResult"))
                : null;
        chatSessionService.saveMessage(
                sessionId,
                userId,
                ConsultConstant.ROLE_ASSISTANT,
                vo.getAnswer() != null ? vo.getAnswer() : "",
                state.getCurrentAgent(),
                vo.getRiskLevel(),
                metadataJson
        );
    }

    private String resolveSessionId(Long userId, String sessionId, String scene) {
        if (StringUtils.hasText(sessionId)) {
            chatSessionService.getSessionForUser(sessionId, userId);
            return sessionId;
        }
        ChatSessionVO created = chatSessionService.createSession(userId, scene, null);
        return created.getSessionId();
    }
}
