package com.medical.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisMedicalChatMemoryRepository implements MedicalChatMemoryRepository {

    public static final String KEY_PREFIX = "medical:chat:memory:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void optimize(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size != null && size >= 2) {
            stringRedisTemplate.opsForList().rightPop(key, 2);
            log.debug("Optimized chat memory for {}, removed 2 records", conversationId);
        }
    }
}
