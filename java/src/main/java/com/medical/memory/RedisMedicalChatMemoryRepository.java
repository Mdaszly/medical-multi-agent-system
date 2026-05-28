package com.medical.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 问诊 Redis 会话记忆的运维操作（与 {@link RedisChatMemory} 共用键前缀）。
 * <p>
 * 路由 Agent 每次分类后会调用 {@link #optimize}，从 List 尾部弹出最近 2 条记录，
 * 避免 Router 的 user/assistant 回合占用 Spring AI 记忆窗口。
 * </p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisMedicalChatMemoryRepository implements MedicalChatMemoryRepository {

    /** Redis 键前缀，完整键为 {@code medical:chat:memory:{sessionId}} */
    public static final String KEY_PREFIX = "medical:chat:memory:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * void optimize(String conversationId)
     * <p>路由完成后裁剪记忆：若 List 长度 ≥ 2，则从右端弹出 2 条（通常为刚写入的路由问答）。</p>
     */
    @Override
    public void optimize(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        // LLEN：判断是否有足够条目需要裁剪
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size != null && size >= 2) {
            // RPOP count：从 List 右端批量弹出 2 条，去掉 Router 阶段临时占用的消息
            stringRedisTemplate.opsForList().rightPop(key, 2);
            log.debug("Optimized chat memory for {}, removed 2 records", conversationId);
        }
    }
}
