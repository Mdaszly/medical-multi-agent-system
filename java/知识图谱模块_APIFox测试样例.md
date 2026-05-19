# 知识图谱模块 - APIFox 测试样例

## 📋 文档说明

| 项目信息   | 说明                             |
| ------ | ------------------------------ |
| 模块名称   | 医疗知识图谱服务                      |
| 测试方案版本 | v1.0                           |
| 创建日期   | 2026-05-19                     |
| 测试工具   | Apifox                         |
| 测试环境   | 开发环境 (`http://localhost:8080`) |

---

## 🔧 环境配置

### 环境变量设置

在 Apifox 中配置以下环境变量：

| 变量名           | 示例值                     | 说明                |
| ------------- | ----------------------- | ----------------- |
| `baseUrl`     | `http://localhost:8080` | 基础URL             |
| `neo4jHost`   | `localhost`             | Neo4j数据库主机       |
| `neo4jPort`   | `7687`                 | Neo4j Bolt端口       |
| `neo4jUser`   | `neo4j`                | Neo4j用户名          |
| `neo4jPass`   | `neo4jpass`            | Neo4j密码           |

---

## 📝 测试用例详细说明

---

### 一、服务健康检查

#### 1.1 健康检查接口

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 健康检查                                   |
| **请求方法**  | `GET`                                  |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/health` |
| **请求头**   | `Content-Type: application/json`       |

**预期响应:**

```json
{
  "status": "UP",
  "service": "Knowledge Graph Service"
}
```

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.status | 等于 | "UP" |
| response.service | 等于 | "Knowledge Graph Service" |

---

### 二、数据导入测试

#### 2.1 导入症状数据文件

**Apifox 测试样例**

| 项目        | 内容                                           |
| --------- | -------------------------------------------- |
| **接口名称**  | 导入数据文件                                       |
| **请求方法**  | `POST`                                         |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/import`       |
| **请求头**   | `Content-Type: multipart/form-data`           |
| **前置条件**  | 准备 CSV 或 JSON 格式的数据文件                      |

**请求参数 (form-data):**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| file | File | 是 | - | 数据文件 (CSV/JSON) |
| **entityType** | String | **是** | - | **实体类型 (symptom/icd10/drug/drugeffect/disease/relation)** |
| skipHeader | Boolean | 否 | true | 是否跳过首行 |
| delimiter | String | 否 | , | CSV分隔符 |

**⚠️ 重要说明**

`entityType` 参数**必须**严格匹配以下值（**小写**）：

| entityType值 | 对应实体 | 说明 |
| ---- | ---- | ---- |
| `symptom` | 症状实体 | 导入症状数据 |
| `icd10` | ICD-10编码 | 导入ICD-10编码数据 |
| `drug` | 药品实体 | 导入药品数据 |
| `drugeffect` | 药效实体 | 导入药效数据 |
| `disease` | 疾病实体 | 导入疾病数据 |
| `relation` | 关系数据 | 导入实体间关系 |

**正确的示例文件内容 (symptoms_data_standard.csv):**

```csv
id,name,code,category_code,category_name,description,body_part,pinyin,frequency,severity,urgent
1,头痛,S001,N01,神经系统,头部疼痛的感觉,头部,toutong,3,2,false
2,发热,S002,N02,全身性症状,体温升高超过正常范围,全身,fare,3,2,false
3,咳嗽,S003,R01,呼吸系统,呼吸道分泌物排出动作,呼吸道,kesou,3,1,false
```

**预期响应:**

```json
{
  "taskId": "import-20260519-001",
  "status": "COMPLETED",
  "entityType": "SYMPTOM",
  "fileName": "symptom_sample.csv",
  "totalRecords": 3,
  "successCount": 3,
  "failedCount": 0,
  "message": "导入成功",
  "createTime": "2026-05-19T10:30:00"
}
```

**后置操作**: 保存 `taskId` 到环境变量 `{{importTaskId}}`

**详细配置步骤:**

1. **打开后置操作面板**: 在Apifox接口编辑页面，点击顶部的 **后置操作** 标签
2. **添加后置操作**: 点击 **+** 按钮，选择 **提取变量**
3. **配置变量提取**:
   - **变量名称**: `importTaskId`（直接输入，不需要加`{{}}`）
   - **变量类型**: `环境变量`
   - **提取源**: `Response JSON`
   - **提取方式**: `JSONPath`
   - **JSONPath表达式**: `$.taskId`
   - **是否启用**: 勾选启用

**配置界面示意:**

```
┌─────────────────────────────────────────────────────────┐
│  后置操作 → 提取变量                                    │
├───────────────┬───────────────────────────────────────┤
│ 变量名称      │ importTaskId                           │  ← 直接输入，不加{{}}
│ 变量类型      │ 环境变量                                │  ← 选择"环境变量"
│ 提取源        │ Response JSON                          │
│ 提取方式      │ JSONPath                               │
│ JSONPath表达式│ $.taskId                               │  ← 提取响应中的taskId字段
│ 是否启用      │ ☑ 启用                                │
└───────────────┴───────────────────────────────────────┘
```

**注意事项:**
- ✅ `importTaskId` 是环境变量名，不需要加 `${{}}`，Apifox会自动添加
- ✅ JSONPath表达式必须正确：`$.taskId` 表示从响应JSON根节点提取taskId字段
- ✅ 确保响应中包含taskId字段，否则提取会失败
- ✅ 配置完成后，后续接口可以使用 `{{importTaskId}}` 引用该变量

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.status | 等于 | "COMPLETED" |
| response.successCount | 大于 | 0 |

---

#### 2.2 查询导入任务状态

**Apifox 测试样例**

| 项目        | 内容                                                 |
| --------- | -------------------------------------------------- |
| **接口名称**  | 获取导入任务状态                                          |
| **请求方法**  | `GET`                                              |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/import/{{importTaskId}}` |
| **请求头**   | `Content-Type: application/json`                   |

**预期响应:**

```json
{
  "taskId": "import-20260519-001",
  "status": "COMPLETED",
  "entityType": "SYMPTOM",
  "fileName": "symptom_sample.csv",
  "totalRecords": 3,
  "successCount": 3,
  "failedCount": 0,
  "message": "导入成功",
  "createTime": "2026-05-19T10:30:00"
}
```

---

### 三、节点查询测试

#### 3.1 根据名称查询节点

**Apifox 测试样例**

| 项目        | 内容                                             |
| --------- | ---------------------------------------------- |
| **接口名称**  | 根据名称查询节点                                      |
| **请求方法**  | `GET`                                          |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/node/{label}/{name}` |
| **请求头**   | `Content-Type: application/json`               |

**路径参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| label | String | 是 | Symptom | 节点标签 |
| name | String | 是 | 头痛 | 节点名称 |

**完整请求URL示例:**
```
{{baseUrl}}/api/knowledge-graph/node/Symptom/头痛
```

**预期响应:**

```json
{
  "queryId": "q1",
  "query": "MATCH (n:Symptom) WHERE n.name CONTAINS $name RETURN n",
  "queryType": "CYPHER",
  "executionTime": 15,
  "totalCount": 1,
  "nodes": [
    {
      "id": "1",
      "label": "Symptom",
      "name": "头痛",
      "properties": {
        "name": "头痛",
        "description": "头部疼痛的感觉",
        "icdCode": "R51",
        "severity": "中等",
        "frequency": "常见"
      }
    }
  ],
  "relations": [],
  "paths": []
}
```

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.totalCount | 大于等于 | 1 |
| response.nodes[0].label | 等于 | "Symptom" |
| response.nodes[0].name | 包含 | "头痛" |

---

#### 3.2 查询节点关联关系

**Apifox 测试样例**

| 项目        | 内容                                                   |
| --------- | ---------------------------------------------------- |
| **接口名称**  | 查询节点关联关系                                          |
| **请求方法**  | `GET`                                                |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/relations/{label}/{name}` |
| **请求头**   | `Content-Type: application/json`                     |

**路径参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| label | String | 是 | Symptom | 节点标签 |
| name | String | 是 | 头痛 | 节点名称 |

**查询参数:**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| depth | int | 否 | 1 | 查询深度 |

**完整请求URL示例:**
```
{{baseUrl}}/api/knowledge-graph/relations/Symptom/头痛?depth=2
```

**预期响应:**

```json
{
  "queryId": "q2",
  "queryType": "CYPHER",
  "executionTime": 25,
  "totalCount": 3,
  "nodes": [
    {
      "id": "1",
      "label": "Symptom",
      "name": "头痛",
      "properties": {...}
    },
    {
      "id": "10",
      "label": "Disease",
      "name": "偏头痛",
      "properties": {...}
    },
    {
      "id": "11",
      "label": "Disease",
      "name": "高血压",
      "properties": {...}
    }
  ],
  "relations": [
    {
      "sourceId": "1",
      "targetId": "10",
      "type": "INDICATES",
      "properties": {
        "weight": 0.8,
        "description": "症状指示疾病"
      }
    },
    {
      "sourceId": "1",
      "targetId": "11",
      "type": "INDICATES",
      "properties": {
        "weight": 0.6,
        "description": "症状指示疾病"
      }
    }
  ],
  "paths": []
}
```

---

### 四、业务查询测试

#### 4.1 查询症状诊断（核心业务）

**Apifox 测试样例**

| 项目        | 内容                                               |
| --------- | ------------------------------------------------ |
| **接口名称**  | 查询症状诊断                                          |
| **请求方法**  | `GET`                                            |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/diagnosis/{symptomName}` |
| **请求头**   | `Content-Type: application/json`                 |

**路径参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| symptomName | String | 是 | 头痛 | 症状名称 |

**完整请求URL示例:**
```
{{baseUrl}}/api/knowledge-graph/diagnosis/头痛
```

**预期响应:**

```json
{
  "queryId": "q3",
  "queryType": "CYPHER",
  "executionTime": 30,
  "totalCount": 2,
  "nodes": [
    {
      "id": "1",
      "label": "Symptom",
      "name": "头痛",
      "properties": {
        "name": "头痛",
        "icdCode": "R51"
      }
    },
    {
      "id": "10",
      "label": "Disease",
      "name": "偏头痛",
      "properties": {
        "name": "偏头痛",
        "diseaseCode": "G43",
        "icd10Code": "G43.9"
      }
    },
    {
      "id": "20",
      "label": "ICD10",
      "name": "G43.9",
      "properties": {
        "code": "G43.9",
        "descriptionCn": "偏头痛，未特指"
      }
    }
  ],
  "relations": [
    {
      "sourceId": "1",
      "targetId": "10",
      "type": "INDICATES",
      "properties": {...}
    },
    {
      "sourceId": "10",
      "targetId": "20",
      "type": "CLASSIFIED_AS",
      "properties": {...}
    }
  ]
}
```

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.totalCount | 大于 | 0 |
| response.nodes | 包含标签 | "ICD10" |

---

#### 4.2 查询药品适应症

**Apifox 测试样例**

| 项目        | 内容                                                   |
| --------- | ---------------------------------------------------- |
| **接口名称**  | 查询药品适应症                                            |
| **请求方法**  | `GET`                                                |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/drug-indications/{drugName}` |
| **请求头**   | `Content-Type: application/json`                     |

**路径参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| drugName | String | 是 | 布洛芬 | 药品名称 |

**预期响应:**

```json
{
  "queryId": "q4",
  "queryType": "CYPHER",
  "executionTime": 20,
  "totalCount": 3,
  "nodes": [
    {
      "id": "50",
      "label": "Drug",
      "name": "布洛芬",
      "properties": {
        "name": "布洛芬",
        "specification": "0.3g*20粒"
      }
    },
    {
      "id": "10",
      "label": "Disease",
      "name": "偏头痛",
      "properties": {...}
    },
    {
      "id": "15",
      "label": "Disease",
      "name": "关节炎",
      "properties": {...}
    }
  ],
  "relations": [
    {
      "sourceId": "50",
      "targetId": "10",
      "type": "TREATS",
      "properties": {
        "effectiveness": "高"
      }
    },
    {
      "sourceId": "50",
      "targetId": "15",
      "type": "TREATS",
      "properties": {
        "effectiveness": "中"
      }
    }
  ]
}
```

---

#### 4.3 查找两个节点间的路径

**Apifox 测试样例**

| 项目        | 内容                                         |
| --------- | ------------------------------------------ |
| **接口名称**  | 查找节点路径                                     |
| **请求方法**  | `GET`                                      |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/paths`     |
| **请求头**   | `Content-Type: application/json`           |

**查询参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| sourceLabel | String | 是 | Symptom | 源节点标签 |
| sourceName | String | 是 | 头痛 | 源节点名称 |
| targetLabel | String | 是 | Drug | 目标节点标签 |
| targetName | String | 是 | 布洛芬 | 目标节点名称 |
| maxDepth | int | 否 | 5 | 最大路径深度 |

**完整请求URL示例:**
```
{{baseUrl}}/api/knowledge-graph/paths?sourceLabel=Symptom&sourceName=头痛&targetLabel=Drug&targetName=布洛芬&maxDepth=3
```

**预期响应:**

```json
{
  "queryId": "q5",
  "queryType": "CYPHER",
  "executionTime": 45,
  "totalCount": 1,
  "nodes": [...],
  "relations": [...],
  "paths": [
    {
      "nodes": ["头痛", "偏头痛", "布洛芬"],
      "relationships": ["INDICATES", "TREATS"],
      "weight": 1.4
    }
  ]
}
```

---

### 五、Cypher查询测试

#### 5.1 执行Cypher查询

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 执行Cypher查询                              |
| **请求方法**  | `POST`                                  |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/query` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "cypher": "MATCH (n:Symptom) RETURN n.name, n.icdCode LIMIT 5",
  "params": {}
}
```

**预期响应:**

```json
{
  "queryId": "q6",
  "query": "MATCH (n:Symptom) RETURN n.name, n.icdCode LIMIT 5",
  "queryType": "CYPHER",
  "executionTime": 10,
  "totalCount": 5,
  "nodes": [
    {
      "id": "1",
      "label": "Symptom",
      "name": "头痛",
      "properties": {
        "name": "头痛",
        "icdCode": "R51"
      }
    },
    {
      "id": "2",
      "label": "Symptom",
      "name": "发热",
      "properties": {
        "name": "发热",
        "icdCode": "R50"
      }
    }
  ],
  "relations": [],
  "paths": []
}
```

---

### 六、管理操作测试

#### 6.1 获取统计信息

**Apifox 测试样例**

| 项目        | 内容                                         |
| --------- | ------------------------------------------ |
| **接口名称**  | 获取知识图谱统计信息                               |
| **请求方法**  | `GET`                                      |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/statistics` |
| **请求头**   | `Content-Type: application/json`           |

**预期响应:**

```json
{
  "Symptom": 150,
  "Disease": 200,
  "Drug": 100,
  "DrugEffect": 50,
  "ICD10": 1000,
  "Relationships": 500
}
```

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.Symptom | 大于等于 | 0 |
| response.Disease | 大于等于 | 0 |
| response.Relationships | 大于等于 | 0 |

---

#### 6.2 创建索引

**Apifox 测试样例**

| 项目        | 内容                                       |
| --------- | ---------------------------------------- |
| **接口名称**  | 创建节点索引                                   |
| **请求方法**  | `POST`                                    |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/index`   |
| **请求头**   | `Content-Type: application/json`         |

**查询参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| label | String | 是 | Symptom | 节点标签 |
| property | String | 是 | name | 属性名称 |

**完整请求URL示例:**
```
{{baseUrl}}/api/knowledge-graph/index?label=Symptom&property=name
```

**预期响应:**

```json
{
  "message": "索引创建成功"
}
```

---

#### 6.3 创建约束

**Apifox 测试样例**

| 项目        | 内容                                           |
| --------- | -------------------------------------------- |
| **接口名称**  | 创建唯一性约束                                     |
| **请求方法**  | `POST`                                         |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/constraint`   |
| **请求头**   | `Content-Type: application/json`               |

**查询参数:**

| 参数名 | 类型 | 必填 | 示例值 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| label | String | 是 | ICD10 | 节点标签 |
| property | String | 是 | code | 属性名称 |

**预期响应:**

```json
{
  "message": "约束创建成功"
}
```

---

### 七、实体抽取测试

#### 7.1 从文本提取实体

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 从文本提取实体                               |
| **请求方法**  | `POST`                                  |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/extract` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "text": "患者主诉头痛、发热3天，伴有咳嗽。既往有高血压病史。"
}
```

**预期响应:**

```json
{
  "symptoms": ["头痛", "发热", "咳嗽"],
  "diseases": ["高血压"],
  "symptomCount": 3,
  "diseaseCount": 1
}
```

**断言规则:**

| 断言项 | 断言条件 | 期望结果 |
| ---- | ---- | ---- |
| HTTP状态码 | 等于 | 200 |
| response.symptomCount | 大于 | 0 |
| response.symptoms | 包含 | "头痛" |

---

#### 7.2 从病历提取并构建图谱

**Apifox 测试样例**

| 项目        | 内容                                               |
| --------- | ------------------------------------------------ |
| **接口名称**  | 从病历提取并构建知识图谱                                  |
| **请求方法**  | `POST`                                            |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/extract-from-record` |
| **请求头**   | `Content-Type: application/json`                 |

**请求体 (Body - JSON):**

```json
{
  "recordText": "【主诉】头痛、发热3天。【现病史】患者3天前无明显诱因出现头痛，伴发热，体温最高38.5℃，自行服用布洛芬后体温下降，但头痛缓解不明显。【既往史】高血压病史5年，规律服用降压药。【体格检查】血压145/95mmHg，神志清楚，精神尚可。【初步诊断】1. 上呼吸道感染；2. 高血压。"
}
```

**预期响应:**

```json
{
  "message": "知识图谱构建完成"
}
```

---

### 八、数据清理测试（谨慎使用）

#### 8.1 清空所有数据

**Apifox 测试样例**

| 项目        | 内容                                   |
| --------- | ------------------------------------ |
| **接口名称**  | 清空所有知识图谱数据                          |
| **请求方法**  | `DELETE`                             |
| **请求URL** | `{{baseUrl}}/api/knowledge-graph/clear` |
| **请求头**   | `Content-Type: application/json`     |
| **警告**     | 此操作将删除所有数据，谨慎使用！                 |

**预期响应:**

```json
{
  "message": "数据已清空"
}
```

**验证步骤:**
1. 调用清空接口
2. 调用统计接口验证数据已清空
3. 确认所有节点计数为0

---

## 📊 测试检查清单

### 功能测试

| 测试项 | 测试状态 | 备注 |
| ---- | ---- | ---- |
| 服务健康检查 | ☐ | 验证服务是否正常运行 |
| 数据导入功能 | ☐ | CSV/JSON文件导入 |
| 节点查询功能 | ☐ | 根据标签和名称查询 |
| 关系查询功能 | ☐ | 查询节点关联关系 |
| 症状诊断查询 | ☐ | 核心业务：症状→疾病→ICD编码 |
| 药品适应症查询 | ☐ | 查询药品治疗的疾病 |
| 路径查找功能 | ☐ | 查找节点间路径 |
| Cypher查询执行 | ☐ | 执行自定义Cypher |
| 统计信息获取 | ☐ | 获取节点和关系统计 |
| 索引/约束管理 | ☐ | 创建索引和约束 |
| 实体抽取功能 | ☐ | 从文本提取实体 |

### 边界测试

| 测试项 | 测试状态 | 备注 |
| ---- | ---- | ---- |
| 空文件导入 | ☐ | 应返回错误 |
| 不存在的节点查询 | ☐ | 应返回空结果 |
| 无效Cypher语句 | ☐ | 应返回错误信息 |
| 超深度路径查询 | ☐ | 应有限制或超时 |

---

## 🎯 测试执行顺序建议

```
阶段一：服务验证
    1. 健康检查

阶段二：数据准备
    2. 导入症状数据
    3. 导入疾病数据
    4. 导入ICD编码数据
    5. 导入药品数据

阶段三：核心查询测试
    6. 查询症状节点
    7. 查询症状关联关系
    8. 查询症状诊断（核心业务）
    9. 查询药品适应症
    10. 查找节点路径

阶段四：高级功能测试
    11. 执行Cypher查询
    12. 获取统计信息
    13. 创建索引和约束
    14. 实体抽取测试

阶段五：清理（可选）
    15. 清空所有数据
```

---

## 🔍 断言规则汇总

### 通用断言

| 断言项 | 条件 | 期望值 |
| ---- | ---- | ---- |
| HTTP状态码 | == | 200 |
| response.code | == | 0 (如有) |
| response.message | != | null |

### 查询结果断言

| 断言项 | 条件 | 期望值 |
| ---- | ---- | ---- |
| response.totalCount | >= | 0 |
| response.nodes | != | null |
| response.executionTime | < | 500 (ms) |

---

**文档版本**: v1.0  
**最后更新**: 2026-05-19  
**维护人员**: 测试团队