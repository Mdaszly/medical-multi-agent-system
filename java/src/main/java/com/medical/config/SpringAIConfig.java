package com.medical.config;

import com.medical.memory.RedisChatMemory;
import com.medical.tools.MedicalTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 增强版问诊的 Spring AI 装配：Redis 会话记忆、记忆 Advisor、医学工具集、默认 ChatClient。
 */
@Configuration
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class SpringAIConfig {

    /**
     * ChatMemory chatMemory(RedisChatMemory redisChatMemory)
     * <p>将 {@link RedisChatMemory} 注册为全局 {@link ChatMemory} 实现。</p>
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemory redisChatMemory) {
        return redisChatMemory;
    }

    /**
     * MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory)
     * <p>在每次 ChatClient 调用前后自动读写 Redis 中的多轮消息。</p>
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return new MessageChatMemoryAdvisor(chatMemory);
    }

    /**
     * ToolCallback[] medicalToolCallbacks(MedicalTools medicalTools)
     * <p>把 {@link MedicalTools} 中带 {@code @Tool} 的方法注册为 LLM 可调用工具。</p>
     */
    @Bean
    public ToolCallback[] medicalToolCallbacks(MedicalTools medicalTools) {
        return MethodToolCallbackProvider.builder().toolObjects(medicalTools).build().getToolCallbacks();
    }

    /**
     * ChatClient consultChatClient(ChatClient.Builder, MessageChatMemoryAdvisor, ToolCallback[])
     * <p>问诊专用 ChatClient：默认挂载记忆 Advisor 与医学工具。</p>
     */
    @Bean(name = "consultChatClient")
    public ChatClient consultChatClient(ChatClient.Builder chatClientBuilder,
                                        MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                        ToolCallback[] medicalToolCallbacks) {
        return chatClientBuilder
                .defaultAdvisors(messageChatMemoryAdvisor)
                .defaultTools(medicalToolCallbacks)
                .build();
    }
}
