package com.medical.service.kg;

import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import com.medical.knowledgegraph.model.entity.Symptom;
import com.medical.model.kg.GraphEvidence;
import com.medical.service.kg.symptom.SymptomMatch;
import com.medical.service.kg.symptom.SymptomResolutionResult;
import com.medical.service.kg.symptom.SymptomResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphFacade {

    private final KnowledgeGraphService knowledgeGraphService;
    private final EntityExtractionService entityExtractionService;
    private final MedicalGraphProperties graphProperties;

    @Autowired(required = false)
    private SymptomResolver symptomResolver;

    public GraphEvidence extractAndQuery(String rawText) {
        long start = System.currentTimeMillis();
        SymptomResolutionResult resolution = resolveSymptoms(rawText);
        Set<String> symptomNames = new LinkedHashSet<>(resolution.getCanonicalSymptomNames());
        symptomNames.addAll(extractSymptomNamesLegacy(rawText));
        List<SymptomDiagnosisRow> allRows = new ArrayList<>();
        Set<String> icdCodes = new LinkedHashSet<>();

        for (String symptom : symptomNames) {
            List<SymptomDiagnosisRow> rows = queryBySymptomName(symptom);
            if (rows.isEmpty() && graphProperties.isEnabled()) {
                rows = knowledgeGraphService.findSymptomDiagnosesFuzzy(
                        symptom, graphProperties.getFuzzySymptomLimit());
            }
            mergeRows(allRows, rows, icdCodes);
        }

        if (allRows.isEmpty() && StringUtils.hasText(rawText)) {
            List<String> suggested = suggestSymptoms(rawText, graphProperties.getFuzzySymptomLimit());
            for (String name : suggested) {
                symptomNames.add(name);
                mergeRows(allRows, queryBySymptomName(name), icdCodes);
            }
        }

        GraphEvidence evidence = GraphEvidence.builder()
                .rows(allRows)
                .extractedSymptoms(symptomNames)
                .icdCandidateCodes(icdCodes)
                .graphHit(!allRows.isEmpty())
                .queryTimeMs(System.currentTimeMillis() - start)
                .symptomMatches(resolution.getMatches())
                .symptomResolutionTrace(resolution.getTraceSummary())
                .build();
        evidence.setFormattedText(formatEvidenceText(evidence));
        return evidence;
    }

    private SymptomResolutionResult resolveSymptoms(String rawText) {
        if (symptomResolver != null && symptomResolver.isEnabled()) {
            return symptomResolver.resolve(rawText);
        }
        return SymptomResolutionResult.builder().build();
    }

    public List<SymptomDiagnosisRow> queryBySymptomName(String symptomName) {
        if (!graphProperties.isEnabled() || !StringUtils.hasText(symptomName)) {
            return List.of();
        }
        try {
            return knowledgeGraphService.findSymptomDiagnosesRows(symptomName.trim());
        } catch (Exception e) {
            log.warn("图谱查询失败 symptom={}: {}", symptomName, e.getMessage());
            return List.of();
        }
    }

    public List<String> suggestSymptoms(String prefix, int limit) {
        if (!graphProperties.isEnabled() || !StringUtils.hasText(prefix)) {
            return List.of();
        }
        try {
            return knowledgeGraphService.suggestSymptomNames(prefix, limit);
        } catch (Exception e) {
            log.warn("症状联想失败: {}", e.getMessage());
            return List.of();
        }
    }

    public Optional<SymptomDiagnosisRow> lookupIcd(String code) {
        if (!graphProperties.isEnabled() || !StringUtils.hasText(code)) {
            return Optional.empty();
        }
        try {
            return knowledgeGraphService.lookupByIcdCode(code.trim());
        } catch (Exception e) {
            log.warn("ICD 反查失败 code={}: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    public String formatEvidenceText(GraphEvidence evidence) {
        if (evidence == null || evidence.getRows() == null || evidence.getRows().isEmpty()) {
            String trace = evidence != null && StringUtils.hasText(evidence.getSymptomResolutionTrace())
                    ? "（语义解析: " + evidence.getSymptomResolutionTrace() + "）"
                    : "";
            return "【知识图谱检索结果】未命中相关症状-疾病-ICD 关联。请勿编造 ICD 编码；"
                    + "请在 reasoning 中说明「图谱未命中」。" + trace;
        }
        StringBuilder sb = new StringBuilder();
        if (evidence.getSymptomMatches() != null && !evidence.getSymptomMatches().isEmpty()) {
            sb.append("【症状语义解析】\n");
            for (SymptomMatch match : evidence.getSymptomMatches()) {
                sb.append("- ").append(match.getUserPhrase())
                        .append(" → ").append(match.getCanonicalName())
                        .append(" (").append(match.getMethod())
                        .append(", 置信度=").append(String.format("%.2f", match.getConfidence()))
                        .append(")\n");
            }
            sb.append("\n");
        }
        sb.append("【知识图谱检索结果 - ICD 编码必须来自下表，不得编造】\n");
        int i = 1;
        for (SymptomDiagnosisRow row : evidence.getRows()) {
            sb.append(i++).append(". 症状: ").append(nullToEmpty(row.getSymptom()))
                    .append(" | 可能疾病: ").append(nullToEmpty(row.getDisease()))
                    .append(" | ICD: ").append(nullToEmpty(row.getIcdCode()))
                    .append(" | 描述: ").append(nullToEmpty(row.getIcdDescription()))
                    .append("\n");
        }
        sb.append("若需引用 ICD，请使用 icd_references 字段，且 code 必须来自上表。\n");
        return sb.toString();
    }

    public String querySymptomDiagnosisAsText(String symptomName) {
        return formatEvidenceText(GraphEvidence.builder()
                .rows(queryBySymptomName(symptomName))
                .graphHit(!queryBySymptomName(symptomName).isEmpty())
                .build());
    }

    public String suggestSymptomsAsText(String prefix) {
        List<String> names = suggestSymptoms(prefix, graphProperties.getFuzzySymptomLimit());
        if (names.isEmpty()) {
            return "未找到匹配症状";
        }
        return "症状联想：" + String.join("、", names);
    }

    public String lookupIcdAsText(String code) {
        return lookupIcd(code)
                .map(row -> row.getIcdCode() + " - " + nullToEmpty(row.getIcdDescription())
                        + "（疾病: " + nullToEmpty(row.getDisease()) + "）")
                .orElse("图谱中未找到 ICD: " + code);
    }

    private Set<String> extractSymptomNamesLegacy(String rawText) {
        Set<String> names = new LinkedHashSet<>();
        if (!StringUtils.hasText(rawText)) {
            return names;
        }
        List<Symptom> extracted = entityExtractionService.extractSymptoms(rawText);
        for (Symptom s : extracted) {
            if (StringUtils.hasText(s.getName())) {
                names.add(s.getName());
            }
        }
        for (String seed : List.of("发热", "咳嗽", "头痛", "胸痛", "腹痛", "呼吸困难", "乏力", "恶心", "眩晕", "关节痛")) {
            if (rawText.contains(seed)) {
                names.add(seed);
            }
        }
        return names;
    }

    private void mergeRows(List<SymptomDiagnosisRow> target, List<SymptomDiagnosisRow> source, Set<String> icdCodes) {
        Map<String, SymptomDiagnosisRow> dedup = new LinkedHashMap<>();
        for (SymptomDiagnosisRow row : target) {
            dedup.put(dedupKey(row), row);
        }
        for (SymptomDiagnosisRow row : source) {
            dedup.put(dedupKey(row), row);
            if (StringUtils.hasText(row.getIcdCode())) {
                icdCodes.add(row.getIcdCode());
            }
        }
        target.clear();
        target.addAll(dedup.values());
    }

    private String dedupKey(SymptomDiagnosisRow row) {
        return (row.getSymptom() + "|" + row.getDisease() + "|" + row.getIcdCode());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
