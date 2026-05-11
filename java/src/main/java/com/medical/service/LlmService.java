package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//1. LLM服务抽象层，统一管理不同LLM提供者的调用
//2. 支持多提供者切换：DashScope（阿里云通义千问）、OpenAI兼容API
//3. 使用策略模式，根据配置动态选择LLM提供者
@Slf4j
@Service
public class LlmService {

    //4. Spring AI ChatClient，用于调用OpenAI兼容API
    private final ChatClient chatClient;
    
    //5. DashScope服务，用于调用阿里云通义千问API
    private final DashScopeService dashScopeService;
    
    //6. 当前LLM提供者（从配置文件读取，默认dashscope）
    private final String provider;

    //7. 构造函数注入依赖
    public LlmService(ChatClient.Builder chatClientBuilder, 
                      DashScopeService dashScopeService,
                      @Value("${app.llm.provider:dashscope}") String provider) {
        this.chatClient = chatClientBuilder.build();
        this.dashScopeService = dashScopeService;
        this.provider = provider.toLowerCase();
    }

    //8. 核心方法：调用LLM生成响应
    //9. 参数：systemPrompt（系统提示）、userPrompt（用户提示）
    public String generate(String systemPrompt, String userPrompt) throws Exception {
        log.info("Using LLM provider: {}", provider);
        
        //10. 根据配置选择LLM提供者
        if ("dashscope".equals(provider) && dashScopeService.isConfigured()) {
            return dashScopeService.generate(systemPrompt, userPrompt);
        } else {
            //11. 使用Spring AI默认调用（OpenAI兼容API）
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        }
    }

    //12. 获取当前LLM提供者名称
    public String getProvider() {
        return provider;
    }
}