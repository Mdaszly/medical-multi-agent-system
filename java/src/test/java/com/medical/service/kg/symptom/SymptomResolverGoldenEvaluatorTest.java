package com.medical.service.kg.symptom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 黄金集评测：Recall@1（每条 input 的 expected 是否被 canonical 集合覆盖）
 */
class SymptomResolverGoldenEvaluatorTest {

    @Test
    @DisplayName("黄金集结构可加载")
    void goldenFile_loadable() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/kg/symptom_resolver_golden.json")) {
            List<GoldenCase> cases = new ObjectMapper().readValue(in, new TypeReference<>() {});
            assertTrue(cases.size() >= 5);
        }
    }

    record GoldenCase(String input, List<String> expected) {
    }

    /** 供集成测试或人工跑分：expected ⊆ canonicalNames 则命中 */
    public static double recallAt1(SymptomResolver resolver, List<GoldenCase> cases) {
        int hit = 0;
        int total = 0;
        for (GoldenCase c : cases) {
            Set<String> canonical = resolver.resolve(c.input()).getCanonicalSymptomNames();
            for (String exp : c.expected()) {
                total++;
                if (canonical.contains(exp)) {
                    hit++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hit / total;
    }
}
