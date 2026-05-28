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

/**
 * 线上问诊「双写记忆」协调层：MySQL 为权威存储，Redis 为 LLM 推理热缓存。
 * <ul>
 *   <li>Prompt 历史：直接从 DB 取最近 N 条，格式化为 {@code chatHistory} 扩展字段</li>
 *   <li>Spring AI 记忆：每次问诊前 {@link #syncFromDatabase} 清空并重建 Redis List</li>
 *   <li>路由后：{@link #optimizeAfterRoute} 裁剪 Router 临时消息</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ConsultMemoryService {

    /** 从 DB 同步到 Redis 的最大条数 */
    private static final int MEMORY_LAST_N = 20;
    /** 拼入 Agent Prompt 的历史条数 */
    private static final int HISTORY_PROMPT_LIMIT = 10;

    private final ChatSessionService chatSessionService;
    private final RedisChatMemory redisChatMemory;
    private final MedicalChatMemoryRepository memoryRepository;

    /**
     * String loadHistoryPrompt(String sessionId, Long userId)
     * <p>加载最近 {@link #HISTORY_PROMPT_LIMIT} 条 DB 消息并格式化为 Prompt 文本（不读 Redis）。</p>
     */
    public String loadHistoryPrompt(String sessionId, Long userId) {
        List<ChatMessageVO> messages = chatSessionService.listMessages(sessionId, userId, HISTORY_PROMPT_LIMIT);
        return ConsultHistoryFormatter.format(messages);
    }

    /**
     * void syncFromDatabase(String sessionId, Long userId)
     * <p>问诊开始前：先 {@link RedisChatMemory#clear}，再将 DB 最近 {@link #MEMORY_LAST_N} 条灌入 Redis。</p>
     */
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

    /**
     * void optimizeAfterRoute(String sessionId)
     * <p>Router 分类完成后，委托 {@link MedicalChatMemoryRepository#optimize} 弹出 Redis 尾部 2 条。</p>
     */
    public void optimizeAfterRoute(String sessionId) {
        memoryRepository.optimize(sessionId);
    }
}
