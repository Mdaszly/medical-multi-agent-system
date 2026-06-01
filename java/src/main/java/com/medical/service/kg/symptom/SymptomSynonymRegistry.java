package com.medical.service.kg.symptom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class SymptomSynonymRegistry {

    private final ObjectMapper objectMapper;
    private volatile Map<String, String> aliasToCanonical = Map.of();

    public SymptomSynonymRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        reload();
    }

    public void reload() {
        try (InputStream in = new ClassPathResource("kg/symptom-synonyms.json").getInputStream()) {
            Map<String, String> loaded = objectMapper.readValue(in, new TypeReference<>() {});
            aliasToCanonical = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
            log.info("症状同义词表加载完成: {} 条", aliasToCanonical.size());
        } catch (Exception e) {
            log.warn("症状同义词表加载失败，将仅依赖向量/LLM: {}", e.getMessage());
            aliasToCanonical = Map.of();
        }
    }

    public Optional<SymptomMatch> resolveExact(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return Optional.empty();
        }
        String trimmed = phrase.trim();
        String canonical = aliasToCanonical.get(trimmed);
        if (canonical != null) {
            return Optional.of(SymptomMatch.builder()
                    .userPhrase(trimmed)
                    .canonicalName(canonical)
                    .confidence(0.98)
                    .method("SYNONYM")
                    .rationale("同义词表映射")
                    .build());
        }
        return Optional.empty();
    }

    public List<String> findAliasesInText(String text) {
        if (text == null || text.isBlank() || aliasToCanonical.isEmpty()) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        List<String> keys = new ArrayList<>(aliasToCanonical.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String alias : keys) {
            if (text.contains(alias)) {
                found.add(alias);
            }
        }
        return new ArrayList<>(found);
    }

    public Map<String, String> getAliasToCanonical() {
        return aliasToCanonical;
    }
}
