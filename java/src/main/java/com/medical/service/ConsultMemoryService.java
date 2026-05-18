package com.medical.service;

import com.medical.constant.ConsultConstant;
import com.medical.memory.RedisChatMemory;
import com.medical.model.vo.ChatMessageVO;
import com.medical.memory.ConsultHistoryFormatter;
import com.medical.memory.MedicalChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultMemoryService {

    private static final int MEMORY_LAST_N = 20;
    private static final int HISTORY_PROMPT_LIMIT = 10;

    private final ChatSessionService chatSessionService;
    private final RedisChatMemory redisChatMemory;
    private final MedicalChatMemoryRepository memoryRepository;

    public String loadHistoryPrompt(String sessionId, Long userId) {
        List<ChatMessageVO> messages = chatSessionService.listMessages(sessionId, userId, HISTORY_PROMPT_LIMIT);
        return ConsultHistoryFormatter.format(messages);
    }

    public void syncFromDatabase(String sessionId, Long userId) {
        List<ChatMessageVO> messages = chatSessionService.listMessages(sessionId, userId, MEMORY_LAST_N);
        redisChatMemory.clear(sessionId);
        if (messages.isEmpty()) {
            return;
        }
        List<Message> aiMessages = new ArrayList<>();
        for (ChatMessageVO vo : messages) {
            if (ConsultConstant.ROLE_ASSISTANT.equals(vo.getRole())) {
                aiMessages.add(new AssistantMessage(vo.getContent()));
            } else {
                aiMessages.add(new UserMessage(vo.getContent()));
            }
        }
        redisChatMemory.add(sessionId, aiMessages);
    }

    public void optimizeAfterRoute(String sessionId) {
        memoryRepository.optimize(sessionId);
    }
}
