package com.medical.service.kg.symptom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import com.medical.mapper.SymptomMapper;
import com.medical.service.kg.clinical.NonClinicalPhraseFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomResolverSynonymTest {

    @Mock
    private KnowledgeGraphService knowledgeGraphService;
    @Mock
    private SymptomMapper symptomMapper;
    @Mock
    private EntityExtractionService entityExtractionService;
    @Mock
    private SymptomEmbeddingService embeddingService;
    @Mock
    private InMemorySymptomVectorIndex vectorIndex;
    @Mock
    private SymptomLlmNormalizer llmNormalizer;

    private SymptomResolver resolver;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setEnabled(true);
        props.getSymptomResolver().setEnabled(true);
        props.getSymptomResolver().setLlmDisambiguate(false);

        SymptomSynonymRegistry registry = new SymptomSynonymRegistry(new ObjectMapper());
        registry.load();

        SymptomVocabularyService vocabularyService = new SymptomVocabularyService(
                knowledgeGraphService, symptomMapper, registry, props);
        when(knowledgeGraphService.listSymptomVocabulary()).thenReturn(List.of(
                Map.of("name", "头痛", "code", "S001", "description", "头部疼痛"),
                Map.of("name", "发热", "code", "S002"),
                Map.of("name", "咳嗽", "code", "S003"),
                Map.of("name", "腹泻", "code", "S007"),
                Map.of("name", "胸痛", "code", "S011"),
                Map.of("name", "呼吸困难", "code", "S004"),
                Map.of("name", "失眠", "code", "S020"),
                Map.of("name", "乏力", "code", "S010")
        ));
        vocabularyService.loadVocabulary();

        when(embeddingService.isAvailable()).thenReturn(false);
        when(vectorIndex.isReady()).thenReturn(false);

        SymptomPhraseExtractor extractor = new SymptomPhraseExtractor(
                entityExtractionService, registry, vocabularyService, new NonClinicalPhraseFilter());

        resolver = new SymptomResolver(
                props, extractor, registry, vocabularyService,
                embeddingService, vectorIndex, llmNormalizer);
    }

    @Test
    @DisplayName("头疼应通过同义词表映射为头痛")
    void resolve_touteng_mapsToToutong() {
        SymptomResolutionResult result = resolver.resolve("我最近经常头疼");
        assertTrue(result.getCanonicalSymptomNames().contains("头痛"),
                "expected 头痛 but got " + result.getCanonicalSymptomNames());
    }

    @Test
    @DisplayName("发烧咳嗽应映射为标准症状")
    void resolve_feverAndCough() {
        SymptomResolutionResult result = resolver.resolve("有点发烧还咳嗽");
        assertTrue(result.getCanonicalSymptomNames().contains("发热"));
        assertTrue(result.getCanonicalSymptomNames().contains("咳嗽"));
    }
}
