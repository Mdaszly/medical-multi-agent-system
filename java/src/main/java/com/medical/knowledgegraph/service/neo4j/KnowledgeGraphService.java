package com.medical.knowledgegraph.service.neo4j;

import com.medical.knowledgegraph.exception.KnowledgeGraphException;
import com.medical.knowledgegraph.model.dto.QueryResultDTO;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.knowledgegraph.model.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j知识图谱核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final Driver neo4jDriver;

    // ==================== 节点操作 ====================

    /**
     * 创建节点
     */
    public void createNode(BaseNode node) {
        String cypher = String.format(
                "CREATE (n:%s $props) RETURN n",
                node.getLabel()
        );
        
        Map<String, Object> params = new HashMap<>();
        params.put("props", node.toNeo4jProperties());
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.value(params));
            log.debug("创建节点成功: label={}, name={}", node.getLabel(), node.getName());
        } catch (Exception e) {
            log.error("创建节点失败: label={}, name={}", node.getLabel(), node.getName(), e);
            throw new KnowledgeGraphException("CREATE_NODE_ERROR", 
                    "创建节点失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量创建节点
     */
    public void createNodes(List<? extends BaseNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        String label = nodes.get(0).getLabel();
        String cypher = String.format(
                "UNWIND $batch AS row " +
                "CREATE (n:%s) " +
                "SET n = row.props " +
                "RETURN count(n) AS createdCount",
                label
        );

        List<Map<String, Object>> batch = nodes.stream()
                .map(node -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("props", node.toNeo4jProperties());
                    return item;
                })
                .collect(Collectors.toList());

        try (Session session = neo4jDriver.session()) {
            Map<String, Object> params = Collections.singletonMap("batch", batch);
            Result result = session.run(cypher, Values.value(params));
            long count = result.single().get("createdCount").asLong();
            log.info("批量创建节点完成: label={}, count={}", label, count);
        } catch (Exception e) {
            log.error("批量创建节点失败: label={}", label, e);
            throw new KnowledgeGraphException("BATCH_CREATE_NODES_ERROR", 
                    "批量创建节点失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建或更新节点 (MERGE)
     */
    public void upsertNode(BaseNode node, String uniqueProperty) {
        String cypher = String.format(
                "MERGE (n:%s {%s: $props.%s}) " +
                "ON CREATE SET n = $props " +
                "ON MATCH SET n += $props " +
                "RETURN n",
                node.getLabel(),
                uniqueProperty,
                uniqueProperty
        );

        Map<String, Object> props = node.toNeo4jProperties();
        Map<String, Object> params = new HashMap<>();
        params.put("props", props);

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, params);
                return null;
            });
            log.debug("Upsert节点成功: label={}, name={}", node.getLabel(), node.getName());
        } catch (Exception e) {
            log.error("Upsert节点失败: label={}, name={}", node.getLabel(), node.getName(), e);
            throw new KnowledgeGraphException("UPSERT_NODE_ERROR", 
                    "Upsert节点失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除节点
     */
    public void deleteNode(String label, String property, Object value) {
        String cypher = String.format(
                "MATCH (n:%s {%s: $value}) DETACH DELETE n",
                label,
                property
        );

        Map<String, Object> params = Collections.singletonMap("value", value);

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, params);
                return null;
            });
            log.debug("删除节点成功: label={}, {}={}", label, property, value);
        } catch (Exception e) {
            log.error("删除节点失败: label={}, {}={}", label, property, value, e);
            throw new KnowledgeGraphException("DELETE_NODE_ERROR", 
                    "删除节点失败: " + e.getMessage(), e);
        }
    }

    // ==================== 关系操作 ====================

    /**
     * 创建关系
     */
    public void createRelationship(KnowledgeRelation relation) {
        String cypher = String.format(
                "MATCH (source:%s {name: $sourceName}) " +
                "MATCH (target:%s {name: $targetName}) " +
                "MERGE (source)-[r:%s]->(target) " +
                "SET r += $properties " +
                "RETURN r",
                relation.getSourceLabel(),
                relation.getTargetLabel(),
                relation.getType()
        );

        Map<String, Object> params = new HashMap<>();
        params.put("sourceName", relation.getSourceName());
        params.put("targetName", relation.getTargetName());
        params.put("properties", buildRelationProperties(relation));

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, params);
                return null;
            });
            log.debug("创建关系成功: {} -> [{}] -> {}", 
                    relation.getSourceName(), relation.getType(), relation.getTargetName());
        } catch (Exception e) {
            log.warn("创建关系失败，可能节点不存在: {} -> [{}] -> {}", 
                    relation.getSourceName(), relation.getType(), relation.getTargetName());
        }
    }

    /**
     * 批量创建关系
     */
    public void createRelationships(List<KnowledgeRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                for (KnowledgeRelation relation : relations) {
                    String cypher = String.format(
                            "MATCH (source:%s {name: $sourceName}) " +
                            "MATCH (target:%s {name: $targetName}) " +
                            "MERGE (source)-[r:%s]->(target) " +
                            "SET r += $props",
                            relation.getSourceLabel() != null ? relation.getSourceLabel() : "UNKNOWN",
                            relation.getTargetLabel() != null ? relation.getTargetLabel() : "UNKNOWN",
                            relation.getType()
                    );

                    Map<String, Object> params = new HashMap<>();
                    params.put("sourceName", relation.getSourceName());
                    params.put("targetName", relation.getTargetName());
                    params.put("props", buildRelationProperties(relation));

                    tx.run(cypher, params);
                }
                return null;
            });
            log.info("批量创建关系完成: count={}", relations.size());
        } catch (Exception e) {
            log.error("批量创建关系失败", e);
            throw new KnowledgeGraphException("BATCH_CREATE_RELATIONS_ERROR", 
                    "批量创建关系失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除关系
     */
    public void deleteRelationship(String sourceName, String targetName, String relationType) {
        String cypher = String.format(
                "MATCH (source)-[r:%s]->(target) " +
                "WHERE source.name = $sourceName AND target.name = $targetName " +
                "DELETE r",
                relationType
        );

        Map<String, Object> params = new HashMap<>();
        params.put("sourceName", sourceName);
        params.put("targetName", targetName);

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, params);
                return null;
            });
            log.debug("删除关系成功: {} -> [{}] -> {}", sourceName, relationType, targetName);
        } catch (Exception e) {
            log.error("删除关系失败", e);
            throw new KnowledgeGraphException("DELETE_RELATION_ERROR", 
                    "删除关系失败: " + e.getMessage(), e);
        }
    }

    // ==================== 查询操作 ====================

    /**
     * 执行Cypher查询
     */
    public QueryResultDTO executeQuery(String cypher, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.value(params));
            
            QueryResultDTO queryResult = QueryResultDTO.builder()
                    .query(cypher)
                    .queryType("CYPHER")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .nodes(new ArrayList<>())
                    .relations(new ArrayList<>())
                    .paths(new ArrayList<>())
                    .build();
            
            while (result.hasNext()) {
                Record record = result.next();
                processRecord(record, queryResult);
            }
            
            queryResult.setTotalCount(queryResult.getNodes().size());
            return queryResult;
            
        } catch (Exception e) {
            log.error("执行Cypher查询失败: {}", cypher, e);
            throw new KnowledgeGraphException("QUERY_ERROR", 
                    "执行查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据名称查询节点
     */
    public QueryResultDTO findNodeByName(String label, String name) {
        String cypher = String.format(
                "MATCH (n:%s) " +
                "WHERE n.name CONTAINS $name " +
                "RETURN n",
                label
        );

        Map<String, Object> params = Collections.singletonMap("name", name);
        return executeQuery(cypher, params);
    }

    /**
     * 查询节点的关联关系
     */
    public QueryResultDTO findNodeRelations(String label, String name, 
                                             String relationType, int depth) {
        String cypher = String.format(
                "MATCH path = (source:%s {name: $name})-[*1..%d]-(target) " +
                "RETURN source, relationships(path) as rels, target",
                label,
                depth
        );

        Map<String, Object> params = Collections.singletonMap("name", name);
        return executeQuery(cypher, params);
    }

    /**
     * 查找两个节点之间的所有路径
     */
    public QueryResultDTO findPaths(String sourceLabel, String sourceName,
                                     String targetLabel, String targetName,
                                     int maxDepth) {
        String cypher = String.format(
                "MATCH path = (source:%s {name: $sourceName})" +
                "-[*1.." + maxDepth + "]->" +
                "(target:%s {name: $targetName}) " +
                "RETURN path",
                sourceLabel,
                targetLabel
        );

        Map<String, Object> params = new HashMap<>();
        params.put("sourceName", sourceName);
        params.put("targetName", targetName);

        return executeQuery(cypher, params);
    }

    /** 症状诊断：标量结果（供 Facade / 同步等表格化消费） */
    private static final String SYMPTOM_DIAGNOSIS_SCALAR_CYPHER =
            "MATCH (s:Symptom {name: $name})-[r:INDICATES]->(d:Disease) " +
            "OPTIONAL MATCH (d)-[:CLASSIFIED_AS]->(i:ICD10) " +
            "RETURN s.name AS symptom, d.name AS disease, " +
            "       d.diseaseCode AS diseaseCode, " +
            "       i.code AS icdCode, i.descriptionCn AS icdDescription, " +
            "       coalesce(r.weight, 1.0) AS weight " +
            "ORDER BY weight DESC";

    /** 症状诊断：同时返回图节点与关系（供 REST diagnosis 接口） */
    private static final String SYMPTOM_DIAGNOSIS_GRAPH_CYPHER =
            "MATCH (s:Symptom {name: $name})-[r:INDICATES]->(d:Disease) " +
            "OPTIONAL MATCH (d)-[c:CLASSIFIED_AS]->(i:ICD10) " +
            "RETURN s, r, d, c, i, " +
            "       s.name AS symptom, d.name AS disease, " +
            "       d.diseaseCode AS diseaseCode, " +
            "       i.code AS icdCode, i.descriptionCn AS icdDescription, " +
            "       coalesce(r.weight, 1.0) AS weight " +
            "ORDER BY weight DESC";

    private static final String SYMPTOM_DIAGNOSIS_FUZZY_CYPHER =
            "MATCH (s:Symptom)-[:INDICATES]->(d:Disease) " +
            "WHERE s.name CONTAINS $keyword " +
            "OPTIONAL MATCH (d)-[:CLASSIFIED_AS]->(i:ICD10) " +
            "RETURN s.name AS symptom, d.name AS disease, " +
            "       d.diseaseCode AS diseaseCode, " +
            "       i.code AS icdCode, i.descriptionCn AS icdDescription, " +
            "       1.0 AS weight " +
            "LIMIT $limit";

    /**
     * 查询症状关联的疾病和 ICD；填充 nodes、relations、去重后的 records。
     */
    public QueryResultDTO findSymptomDiagnoses(String symptomName) {
        long startTime = System.currentTimeMillis();
        if (symptomName == null || symptomName.isBlank()) {
            return buildSymptomDiagnosisResult(List.of(), List.of(), List.of(), startTime);
        }

        Map<Long, QueryResultDTO.NodeResult> nodeById = new LinkedHashMap<>();
        Map<Long, QueryResultDTO.RelationResult> relById = new LinkedHashMap<>();
        LinkedHashMap<String, SymptomDiagnosisRow> recordByKey = new LinkedHashMap<>();

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(
                    SYMPTOM_DIAGNOSIS_GRAPH_CYPHER, Map.of("name", symptomName.trim()));
            while (result.hasNext()) {
                Record record = result.next();
                mergeGraphEntities(record, nodeById, relById);
                SymptomDiagnosisRow row = mapRecordToRow(record);
                recordByKey.putIfAbsent(diagnosisRecordKey(row), row);
            }
        } catch (Exception e) {
            log.error("症状诊断图查询失败", e);
            throw new KnowledgeGraphException("SYMPTOM_DIAGNOSIS_ERROR",
                    "症状诊断查询失败: " + e.getMessage(), e);
        }

        List<SymptomDiagnosisRow> rows = new ArrayList<>(recordByKey.values());
        return buildSymptomDiagnosisResult(
                new ArrayList<>(nodeById.values()),
                new ArrayList<>(relById.values()),
                rows,
                startTime);
    }

    /**
     * 表格化查询：症状 -> 疾病 -> ICD（已按疾病+ICD 去重）
     */
    public List<SymptomDiagnosisRow> findSymptomDiagnosesRows(String symptomName) {
        if (symptomName == null || symptomName.isBlank()) {
            return List.of();
        }
        return runSymptomDiagnosisQuery(
                SYMPTOM_DIAGNOSIS_SCALAR_CYPHER, Map.of("name", symptomName.trim()));
    }

    /**
     * 模糊症状匹配诊断
     */
    public List<SymptomDiagnosisRow> findSymptomDiagnosesFuzzy(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return runSymptomDiagnosisQuery(
                SYMPTOM_DIAGNOSIS_FUZZY_CYPHER,
                Map.of("keyword", keyword.trim(), "limit", safeLimit));
    }

    /**
     * 症状名称联想
     */
    public List<String> suggestSymptomNames(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String cypher =
                "MATCH (s:Symptom) WHERE s.name CONTAINS $prefix " +
                "RETURN s.name AS name ORDER BY s.frequency DESC, s.name LIMIT $limit";
        Map<String, Object> params = Map.of("prefix", prefix.trim(), "limit", safeLimit);
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            List<String> names = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                if (!record.get("name").isNull()) {
                    names.add(record.get("name").asString());
                }
            }
            return names;
        } catch (Exception e) {
            log.error("症状联想查询失败: prefix={}", prefix, e);
            throw new KnowledgeGraphException("SUGGEST_SYMPTOM_ERROR",
                    "症状联想查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按 ICD 编码反查
     */
    public Optional<SymptomDiagnosisRow> lookupByIcdCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String cypher =
                "MATCH (i:ICD10 {code: $code})<-[:CLASSIFIED_AS]-(d:Disease) " +
                "OPTIONAL MATCH (s:Symptom)-[:INDICATES]->(d) " +
                "RETURN coalesce(s.name, '') AS symptom, d.name AS disease, " +
                "       d.diseaseCode AS diseaseCode, i.code AS icdCode, " +
                "       i.descriptionCn AS icdDescription, 1.0 AS weight " +
                "LIMIT 1";
        List<SymptomDiagnosisRow> rows = runSymptomDiagnosisQuery(cypher, Map.of("code", code.trim()));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * 导出全部症状-ICD 关联（供 RDB 同步）
     */
    public List<SymptomDiagnosisRow> exportAllSymptomIcdRelations() {
        String cypher =
                "MATCH (s:Symptom)-[r:INDICATES]->(d:Disease) " +
                "OPTIONAL MATCH (d)-[:CLASSIFIED_AS]->(i:ICD10) " +
                "WHERE i.code IS NOT NULL " +
                "RETURN s.name AS symptom, d.name AS disease, d.diseaseCode AS diseaseCode, " +
                "       i.code AS icdCode, i.descriptionCn AS icdDescription, " +
                "       coalesce(r.weight, 1.0) AS weight, coalesce(r.priority, 1) AS priority " +
                "ORDER BY s.name, priority";
        return runSymptomDiagnosisQueryWithPriority(cypher, Map.of());
    }

    public boolean isEmptyGraph() {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (n) RETURN count(n) AS c");
            if (result.hasNext()) {
                return result.next().get("c").asLong() == 0;
            }
        }
        return true;
    }

    private List<SymptomDiagnosisRow> runSymptomDiagnosisQuery(String cypher, Map<String, Object> params) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            List<SymptomDiagnosisRow> rows = new ArrayList<>();
            while (result.hasNext()) {
                rows.add(mapRecordToRow(result.next()));
            }
            return deduplicateDiagnosisRows(rows);
        } catch (Exception e) {
            log.error("症状诊断查询失败: {}", cypher, e);
            throw new KnowledgeGraphException("SYMPTOM_DIAGNOSIS_ERROR",
                    "症状诊断查询失败: " + e.getMessage(), e);
        }
    }

    private List<SymptomDiagnosisRow> runSymptomDiagnosisQueryWithPriority(String cypher, Map<String, Object> params) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            List<SymptomDiagnosisRow> rows = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                SymptomDiagnosisRow row = mapRecordToRow(record);
                Value priorityVal = record.get("priority");
                if (!priorityVal.isNull()) {
                    row.setPriority(priorityVal.asInt());
                }
                rows.add(row);
            }
            return deduplicateDiagnosisRows(rows);
        } catch (Exception e) {
            log.error("图谱导出查询失败", e);
            throw new KnowledgeGraphException("GRAPH_EXPORT_ERROR",
                    "图谱导出查询失败: " + e.getMessage(), e);
        }
    }

    private SymptomDiagnosisRow mapRecordToRow(Record record) {
        return SymptomDiagnosisRow.builder()
                .symptom(getString(record, "symptom"))
                .disease(getString(record, "disease"))
                .diseaseCode(getString(record, "diseaseCode"))
                .icdCode(getString(record, "icdCode"))
                .icdDescription(getString(record, "icdDescription"))
                .weight(getDouble(record, "weight"))
                .build();
    }

    private String getString(Record record, String key) {
        Value value = record.get(key);
        return value.isNull() ? null : value.asString();
    }

    private Double getDouble(Record record, String key) {
        Value value = record.get(key);
        if (value.isNull()) {
            return null;
        }
        return value.asDouble();
    }

    private QueryResultDTO buildSymptomDiagnosisResult(
            List<QueryResultDTO.NodeResult> nodes,
            List<QueryResultDTO.RelationResult> relations,
            List<SymptomDiagnosisRow> rows,
            long startTime) {
        int count = rows.size();
        return QueryResultDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .query(SYMPTOM_DIAGNOSIS_GRAPH_CYPHER)
                .queryType("SYMPTOM_DIAGNOSIS")
                .executionTime(System.currentTimeMillis() - startTime)
                .totalCount(count)
                .nodes(nodes)
                .relations(relations)
                .paths(new ArrayList<>())
                .records(rows.stream().map(this::rowToMap).collect(Collectors.toList()))
                .pagination(QueryResultDTO.Pagination.builder()
                        .page(1)
                        .pageSize(Math.max(count, 1))
                        .totalPages(count > 0 ? 1 : 0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .build();
    }

    private void mergeGraphEntities(
            Record record,
            Map<Long, QueryResultDTO.NodeResult> nodeById,
            Map<Long, QueryResultDTO.RelationResult> relById) {
        for (String key : List.of("s", "d", "i")) {
            Value value = record.get(key);
            if (!value.isNull() && value.type().name().startsWith("NODE")) {
                Node node = value.asNode();
                nodeById.putIfAbsent(node.id(), extractNode(node));
            }
        }
        for (String key : List.of("r", "c")) {
            Value value = record.get(key);
            if (!value.isNull() && value.type().name().startsWith("RELATIONSHIP")) {
                Relationship rel = value.asRelationship();
                relById.putIfAbsent(rel.id(), extractRelation(rel));
            }
        }
    }

    private List<SymptomDiagnosisRow> deduplicateDiagnosisRows(List<SymptomDiagnosisRow> rows) {
        LinkedHashMap<String, SymptomDiagnosisRow> unique = new LinkedHashMap<>();
        for (SymptomDiagnosisRow row : rows) {
            unique.putIfAbsent(diagnosisRecordKey(row), row);
        }
        return new ArrayList<>(unique.values());
    }

    private String diagnosisRecordKey(SymptomDiagnosisRow row) {
        return Objects.toString(row.getDisease(), "") + "|" + Objects.toString(row.getIcdCode(), "");
    }

    private Map<String, Object> rowToMap(SymptomDiagnosisRow row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("symptom", row.getSymptom());
        map.put("disease", row.getDisease());
        map.put("diseaseCode", row.getDiseaseCode());
        map.put("icdCode", row.getIcdCode());
        map.put("icdDescription", row.getIcdDescription());
        map.put("weight", row.getWeight());
        return map;
    }

    /**
     * 查询药品的适应症
     */
    public QueryResultDTO findDrugIndications(String drugName) {
        String cypher = 
                "MATCH (d:Drug {name: $name})-[:TREATS]->(disease:Disease) " +
                "RETURN d.name AS drug, disease.name AS disease, " +
                "       disease.icd10Code AS icdCode";

        Map<String, Object> params = Collections.singletonMap("name", drugName);
        return executeQuery(cypher, params);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        String[] labels = {"Symptom", "Disease", "Drug", "DrugEffect", "ICD10"};
        
        try (Session session = neo4jDriver.session()) {
            for (String label : labels) {
                String cypher = String.format("MATCH (n:%s) RETURN count(n) AS count", label);
                Result result = session.run(cypher);
                if (result.hasNext()) {
                    stats.put(label, result.next().get("count").asLong());
                }
            }
            
            // 统计关系数量
            String relCypher = "MATCH ()-[r]->() RETURN count(r) AS count";
            Result relResult = session.run(relCypher);
            if (relResult.hasNext()) {
                stats.put("Relationships", relResult.next().get("count").asLong());
            }
        }
        
        return stats;
    }

    // ==================== 辅助方法 ====================

    private void processRecord(org.neo4j.driver.Record record, QueryResultDTO result) {
        for (String key : record.keys()) {
            Value value = record.get(key);
            
            if (value.type().name().startsWith("NODE")) {
                QueryResultDTO.NodeResult node = extractNode(value.asNode());
                result.getNodes().add(node);
            } else if (value.type().name().startsWith("RELATIONSHIP")) {
                QueryResultDTO.RelationResult rel = extractRelation(value.asRelationship());
                result.getRelations().add(rel);
            }
        }
    }

    private QueryResultDTO.NodeResult extractNode(Node node) {
        Map<String, Object> props = new HashMap<>();
        node.keys().forEach(key -> props.put(key, node.get(key).asObject()));

        return QueryResultDTO.NodeResult.builder()
                .id(String.valueOf(node.id()))
                .label(node.labels().iterator().next())
                .name(resolveNodeDisplayName(node))
                .properties(props)
                .build();
    }

    private String resolveNodeDisplayName(Node node) {
        if (node.containsKey("name") && !node.get("name").isNull()) {
            return node.get("name").asString();
        }
        if (node.containsKey("code") && !node.get("code").isNull()) {
            return node.get("code").asString();
        }
        return "";
    }

    private QueryResultDTO.RelationResult extractRelation(Relationship rel) {
        Map<String, Object> props = new HashMap<>();
        rel.keys().forEach(key -> props.put(key, rel.get(key).asObject()));
        
        return QueryResultDTO.RelationResult.builder()
                .sourceId(String.valueOf(rel.startNodeId()))
                .targetId(String.valueOf(rel.endNodeId()))
                .type(rel.type())
                .properties(props)
                .build();
    }

    private Map<String, Object> buildRelationProperties(KnowledgeRelation relation) {
        Map<String, Object> props = new HashMap<>();
        if (relation.getDescription() != null) {
            props.put("description", relation.getDescription());
        }
        if (relation.getWeight() != null) {
            props.put("weight", relation.getWeight());
        }
        if (relation.getPriority() != null) {
            props.put("priority", relation.getPriority());
        }
        props.put("createTime", LocalDateTime.now().toString());
        return props;
    }

    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 清空所有数据（谨慎使用）
     */
    public void clearAll() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("MATCH (n) DETACH DELETE n");
                return null;
            });
            log.warn("已清空所有节点和关系");
        }
    }

    /**
     * 创建索引
     */
    public void createIndex(String label, String property) {
        String cypher = String.format(
                "CREATE INDEX IF NOT EXISTS FOR (n:%s) ON (n.%s)",
                label,
                property
        );
        
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher);
                return null;
            });
            log.info("创建索引: label={}, property={}", label, property);
        } catch (Exception e) {
            log.warn("创建索引失败，可能已存在: label={}, property={}", label, property);
        }
    }

    /**
     * 创建约束
     */
    public void createConstraint(String label, String property) {
        String cypher = String.format(
                "CREATE CONSTRAINT FOR (n:%s) REQUIRE n.%s IS UNIQUE",
                label,
                property
        );
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher);
            log.info("创建约束: label={}, property={}", label, property);
        } catch (Exception e) {
            log.warn("创建约束失败，可能已存在: label={}, property={}", label, property);
        }
    }
}
