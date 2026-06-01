package com.medical.service.kg.clinical;

import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.model.ClinicalState;
import com.medical.service.kg.symptom.SymptomSynonymRegistry;
import com.medical.service.kg.symptom.SymptomVocabularyEntry;
import com.medical.service.kg.symptom.SymptomVocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 临床文本门控 + 片段抽取（L0 结构化主诉优先，L1 规则抽槽，L2 可选 LLM 抽槽）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalSpanExtractor {

    private static final Pattern CLAUSE_SPLIT = Pattern.compile("[，,。；;、\\n]+");

    private final MedicalGraphProperties graphProperties;
    private final NonClinicalPhraseFilter nonClinicalFilter;
    private final SymptomSynonymRegistry synonymRegistry;
    private final SymptomVocabularyService vocabularyService;
    private final EntityExtractionService entityExtractionService;
    private final ClinicalSpanLlmExtractor llmExtractor;

    public ClinicalSpanExtractionResult extract(ClinicalState state) {
        String rawInput = state.getRawInput();
        String structuredSymptom = readStructuredSymptom(state);
        return extract(structuredSymptom, rawInput);
    }

    public ClinicalSpanExtractionResult extract(String structuredSymptom, String rawInput) {
        if (!graphProperties.getClinicalSpan().isEnabled()) {
            String fallback = firstNonBlank(structuredSymptom, rawInput);
            if (!StringUtils.hasText(fallback)) {
                return ClinicalSpanExtractionResult.skipped("SPAN_EXTRACTOR_DISABLED", "片段抽取已关闭且无输入");
            }
            return ClinicalSpanExtractionResult.ok(fallback, List.of(fallback), "LEGACY_RAW", "legacy=" + fallback);
        }

        Set<String> spans = new LinkedHashSet<>();
        String source = null;

        if (StringUtils.hasText(structuredSymptom) && !nonClinicalFilter.isPureNonClinical(structuredSymptom)) {
            spans.addAll(collectMedicalSpans(structuredSymptom.trim()));
            source = "STRUCTURED_FIELD";
        }

        if (StringUtils.hasText(rawInput)) {
            String chat = rawInput.trim();
            if (spans.isEmpty() && nonClinicalFilter.isPureNonClinical(chat)) {
                return ClinicalSpanExtractionResult.skipped(
                        "NON_CLINICAL_UTTERANCE",
                        "非临床话语，跳过图谱: " + chat);
            }
            if (!nonClinicalFilter.isPureNonClinical(chat)) {
                List<String> chatSpans = collectMedicalSpans(chat);
                if (!chatSpans.isEmpty()) {
                    spans.addAll(chatSpans);
                    if (source == null) {
                        source = "CHAT_SPAN";
                    }
                }
            }
        }

        if (spans.isEmpty()
                && graphProperties.getClinicalSpan().isLlmEnabled()
                && StringUtils.hasText(rawInput)
                && !nonClinicalFilter.isPureNonClinical(rawInput)) {
            Optional<List<String>> llmSpans = llmExtractor.extract(rawInput);
            if (llmSpans.isPresent() && !llmSpans.get().isEmpty()) {
                spans.addAll(llmSpans.get());
                source = "LLM_SPAN";
            }
        }

        if (spans.isEmpty()) {
            return ClinicalSpanExtractionResult.skipped(
                    "NO_CLINICAL_SPAN",
                    "未识别到可检索的临床片段");
        }

        List<String> spanList = new ArrayList<>(spans);
        String clinicalText = String.join("，", spanList);
        String trace = (source != null ? source : "UNKNOWN") + ": " + clinicalText;
        return ClinicalSpanExtractionResult.ok(clinicalText, spanList, source, trace);
    }

    private List<String> collectMedicalSpans(String text) {
        Set<String> spans = new LinkedHashSet<>();

        spans.addAll(synonymRegistry.findAliasesInText(text));

        for (SymptomVocabularyEntry entry : vocabularyService.getCachedVocabulary()) {
            if (StringUtils.hasText(entry.getName()) && text.contains(entry.getName())) {
                spans.add(entry.getName());
            }
            if (entry.getAliases() != null) {
                for (String alias : entry.getAliases()) {
                    if (StringUtils.hasText(alias) && text.contains(alias)) {
                        spans.add(alias);
                    }
                }
            }
        }

        entityExtractionService.extractSymptoms(text).stream()
                .map(s -> s.getName())
                .filter(StringUtils::hasText)
                .forEach(spans::add);

        for (String clause : CLAUSE_SPLIT.split(text)) {
            String part = clause.trim();
            if (part.length() < 2 || part.length() > 24) {
                continue;
            }
            if (nonClinicalFilter.isPureNonClinical(part)) {
                continue;
            }
            if (nonClinicalFilter.hasSymptomSignal(part) || synonymRegistry.resolveExact(part).isPresent()) {
                spans.add(part);
            }
        }

        spans.removeIf(nonClinicalFilter::isPureNonClinical);
        return new ArrayList<>(spans);
    }

    @SuppressWarnings("unchecked")
    private String readStructuredSymptom(ClinicalState state) {
        Object ctx = state.getExtensions().get("patientContext");
        if (!(ctx instanceof Map<?, ?> map)) {
            return null;
        }
        Object symptom = map.get("symptom");
        return symptom == null ? null : String.valueOf(symptom).trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        if (StringUtils.hasText(b)) {
            return b.trim();
        }
        return null;
    }
}
