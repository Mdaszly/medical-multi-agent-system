package com.medical.service.kg;

import com.medical.config.MedicalGraphProperties;
import com.medical.model.ClinicalState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IcdGroundingValidator {

    private static final Pattern ICD_PATTERN =
            Pattern.compile("\\b([A-TV-Z][0-9]{2}(?:\\.[0-9A-Z]{1,4})?)\\b");

    private final MedicalGraphProperties graphProperties;

    public String validate(ClinicalState state) {
        if (!graphProperties.isValidateIcd()) {
            return "SKIPPED";
        }

        @SuppressWarnings("unchecked")
        Set<String> candidates = new HashSet<>();
        Object raw = state.getExtensions().get(KnowledgeEnrichmentService.EXT_ICD_CANDIDATES);
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    candidates.add(String.valueOf(item));
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> consultResult = state.getExtensions().get("consultResult") instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : state.getDiagnosis();

        Set<String> mentioned = extractIcdFromResult(consultResult);
        if (mentioned.isEmpty()) {
            String status = candidates.isEmpty() ? "NO_HIT" : "VERIFIED";
            state.getExtensions().put(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS, status);
            return status;
        }

        if (candidates.isEmpty()) {
            state.getExtensions().put(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS, "WARNING");
            appendWarningTrace(state, "LLM 提及 ICD 但图谱无候选: " + mentioned);
            return "WARNING";
        }

        boolean allValid = candidates.containsAll(mentioned);
        String status = allValid ? "VERIFIED" : "WARNING";
        if (!allValid) {
            Set<String> invalid = new HashSet<>(mentioned);
            invalid.removeAll(candidates);
            appendWarningTrace(state, "未在图谱候选中的 ICD: " + invalid);
        }
        state.getExtensions().put(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS, status);
        return status;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractIcdFromResult(Map<String, Object> consultResult) {
        Set<String> codes = new HashSet<>();
        if (consultResult == null) {
            return codes;
        }
        Object refs = consultResult.get("icd_references");
        if (refs instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object code = map.get("code");
                    if (code != null && StringUtils.hasText(code.toString())) {
                        codes.add(code.toString().trim());
                    }
                }
            }
        }
        for (String field : List.of("reasoning", "conclusion", "evidence_summary")) {
            Object text = consultResult.get(field);
            if (text != null) {
                codes.addAll(extractIcdFromText(text.toString()));
            }
        }
        return codes;
    }

    private Set<String> extractIcdFromText(String text) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = ICD_PATTERN.matcher(text);
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return codes;
    }

    @SuppressWarnings("unchecked")
    private void appendWarningTrace(ClinicalState state, String detail) {
        List<Map<String, String>> trace = (List<Map<String, String>>) state.getExtensions()
                .computeIfAbsent("agentTrace", k -> new ArrayList<>());
        Map<String, String> item = new HashMap<>();
        item.put("agent", "IcdGroundingValidator");
        item.put("action", "grounding_warning");
        item.put("detail", detail);
        trace.add(item);
    }
}
