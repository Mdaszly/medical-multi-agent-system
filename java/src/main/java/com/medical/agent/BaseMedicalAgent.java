package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseMedicalAgent implements MedicalAgent {

    protected final LlmService llmService;
    protected final ObjectMapper objectMapper;

    protected String cleanJsonResponse(String response) {
        String content = response.trim();
        if (content.startsWith("```")) {
            content = content.substring(content.indexOf('\n') + 1);
            int lastFence = content.lastIndexOf("```");
            if (lastFence >= 0) {
                content = content.substring(0, lastFence).trim();
            }
        }
        return content;
    }

    protected String generate(String systemPrompt, String userPrompt) throws Exception {
        return llmService.generate(systemPrompt, userPrompt);
    }
}
