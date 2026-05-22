package com.medical.service.kg.symptom.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorEvalCase {

    private String id;
    private String bucket;
    private String query;
    private List<String> queries;
    private List<String> expected = new ArrayList<>();
    private String note;

    public List<String> resolveQueryPhrases() {
        if (queries != null && !queries.isEmpty()) {
            return queries;
        }
        if (query != null && !query.isBlank()) {
            return List.of(query.trim());
        }
        return List.of();
    }
}
