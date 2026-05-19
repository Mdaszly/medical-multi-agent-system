# 知识图谱模块技术文档

## 一、项目概述

本知识图谱模块是基于Java和Neo4j开发的医疗领域知识图谱构建系统，用于存储和管理症状、疾病、ICD编码、药品、药效等实体及其关系。

## 二、技术架构

### 2.1 技术栈

- **编程语言**: Java 17
- **图数据库**: Neo4j 5.x
- **框架**: Spring Boot 3.2
- **构建工具**: Maven
- **Neo4j驱动**: neo4j-java-driver 5.24.0

### 2.2 模块结构

```
knowledgegraph/
├── config/                    # 配置类
│   ├── Neo4jConfig.java       # Neo4j数据库配置
│   └── Neo4jProperties.java   # Neo4j属性配置
├── controller/                # REST API控制器
│   └── KnowledgeGraphController.java
├── exception/                 # 异常处理
│   ├── GlobalExceptionHandler.java
│   └── KnowledgeGraphException.java
├── model/
│   ├── dto/                  # 数据传输对象
│   │   ├── ImportTaskDTO.java
│   │   └── QueryResultDTO.java
│   └── entity/               # 实体模型
│       ├── BaseNode.java     # 节点基类
│       ├── Symptom.java      # 症状实体
│       ├── IcdCode.java      # ICD编码实体
│       ├── Drug.java         # 药品实体
│       ├── DrugEffect.java   # 药效实体
│       ├── Disease.java      # 疾病实体
│       └── KnowledgeRelation.java  # 关系模型
├── service/
│   ├── datainput/            # 数据导入服务
│   │   ├── DataImportService.java
│   │   ├── CsvImportService.java
│   │   └── JsonImportService.java
│   ├── extraction/           # 实体关系抽取
│   │   └── EntityExtractionService.java
│   └── neo4j/                # Neo4j核心服务
│       └── KnowledgeGraphService.java
└── resources/
    ├── application.yml       # 应用配置
    └── logback-spring.xml    # 日志配置
```

## 三、数据模型

### 3.1 实体类型

| 实体类型 | 标签 | 主要属性 |
|---------|------|---------|
| 症状 | `Symptom` | name, code, categoryCode, severity, bodyPart |
| 疾病 | `Disease` | name, diseaseCode, icd10Code, category, system |
| ICD编码 | `ICD10` | code, descriptionEn, descriptionCn, chapterCode |
| 药品 | `Drug` | name, drugCode, genericName, category, manufacturer |
| 药效 | `DrugEffect` | name, effectCode, category, indications |

### 3.2 关系类型

| 关系类型 | 含义 | 方向 |
|---------|------|------|
| `INDICATES` | 症状指示疾病 | Symptom → Disease |
| `HAS_SYMPTOM` | 疾病具有症状 | Disease → Symptom |
| `TREATS` | 药品治疗疾病 | Drug → Disease |
| `HAS_EFFECT` | 药品具有药效 | Drug → DrugEffect |
| `APPLIES_TO` | 药效适用于疾病 | DrugEffect → Disease |
| `CLASSIFIED_AS` | 分类为ICD | Disease → ICD10 |
| `ASSOCIATED_WITH` | 一般关联 | 任意 → 任意 |

## 四、API接口文档

### 4.1 数据导入

#### POST /api/knowledge-graph/import
导入数据文件

**请求参数**:
- `file`: MultipartFile (必填)
- `entityType`: String (必填) - symptom|icd10|drug|drugeffect|disease|relation
- `skipHeader`: Boolean (可选, 默认true)
- `delimiter`: String (可选, 默认",")

**响应示例**:
```json
{
  "taskId": "uuid",
  "status": "COMPLETED",
  "totalRecords": 100,
  "successCount": 98,
  "failureCount": 2,
  "errors": ["行3: 数据格式错误"]
}
```

### 4.2 查询接口

#### POST /api/knowledge-graph/query
执行Cypher查询

**请求体**:
```json
{
  "cypher": "MATCH (s:Symptom {name: $name})-[:INDICATES]->(d:Disease) RETURN s, d",
  "params": {"name": "头痛"}
}
```

**响应示例**:
```json
{
  "queryId": "xxx",
  "query": "MATCH ...",
  "executionTime": 25,
  "totalCount": 5,
  "nodes": [
    {"id": "1", "label": "Symptom", "name": "头痛", "properties": {...}}
  ],
  "relations": [...]
}
```

#### GET /api/knowledge-graph/node/{label}/{name}
根据名称查询节点

#### GET /api/knowledge-graph/diagnosis/{symptomName}
查询症状可能的诊断

**响应示例**:
```json
{
  "nodes": [
    {"name": "头痛", "label": "Symptom"},
    {"name": "偏头痛", "label": "Disease"}
  ],
  "relations": [
    {"sourceId": "1", "targetId": "2", "type": "INDICATES", "properties": {"weight": 0.8}}
  ]
}
```

### 4.3 实体抽取

#### POST /api/knowledge-graph/extract
从文本中提取实体

**请求体**:
```json
{"text": "患者主诉头痛、发热三天，伴有咳嗽"}
```

**响应示例**:
```json
{
  "symptoms": [
    {"name": "头痛", "code": "EXT_xxx", "categoryName": "疼痛类"},
    {"name": "发热", "code": "EXT_xxx", "categoryName": "发热类"},
    {"name": "咳嗽", "code": "EXT_xxx", "categoryName": "呼吸系统"}
  ],
  "diseases": [],
  "symptomCount": 3,
  "diseaseCount": 0
}
```

### 4.4 管理接口

#### GET /api/knowledge-graph/statistics
获取图谱统计信息

**响应示例**:
```json
{
  "Symptom": 150,
  "Disease": 200,
  "Drug": 500,
  "ICD10": 1200,
  "DrugEffect": 300,
  "Relationships": 2500
}
```

#### DELETE /api/knowledge-graph/clear
清空所有数据

## 五、数据格式要求

### 5.1 CSV文件格式

**症状数据 (symptom)**:
```csv
id,name,code,category_code,category_name,description,body_part,pinyin,frequency,severity,urgent
1,头痛,HEADACHE,PAIN,疼痛类,头部疼痛,头部,toutong,5,2,false
```

**ICD编码数据 (icd10)**:
```csv
code,description_en,description_cn,chapter_code,chapter_name,disease_class,medical_insurance,drg_code
J18.9,Pneumonia,肺炎, X,呼吸系统疾病,呼吸系统感染,true,E61
```

**关系数据 (relation)**:
```csv
source_name,source_label,target_name,target_label,type,description,weight,priority
头痛,Symptom,偏头痛,Disease,INDICATES,头痛指示偏头痛,0.8,1
```

### 5.2 JSON文件格式

```json
{
  "data": [
    {
      "name": "头痛",
      "code": "HEADACHE",
      "category_name": "疼痛类"
    }
  ]
}
```

## 六、快速开始

### 6.1 环境要求

- Java 17+
- Maven 3.8+
- Neo4j 5.x
- Docker (可选)

### 6.2 启动Neo4j

```bash
# 使用Docker启动
docker run -d \
  --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/neo4jpass \
  neo4j:5-community
```

### 6.3 编译运行

```bash
cd knowledgegraph
mvn clean compile
mvn spring-boot:run
```

### 6.4 测试示例

```bash
# 导入症状数据
curl -X POST -F "file=@sample/symptom_sample.csv" \
     -F "entityType=symptom" \
     http://localhost:8080/api/knowledge-graph/import

# 查询症状诊断
curl http://localhost:8080/api/knowledge-graph/diagnosis/头痛

# 提取文本实体
curl -X POST -H "Content-Type: application/json" \
     -d '{"text":"患者头痛发热三天"}' \
     http://localhost:8080/api/knowledge-graph/extract
```

## 七、性能优化建议

1. **批量导入**: 使用批量导入接口，避免单条插入
2. **索引优化**: 为常用查询属性创建索引
3. **约束建立**: 为唯一标识属性建立约束
4. **连接池**: 根据并发量调整连接池大小

## 八、扩展指南

### 8.1 添加新实体类型

1. 继承 `BaseNode` 类
2. 实现 `toNeo4jProperties()` 方法
3. 在 `DataImportService` 中添加映射逻辑

### 8.2 添加新关系类型

1. 在 `KnowledgeRelation` 类中添加常量
2. 实现关系创建逻辑
3. 更新查询模板

## 九、故障排查

### 9.1 Neo4j连接失败

检查配置：
```yaml
app.neo4j:
  uri: bolt://localhost:7687
  username: neo4j
  password: neo4jpass
```

### 9.2 数据导入失败

检查文件格式是否正确，字段映射是否匹配。

### 9.3 查询性能问题

- 创建适当的索引
- 使用参数化查询
- 避免全图扫描

## 十、联系方式

技术支持: medical@example.com
文档版本: 1.0.0
更新日期: 2026-05-19
