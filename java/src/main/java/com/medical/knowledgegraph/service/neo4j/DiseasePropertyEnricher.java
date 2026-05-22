package com.medical.knowledgegraph.service.neo4j;

import com.medical.knowledgegraph.model.dto.QueryResultDTO;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Disease 节点编码补全、校验与 API 响应属性增强。
 */
@Slf4j
@Component
public class DiseasePropertyEnricher {

    /** ICD-10 常见格式，如 G43.909、I10、J06.9 */
    private static final Pattern ICD10_PATTERN =
            Pattern.compile("^[A-TV-Z][0-9][0-9A-Z](?:\\.[0-9A-Z]{1,4})?$", Pattern.CASE_INSENSITIVE);

    private static final String DEFAULT_UNKNOWN_ICD = "UNKNOWN";
    private static final String DEFAULT_UNKNOWN_DISEASE_CODE = "D_UNKNOWN";

    private static final String BACKFILL_CYPHER =
            "MATCH (d:Disease) " +
            "WHERE d.diseaseCode IS NULL OR d.icd10Code IS NULL " +
            "OPTIONAL MATCH (d)-[:CLASSIFIED_AS]->(i:ICD10) " +
            "WITH d, head(collect(i)) AS i " +
            "WHERE i IS NOT NULL " +
            "SET d.icd10Code = coalesce(d.icd10Code, i.code), " +
            "    d.diseaseCode = coalesce(d.diseaseCode, $prefix + replace(i.code, '.', '_')) " +
            "RETURN count(d) AS updated";

    /**
     * 从 CLASSIFIED_AS 关联的 ICD10 节点回写缺失的 diseaseCode / icd10Code。
     */
    public long backfillFromIcdRelationships(Driver driver) {
        try (Session session = driver.session()) {
            long updated = session.run(BACKFILL_CYPHER, Map.of("prefix", "D_"))
                    .single()
                    .get("updated")
                    .asLong();
            if (updated > 0) {
                log.info("已从 ICD10 关系补全 {} 个 Disease 节点编码", updated);
            }
            return updated;
        } catch (Exception e) {
            log.warn("Disease 编码回写失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 诊断记录去重键：优先 diseaseCode，其次 疾病名+ICD，最后仅疾病名。
     */
    public String diagnosisDedupKey(SymptomDiagnosisRow row) {
        if (row == null) {
            return "";
        }
        if (isPresent(row.getDiseaseCode())) {
            return "dc:" + row.getDiseaseCode().trim();
        }
        if (isPresent(row.getIcdCode())) {
            return "dn:" + row.getDisease() + "|icd:" + row.getIcdCode().trim();
        }
        return "dn:" + Objects.toString(row.getDisease(), "");
    }

    /**
     * 合并两条诊断记录，保留权重更高、字段更完整的一条。
     */
    public SymptomDiagnosisRow mergeDiagnosisRows(SymptomDiagnosisRow existing, SymptomDiagnosisRow incoming) {
        if (existing == null) {
            return enrichDiagnosisRow(incoming);
        }
        if (incoming == null) {
            return enrichDiagnosisRow(existing);
        }
        SymptomDiagnosisRow preferred = preferRow(existing, incoming);
        SymptomDiagnosisRow other = preferred == existing ? incoming : existing;
        return SymptomDiagnosisRow.builder()
                .symptom(coalesce(preferred.getSymptom(), other.getSymptom()))
                .disease(coalesce(preferred.getDisease(), other.getDisease()))
                .diseaseCode(coalesce(preferred.getDiseaseCode(), other.getDiseaseCode()))
                .icdCode(coalesce(preferred.getIcdCode(), other.getIcdCode()))
                .icdDescription(coalesce(preferred.getIcdDescription(), other.getIcdDescription()))
                .weight(maxWeight(preferred.getWeight(), other.getWeight()))
                .priority(preferred.getPriority() != null ? preferred.getPriority() : other.getPriority())
                .build();
    }

    public SymptomDiagnosisRow enrichDiagnosisRow(SymptomDiagnosisRow row) {
        if (row == null) {
            return null;
        }
        String icd = normalizeIcdCode(row.getIcdCode());
        String diseaseCode = row.getDiseaseCode();
        if (!isPresent(diseaseCode) && isPresent(icd)) {
            diseaseCode = diseaseCodeFromIcd(icd);
        }
        return SymptomDiagnosisRow.builder()
                .symptom(row.getSymptom())
                .disease(row.getDisease())
                .diseaseCode(diseaseCode)
                .icdCode(icd)
                .icdDescription(row.getIcdDescription())
                .weight(row.getWeight())
                .priority(row.getPriority())
                .build();
    }

    /**
     * 增强 API 中的 Disease 节点属性（补全 diseaseCode / icd10Code）。
     */
    public QueryResultDTO.NodeResult enrichDiseaseNodeResult(
            QueryResultDTO.NodeResult node,
            SymptomDiagnosisRow row) {
        if (node == null || !"Disease".equals(node.getLabel())) {
            return node;
        }
        Map<String, Object> props = new LinkedHashMap<>(
                node.getProperties() != null ? node.getProperties() : Map.of());

        String icd = normalizeIcdCode(coalesce(
                stringProp(props, "icd10Code"),
                row != null ? row.getIcdCode() : null));
        String diseaseCode = coalesce(
                stringProp(props, "diseaseCode"),
                row != null ? row.getDiseaseCode() : null);

        if (!isPresent(diseaseCode) && isPresent(icd)) {
            diseaseCode = diseaseCodeFromIcd(icd);
        }
        if (!isPresent(icd)) {
            icd = null;
            if (!isPresent(diseaseCode)) {
                diseaseCode = DEFAULT_UNKNOWN_DISEASE_CODE;
            }
        }

        if (isPresent(diseaseCode)) {
            props.put("diseaseCode", diseaseCode);
        }
        if (isPresent(icd)) {
            props.put("icd10Code", icd);
        }

        return QueryResultDTO.NodeResult.builder()
                .id(node.getId())
                .label(node.getLabel())
                .name(node.getName())
                .properties(props)
                .build();
    }

    /**
     * 合并同名 Disease 节点视图，保留属性更完整者。
     */
    public QueryResultDTO.NodeResult mergeDiseaseNodeResults(
            QueryResultDTO.NodeResult existing,
            QueryResultDTO.NodeResult incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }
        if (completenessScore(incoming) >= completenessScore(existing)) {
            return mergeProperties(incoming, existing);
        }
        return mergeProperties(existing, incoming);
    }

    public String diseaseNodeKey(QueryResultDTO.NodeResult node) {
        if (node == null) {
            return "";
        }
        Map<String, Object> props = node.getProperties();
        String code = props != null ? stringProp(props, "diseaseCode") : null;
        if (isPresent(code)) {
            return "dc:" + code;
        }
        return "dn:" + Objects.toString(node.getName(), "");
    }

    public String normalizeIcdCode(String code) {
        if (!isPresent(code)) {
            return null;
        }
        String trimmed = code.trim().toUpperCase();
        if (DEFAULT_UNKNOWN_ICD.equals(trimmed)) {
            return trimmed;
        }
        if (ICD10_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        log.debug("ICD-10 格式未通过校验，保留原值: {}", trimmed);
        return trimmed;
    }

    public boolean isValidIcdCode(String code) {
        return isPresent(code) && ICD10_PATTERN.matcher(code.trim().toUpperCase()).matches();
    }

    public String diseaseCodeFromIcd(String icdCode) {
        String normalized = normalizeIcdCode(icdCode);
        if (!isPresent(normalized)) {
            return DEFAULT_UNKNOWN_DISEASE_CODE;
        }
        return "D_" + normalized.replace(".", "_");
    }

    private SymptomDiagnosisRow preferRow(SymptomDiagnosisRow a, SymptomDiagnosisRow b) {
        int scoreA = rowCompletenessScore(a);
        int scoreB = rowCompletenessScore(b);
        if (scoreB > scoreA) {
            return b;
        }
        if (scoreA > scoreB) {
            return a;
        }
        double weightA = a.getWeight() != null ? a.getWeight() : 0;
        double weightB = b.getWeight() != null ? b.getWeight() : 0;
        return weightB > weightA ? b : a;
    }

    private int rowCompletenessScore(SymptomDiagnosisRow row) {
        int score = 0;
        if (isPresent(row.getDiseaseCode())) {
            score += 4;
        }
        if (isValidIcdCode(row.getIcdCode())) {
            score += 3;
        } else if (isPresent(row.getIcdCode())) {
            score += 1;
        }
        if (isPresent(row.getIcdDescription())) {
            score += 1;
        }
        return score;
    }

    private int completenessScore(QueryResultDTO.NodeResult node) {
        if (node == null || node.getProperties() == null) {
            return 0;
        }
        Map<String, Object> p = node.getProperties();
        int score = 0;
        if (isPresent(stringProp(p, "diseaseCode"))) {
            score += 4;
        }
        if (isValidIcdCode(stringProp(p, "icd10Code"))) {
            score += 3;
        } else if (isPresent(stringProp(p, "icd10Code"))) {
            score += 1;
        }
        score += p.size() / 5;
        return score;
    }

    private QueryResultDTO.NodeResult mergeProperties(
            QueryResultDTO.NodeResult primary,
            QueryResultDTO.NodeResult secondary) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (secondary.getProperties() != null) {
            merged.putAll(secondary.getProperties());
        }
        if (primary.getProperties() != null) {
            primary.getProperties().forEach((k, v) -> {
                if (v != null && (!(v instanceof String) || !((String) v).isBlank())) {
                    merged.put(k, v);
                } else if (!merged.containsKey(k)) {
                    merged.put(k, v);
                }
            });
        }
        return QueryResultDTO.NodeResult.builder()
                .id(primary.getId())
                .label(primary.getLabel())
                .name(primary.getName())
                .properties(merged)
                .build();
    }

    private double maxWeight(Double a, Double b) {
        double wa = a != null ? a : 0;
        double wb = b != null ? b : 0;
        return Math.max(wa, wb);
    }

    private String stringProp(Map<String, Object> props, String key) {
        Object v = props.get(key);
        return v != null ? v.toString() : null;
    }

    private String coalesce(String first, String second) {
        return isPresent(first) ? first : second;
    }

    private boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
