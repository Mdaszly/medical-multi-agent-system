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

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }
        String key = RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId;
        for (Message message : messages) {
            try {
                stringRedisTemplate.opsForList().rightPush(key, serialize(message));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize chat message", e);
            }
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        if (conversationId == null || lastN <= 0) {
            return List.of();
        }
        String key = RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return List.of();
        }
        long start = Math.max(0, size - lastN);
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

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }
        stringRedisTemplate.delete(RedisMedicalChatMemoryRepository.KEY_PREFIX + conversationId);
    }

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
