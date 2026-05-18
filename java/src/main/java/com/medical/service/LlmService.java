package com.medical.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Slf4j
@Service
public class LlmService {

    private final ChatClient chatClient;
    private final ChatClient consultChatClient;
    private final DashScopeService dashScopeService;
    private final String provider;

    public LlmService(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) @Qualifier("consultChatClient") ChatClient consultChatClient,
            DashScopeService dashScopeService,
            @Value("${app.llm.provider:dashscope}") String provider) {
        this.chatClient = chatClientBuilder.build();
        this.consultChatClient = consultChatClient != null ? consultChatClient : this.chatClient;
        this.dashScopeService = dashScopeService;
        this.provider = provider.toLowerCase();
    }

    public String generate(String systemPrompt, String userPrompt) throws Exception {
        log.info("Using LLM provider: {}", provider);
        if ("dashscope".equals(provider) && dashScopeService.isConfigured()) {
            return dashScopeService.generate(systemPrompt, userPrompt);
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    public void generateStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) throws Exception {
        if ("dashscope".equals(provider) && dashScopeService.isConfigured()) {
            dashScopeService.generateStream(systemPrompt, userPrompt, onChunk);
            return;
        }
        Flux<String> flux = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
        flux.doOnNext(onChunk).blockLast();
    }

    public void generateStreamWithMemory(String sessionId, String systemPrompt, String userPrompt,
                                         Consumer<String> onChunk) {
        Flux<String> flux = consultChatClient.prompt()
                .advisors(spec -> spec.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
        flux.doOnNext(onChunk).blockLast();
    }

    public String getProvider() {
        return provider;
    }
}
