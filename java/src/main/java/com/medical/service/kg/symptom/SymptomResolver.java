package com.medical.service.kg.symptom;

import com.medical.config.MedicalGraphProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 症状语义解析：同义词表 → 向量 Top-K → LLM 封闭词表消歧（中期 AI 工程化方案）
 */
@Slf4j
@Service
public class SymptomResolver {

    private final MedicalGraphProperties graphProperties;
    private final SymptomPhraseExtractor phraseExtractor;
    private final SymptomSynonymRegistry synonymRegistry;
    private final SymptomVocabularyService vocabularyService;
    private final SymptomEmbeddingService embeddingService;
    private final InMemorySymptomVectorIndex vectorIndex;
    private final SymptomLlmNormalizer llmNormalizer;

    public SymptomResolver(MedicalGraphProperties graphProperties,
                           SymptomPhraseExtractor phraseExtractor,
                           SymptomSynonymRegistry synonymRegistry,
                           SymptomVocabularyService vocabularyService,
                           SymptomEmbeddingService embeddingService,
                           InMemorySymptomVectorIndex vectorIndex,
                           @Lazy SymptomLlmNormalizer llmNormalizer) {
        this.graphProperties = graphProperties;
        this.phraseExtractor = phraseExtractor;
        this.synonymRegistry = synonymRegistry;
        this.vocabularyService = vocabularyService;
        this.embeddingService = embeddingService;
        this.vectorIndex = vectorIndex;
        this.llmNormalizer = llmNormalizer;
    }

    public boolean isEnabled() {
        return graphProperties.isEnabled() && graphProperties.getSymptomResolver().isEnabled();
    }

    public SymptomResolutionResult resolve(String rawText) {
        long start = System.currentTimeMillis();
        MedicalGraphProperties.SymptomResolver cfg = graphProperties.getSymptomResolver();
        SymptomResolutionResult.SymptomResolutionResultBuilder result = SymptomResolutionResult.builder()
                .vectorIndexReady(vectorIndex.isReady());

        if (!isEnabled() || !StringUtils.hasText(rawText)) {
            return result.resolveTimeMs(System.currentTimeMillis() - start).build();
        }

        List<SymptomVocabularyEntry> vocabulary = vocabularyService.getCachedVocabulary();
        Map<String, SymptomVocabularyEntry> byName = new LinkedHashMap<>();
        for (SymptomVocabularyEntry entry : vocabulary) {
            byName.put(entry.getName(), entry);
        }

        List<String> phrases = phraseExtractor.extractPhrases(rawText);
        List<SymptomMatch> matches = new ArrayList<>();
        Set<String> acceptedCanonical = new LinkedHashSet<>();
        List<String> trace = new ArrayList<>();

        for (String phrase : phrases) {
            Optional<SymptomMatch> match = resolvePhrase(phrase, vocabulary, byName, cfg);
            match.ifPresent(m -> {
                if (m.getConfidence() >= cfg.getAcceptMinConfidence()
                        && StringUtils.hasText(m.getCanonicalName())) {
                    String key = m.getCanonicalName();
                    if (!acceptedCanonical.contains(key)) {
                        matches.add(m);
                        acceptedCanonical.add(key);
                        trace.add(phrase + "→" + m.getCanonicalName()
                                + "(" + m.getMethod() + "," + String.format("%.2f", m.getConfidence()) + ")");
                    }
                }
            });
        }

        return result
                .matches(matches)
                .canonicalSymptomNames(acceptedCanonical)
                .traceSummary(String.join("; ", trace))
                .resolveTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private Optional<SymptomMatch> resolvePhrase(String phrase,
                                                  List<SymptomVocabularyEntry> vocabulary,
                                                  Map<String, SymptomVocabularyEntry> byName,
                                                  MedicalGraphProperties.SymptomResolver cfg) {
        String trimmed = phrase.trim();

        if (byName.containsKey(trimmed)) {
            SymptomVocabularyEntry exact = byName.get(trimmed);
            return Optional.of(SymptomMatch.builder()
                    .userPhrase(trimmed)
                    .canonicalName(exact.getName())
                    .symptomCode(exact.getCode())
                    .confidence(1.0)
                    .method("RULE")
                    .rationale("已是标准症状名")
                    .build());
        }

        if (cfg.isSynonymEnabled()) {
            Optional<SymptomMatch> synonym = synonymRegistry.resolveExact(trimmed);
            if (synonym.isPresent()) {
                enrichCode(synonym.get(), byName);
                return synonym;
            }
        }

        List<ScoredSymptomCandidate> vectorCandidates = List.of();
        if (vectorIndex.isReady() && embeddingService.isAvailable()) {
            float[] queryVector = embeddingService.embed(trimmed);
            if (queryVector.length > 0) {
                vectorCandidates = vectorIndex.search(queryVector, cfg.getVectorTopK());
            }
        }

        if (!vectorCandidates.isEmpty()) {
            ScoredSymptomCandidate top = vectorCandidates.get(0);
            boolean ambiguous = vectorCandidates.size() > 1
                    && (top.getScore() - vectorCandidates.get(1).getScore()) < cfg.getVectorAmbiguityGap();
            if (top.getScore() >= cfg.getVectorMinScore() && !ambiguous) {
                return Optional.of(SymptomMatch.builder()
                        .userPhrase(trimmed)
                        .canonicalName(top.getEntry().getName())
                        .symptomCode(top.getEntry().getCode())
                        .confidence(top.getScore())
                        .method("VECTOR")
                        .rationale("向量语义召回 Top1")
                        .build());
            }
            if (cfg.isLlmDisambiguate() && (!cfg.isLlmOnlyWhenAmbiguous() || ambiguous
                    || top.getScore() < cfg.getVectorMinScore())) {
                Optional<SymptomMatch> llm = llmNormalizer.disambiguate(trimmed, vectorCandidates, vocabulary);
                if (llm.isPresent() && llm.get().getConfidence() >= cfg.getAcceptMinConfidence()) {
                    enrichCode(llm.get(), byName);
                    return llm;
                }
            }
        }

        if (cfg.isLlmDisambiguate() && !cfg.isLlmRequireVectorCandidates()) {
            Optional<SymptomMatch> llm = llmNormalizer.disambiguate(trimmed, List.of(), vocabulary);
            if (llm.isPresent() && llm.get().getConfidence() >= cfg.getAcceptMinConfidence()) {
                enrichCode(llm.get(), byName);
                return llm;
            }
        }

        return Optional.empty();
    }

    private void enrichCode(SymptomMatch match, Map<String, SymptomVocabularyEntry> byName) {
        if (match.getSymptomCode() == null && byName.containsKey(match.getCanonicalName())) {
            match.setSymptomCode(byName.get(match.getCanonicalName()).getCode());
        }
    }
}
