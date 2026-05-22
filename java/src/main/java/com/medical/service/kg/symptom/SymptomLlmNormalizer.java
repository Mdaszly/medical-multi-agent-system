package com.medical.service.kg.symptom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SymptomLlmNormalizer {

    private static final String SYSTEM_PROMPT = """
            你是医疗症状标准化助手。任务：将用户的口语症状表述映射到「候选标准症状」列表中的某一项。
            
            硬性规则：
            1. canonical_name 必须完全等于候选列表中的 name，不得自造症状名。
            2. 若无法确定或表述不是症状，返回 matched=false。
            3. confidence 为 0~1 的小数；仅当 >=0.6 且明确同义/近义时 matched=true。
            4. 只输出 JSON，不要 markdown。
            
            输出格式：
            {"matched":true,"canonical_name":"头痛","confidence":0.92,"rationale":"头疼与头痛为同义口语"}
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public SymptomLlmNormalizer(@Lazy LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public Optional<SymptomMatch> disambiguate(String userPhrase,
                                               List<ScoredSymptomCandidate> candidates,
                                               List<SymptomVocabularyEntry> fullVocabulary) {
        if (!StringUtils.hasText(userPhrase)) {
            return Optional.empty();
        }
        List<SymptomVocabularyEntry> pool = candidates == null || candidates.isEmpty()
                ? fullVocabulary
                : candidates.stream().map(ScoredSymptomCandidate::getEntry).toList();
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        Set<String> allowed = pool.stream().map(SymptomVocabularyEntry::getName).collect(Collectors.toSet());
        String userPrompt = buildUserPrompt(userPhrase, pool);
        try {
            String raw = llmService.generate(SYSTEM_PROMPT, userPrompt);
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            if (!node.path("matched").asBoolean(false)) {
                return Optional.empty();
            }
            String canonical = node.path("canonical_name").asText("").trim();
            if (!allowed.contains(canonical)) {
                log.warn("LLM 返回非候选症状: phrase={} canonical={}", userPhrase, canonical);
                return Optional.empty();
            }
            double confidence = node.path("confidence").asDouble(0.0);
            return Optional.of(SymptomMatch.builder()
                    .userPhrase(userPhrase.trim())
                    .canonicalName(canonical)
                    .symptomCode(pool.stream()
                            .filter(e -> canonical.equals(e.getName()))
                            .map(SymptomVocabularyEntry::getCode)
                            .findFirst()
                            .orElse(null))
                    .confidence(confidence)
                    .method("LLM")
                    .rationale(node.path("rationale").asText("LLM 封闭词表消歧"))
                    .build());
        } catch (Exception e) {
            log.warn("症状 LLM 消歧失败 phrase={}: {}", userPhrase, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildUserPrompt(String userPhrase, List<SymptomVocabularyEntry> pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户表述: ").append(userPhrase.trim()).append("\n\n候选标准症状:\n");
        int i = 1;
        for (SymptomVocabularyEntry entry : pool) {
            sb.append(i++).append(". name=").append(entry.getName());
            if (StringUtils.hasText(entry.getDescription())) {
                sb.append(" | 描述=").append(entry.getDescription());
            }
            if (entry.getAliases() != null && !entry.getAliases().isEmpty()) {
                sb.append(" | 别名=").append(String.join("、", entry.getAliases()));
            }
            sb.append("\n");
        }
        return sb.toString();
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
