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

@Configuration
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
public class SpringAIConfig {

    @Bean
    public ChatMemory chatMemory(RedisChatMemory redisChatMemory) {
        return redisChatMemory;
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return new MessageChatMemoryAdvisor(chatMemory);
    }

    @Bean
    public ToolCallback[] medicalToolCallbacks(MedicalTools medicalTools) {
        return MethodToolCallbackProvider.builder().toolObjects(medicalTools).build().getToolCallbacks();
    }

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
