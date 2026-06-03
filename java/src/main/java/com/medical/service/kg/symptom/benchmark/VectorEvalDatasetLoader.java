package com.medical.service.kg.symptom.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class VectorEvalDatasetLoader {

    public static final String DEFAULT_DATASET_PATH = "kg/vector_topk_eval.json";

    private final ObjectMapper objectMapper;

    public VectorEvalDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VectorEvalDataset loadDefault() {
        return load(DEFAULT_DATASET_PATH);
    }

    public VectorEvalDataset load(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(in, VectorEvalDataset.class);
        } catch (Exception e) {
            throw new IllegalStateException("加载向量评测集失�? " + classpathLocation, e);
        }
    }
}
