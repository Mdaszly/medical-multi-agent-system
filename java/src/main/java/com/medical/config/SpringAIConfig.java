package com.medical.config;

import com.medical.memory.RedisChatMemory;
import com.medical.tools.MedicalTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ToolCallbackProvider medicalToolCallbackProvider(MedicalTools medicalTools) {
        return MethodToolCallbackProvider.builder().toolObjects(medicalTools).build();
    }

    @Bean(name = "consultChatClient")
    public ChatClient consultChatClient(ChatClient.Builder chatClientBuilder,
                                        MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                        ToolCallbackProvider medicalToolCallbackProvider) {
        return chatClientBuilder
                .defaultAdvisors(messageChatMemoryAdvisor)
                .defaultToolCallbacks(medicalToolCallbackProvider)
                .build();
    }
}
