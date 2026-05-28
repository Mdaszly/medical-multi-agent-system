package com.medical.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis List 的 Spring AI {@link ChatMemory} 实现，供线上问诊多轮对话使用。
 * <p>
 * 每个会话对应一个 Redis 键 {@code medical:chat:memory:{conversationId}}，消息按时间顺序存入 List；
 * 与 {@link ConsultMemoryService} 配合：持久化消息在 MySQL，推理前通过 {@code syncFromDatabase} 灌入本组件。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * void add(String conversationId, List&lt;Message&gt; messages)
     * <p>将一批聊天消息追加到会话 Redis List 尾部（保持对话时间序）。</p>
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }
        String key = RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId;
        for (Message message : messages) {
            try {
                // RPUSH：在 List 右端追加一条 JSON 消息，等价于“按时间顺序记下本轮 user/assistant 内容”
                stringRedisTemplate.opsForList().rightPush(key, serialize(message));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize chat message", e);
            }
        }
    }

    /**
     * List&lt;Message&gt; get(String conversationId, int lastN)
     * <p>读取会话最近 {@code lastN} 条消息，供 Spring AI Advisor 或带记忆的 LLM 调用使用。</p>
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        if (conversationId == null || lastN <= 0) {
            return List.of();
        }
        String key = RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId;
        // LLEN：获取当前 List 长度，用于计算“只取最后 N 条”的起始下标
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return List.of();
        }
        long start = Math.max(0, size - lastN);
        // LRANGE start end：按索引区间取出片段（含首尾），此处为 List 中最近 lastN 条原始 JSON
        List<String> raw = stringRedisTemplate.opsForList().range(key, start, size - 1);
        if (raw == null) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>();
        for (String item : raw) {
            Message message = deserialize(item);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * void clear(String conversationId)
     * <p>删除整个会话的 Redis 记忆键（通常在从 DB 全量重灌前调用）。</p>
     */
    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }
        // DEL：移除该会话对应的 List 键，清空 Redis 侧对话缓存
        stringRedisTemplate.delete(RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId);
    }

    /**
     * String serialize(Message message) throws JsonProcessingException
     * <p>将 Spring AI {@link Message} 转为 {@code {"role","content"}} JSON 字符串存入 Redis。</p>
     */
    private String serialize(Message message) throws JsonProcessingException {
        String role;
        if (message instanceof AssistantMessage) {
            role = "assistant";
        } else if (message instanceof SystemMessage) {
            role = "system";
        } else {
            role = "user";
        }
        return objectMapper.writeValueAsString(Map.of(
                "role", role,
                "content", message.getText()
        ));
    }

    /**
     * Message deserialize(String json)
     * <p>将 Redis 中的 JSON 还原为 {@link UserMessage} / {@link AssistantMessage} / {@link SystemMessage}。</p>
     */
    @SuppressWarnings("unchecked")
    private Message deserialize(String json) {
        try {
            Map<String, String> map = objectMapper.readValue(json, Map.class);
            String role = map.getOrDefault("role", "user");
            String content = map.getOrDefault("content", "");
            return switch (role) {
                case "assistant" -> new AssistantMessage(content);
                case "system" -> new SystemMessage(content);
                default -> new UserMessage(content);
            };
        } catch (Exception e) {
            log.warn("Failed to deserialize chat message: {}", json, e);
            return null;
        }
    }
}
