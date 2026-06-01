package com.medical.service.kg.symptom;

import com.medical.knowledgegraph.model.entity.Symptom;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.service.kg.clinical.NonClinicalPhraseFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SymptomPhraseExtractor {

    private static final Pattern CLAUSE_SPLIT = Pattern.compile("[，,。；;、\\n]+");

    private final EntityExtractionService entityExtractionService;
    private final SymptomSynonymRegistry synonymRegistry;
    private final SymptomVocabularyService vocabularyService;
    private final NonClinicalPhraseFilter nonClinicalFilter;

    public List<String> extractPhrases(String rawText) {
        Set<String> phrases = new LinkedHashSet<>();
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }
        String text = rawText.trim();

        for (String alias : synonymRegistry.findAliasesInText(text)) {
            phrases.add(alias);
        }

        for (SymptomVocabularyEntry entry : vocabularyService.getCachedVocabulary()) {
            if (text.contains(entry.getName())) {
                phrases.add(entry.getName());
            }
            if (entry.getAliases() != null) {
                for (String alias : entry.getAliases()) {
                    if (text.contains(alias)) {
                        phrases.add(alias);
                    }
                }
            }
        }

        for (Symptom symptom : entityExtractionService.extractSymptoms(text)) {
            if (StringUtils.hasText(symptom.getName())) {
                phrases.add(symptom.getName());
            }
        }

        for (String clause : CLAUSE_SPLIT.split(text)) {
            String part = clause.trim();
            if (part.length() < 2 || part.length() > 24) {
                continue;
            }
            if (nonClinicalFilter.isPureNonClinical(part)) {
                continue;
            }
            if (nonClinicalFilter.hasSymptomSignal(part) || synonymRegistry.resolveExact(part).isPresent()) {
                phrases.add(part);
            }
        }

        if (phrases.isEmpty()
                && !nonClinicalFilter.isPureNonClinical(text)
                && nonClinicalFilter.hasSymptomSignal(text)) {
            phrases.add(text);
        }
        return new ArrayList<>(phrases);
    }
}
