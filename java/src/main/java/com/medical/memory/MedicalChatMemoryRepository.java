package com.medical.memory;

/**
 * 问诊会话 Redis 记忆的扩展操作（与 Spring AI {@link org.springframework.ai.chat.memory.ChatMemory} 解耦）。
 */
public interface MedicalChatMemoryRepository {

    /**
     * void optimize(String conversationId)
     * <p>路由后裁剪 Redis List，释放 Router 轮次占用的槽位。</p>
     */
    void optimize(String conversationId);
}
