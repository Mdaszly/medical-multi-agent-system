package com.medical.service.kg.clinical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 临床片段 LLM 抽取：从混合句中识别可映射的标准症状表述，忽略寒暄与非主诉内容。
 */
@Slf4j
@Component
public class ClinicalSpanLlmExtractor {

    private static final String SYSTEM_PROMPT = """
            你是门诊预问诊的临床片段抽取助手。从用户输入中识别「可描述身体不适」的短语。
            
            硬性规则：
            1. 忽略寒暄（你好、医生、谢谢）、预约、医保、闲聊等非症状内容。
            2. symptom_spans 只保留口语症状片段，如「头疼」「有点发烧」，不要自造标准医学术语。
            3. 若整句没有任何症状描述，symptom_spans 必须为 []。
            4. 只输出 JSON，不要 markdown。
            
            输出格式：
            {"symptom_spans":["头疼"],"rationale":"用户描述头部疼痛"}
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public ClinicalSpanLlmExtractor(@Lazy LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public Optional<List<String>> extract(String userText) {
        if (!StringUtils.hasText(userText)) {
            return Optional.empty();
        }
        try {
            String raw = llmService.generate(SYSTEM_PROMPT, "用户输入：\n" + userText.trim());
            JsonNode node = objectMapper.readTree(extractJson(raw));
            List<String> spans = new ArrayList<>();
            for (JsonNode item : node.path("symptom_spans")) {
                String span = item.asText("").trim();
                if (StringUtils.hasText(span)) {
                    spans.add(span);
                }
            }
            return spans.isEmpty() ? Optional.empty() : Optional.of(spans);
        } catch (Exception e) {
            log.warn("临床片段 LLM 抽取失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
