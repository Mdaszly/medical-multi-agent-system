# Coding Agent 运行逻辑深度分析

> **Workspace:** d:\IDEA\multi-agent

---

## 一、核心功能定位

**角色类比**：认证医学编码专家（Certified Coding Specialist, CCS）

**核心职责**：基于诊断结果和治疗方案，将临床信息映射为标准化的ICD-10-CM编码和DRGs（疾病诊断相关分组），为医疗计费、统计分析和医保支付提供标准化数据支撑。

**独特价值**：
- 连接临床决策与医疗管理的桥梁
- 提供ICD-10编码的置信度评估
- 自动生成DRGs分组，支持医保支付
- 保留编码说明和依据，便于审计追溯

---

## 二、功能模块划分

| 模块 | 职责 | 对应方法/代码行 |
|------|------|----------------|
| **依赖注入** | LlmService和ObjectMapper初始化 | 构造函数 第31-34行 |
| **输入验证** | 检查诊断结果是否存在 | `process()` 第42-45行 |
| **上下文构建** | 合并诊断结果与治疗方案 | `process()` 第49-53行 |
| **LLM调用** | 调用认证编码专家角色生成编码 | `process()` 第56-57行 |
| **响应清理** | 去除markdown代码块格式 | `cleanJsonResponse()` 第74-82行 |
| **结果解析** | 解析JSON并写入状态 | `process()` 第60-62行 |
| **异常处理** | 捕获并记录处理过程中的错误 | `process()` 第65-68行 |

---

## 三、类与方法调用关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CodingAgent                                  │
├─────────────────────────────────────────────────────────────────────┤
│  process(ClinicalState)  ←  外部调用入口（ClinicalPipeline）       │
│       │                                                             │
│       ├─→ 输入验证：检查diagnosis是否为空                           │
│       │                                                             │
│       ├─→ 构建context：diagnosis + treatment_plan                  │
│       │                                                             │
│       ├─→ ObjectMapper.writeValueAsString()  ──→ JSON序列化       │
│       │                                                             │
│       ├─→ LlmService.generate()  ──→ 调用LLM生成ICD-10编码        │
│       │       │                                                     │
│       │       └─→ DashScopeService / ChatClient                     │
│       │                                                             │
│       ├─→ cleanJsonResponse()  ──→ 清理markdown格式               │
│       │                                                             │
│       ├─→ ObjectMapper.readValue()  ──→ JSON反序列化              │
│       │                                                             │
│       └─→ state.setCodingResult()  ──→ 写入编码结果                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 四、核心业务逻辑流程

### 4.1 完整执行流程图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      CodingAgent.process()                               │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  输入: ClinicalState (包含diagnosis, treatmentPlan)                     │
│         ↓                                                               │
│  ┌─────────────────────┐                                                 │
│  │ 设置currentAgent    │                                                 │
│  │    = "coding"       │                                                 │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐     YES     ┌─────────────────────────┐        │
│  │ diagnosis == null?  ├────────────→│ addError("No diagnosis")│        │
│  └─────────┬───────────┘             │ return state (提前退出)  │        │
│            │ NO                       └─────────────────────────┘        │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ 构建上下文Map       │  ← diagnosis + (treatmentPlan or empty Map)    │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ JSON序列化context   │                                                 │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ LlmService.generate │  ← 调用LLM生成ICD-10编码JSON                   │
│  │ (SYSTEM_PROMPT,     │                                                 │
│  │  contextJson)       │                                                 │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ cleanJsonResponse   │  ← 去除markdown代码块格式                      │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ JSON反序列化为Map   │                                                 │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  ┌─────────────────────┐                                                 │
│  │ setCodingResult     │  ← 写入编码结果到状态                          │
│  └─────────┬───────────┘                                                 │
│            ↓                                                             │
│  输出: ClinicalState (包含codingResult)                                 │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 4.2 System Prompt 设计分析

```java
private static final String SYSTEM_PROMPT = """
    You are a certified medical coding specialist (CCS). Given diagnosis and treatment
    information, provide ICD-10-CM codes and DRGs grouping as JSON with: primary_icd10
    (code, description, confidence), secondary_icd10_codes (array), drg_group (drg_code,
    description, weight, mean_los), coding_notes, coding_confidence. Return ONLY valid JSON.
    """;
```

**设计要点**：

| 要素 | 说明 |
|------|------|
| **角色定位** | "certified medical coding specialist (CCS)" 引导LLM进入专业编码员角色 |
| **主诊断编码** | `primary_icd10`包含code、description、confidence三个字段 |
| **次要诊断编码** | `secondary_icd10_codes`数组，支持多诊断编码 |
| **DRGs分组** | `drg_group`包含编码、描述、权重、平均住院日 |
| **编码说明** | `coding_notes`记录编码依据和特殊说明 |
| **置信度评估** | `coding_confidence`整体编码质量评分 |

---

## 五、数据处理与流转机制

### 5.1 数据流向表

| 数据项 | 来源 | 去向 | 说明 |
|--------|------|------|------|
| `diagnosis` | DiagnosisAgent | LLM调用 | 诊断结果，作为ICD-10编码的主要依据 |
| `treatmentPlan` | TreatmentAgent | LLM调用 | 治疗方案，辅助编码和DRGs分组（可选） |
| `codingResult` | LLM响应 | AuditAgent | 完整编码结果（ICD-10 + DRGs） |
| `errors` | 异常捕获 | AuditAgent | 错误累积，用于合规审计 |

### 5.2 输出数据结构（LLM输出JSON）

```json
{
  "primary_icd10": {
    "code": "J06.9",
    "description": "急性上呼吸道感染，部位未特指",
    "confidence": 0.95
  },
  "secondary_icd10_codes": [
    {
      "code": "R05",
      "description": "咳嗽"
    },
    {
      "code": "R50.9",
      "description": "发热，原因不明"
    }
  ],
  "drg_group": {
    "drg_code": "DRG-123",
    "description": "上呼吸道感染，非手术治疗",
    "weight": 1.2,
    "mean_los": 3
  },
  "coding_notes": "基于诊断结果和抗生素治疗方案编码，无合并症/并发症",
  "coding_confidence": 0.92
}
```

### 5.3 上下文构建逻辑

```java
Map<String, Object> context = Map.of(
        "diagnosis", state.getDiagnosis(),
        "treatment_plan", state.getTreatmentPlan() != null ? state.getTreatmentPlan() : Map.of()
);
```

**设计特点**：
- `treatment_plan`字段使用空Map作为默认值，确保即使TreatmentAgent未生成结果也能正常执行
- 保持上下文结构的一致性，避免LLM因字段缺失产生异常

---

## 六、异常处理策略

```java
try {
    // LLM调用和JSON解析逻辑
} catch (Exception e) {
    log.error("CodingAgent error: {}", e.getMessage());
    state.getErrors().add("Coding error: " + e.getMessage());
}
```

**处理策略特点**：

| 特性 | 实现方式 | 设计意图 |
|------|----------|----------|
| **异常捕获范围** | 捕获所有Exception | 防止任何异常中断Pipeline |
| **错误记录** | 累积到`state.errors` | 便于后续审计和问题排查 |
| **日志记录** | `log.error()`记录详情 | 支持问题追踪和监控告警 |
| **优雅降级** | 不设置特殊标志，允许Pipeline继续 | 即使无编码结果也可进行审计 |

---

## 七、与其他组件的交互方式

### 7.1 组件依赖关系

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│ TreatmentAgent  │─────→│  CodingAgent    │─────→│   AuditAgent    │
│  (治疗方案输入)  │      │  (ICD编码映射)   │      │  (合规审计)     │
└─────────────────┘      └────────┬────────┘      └─────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
            ┌─────────────┐           ┌─────────────┐
            │   LlmService│           │ ClinicalState│
            │ (LLM调用抽象)│           │  (共享状态)  │
            └─────────────┘           └─────────────┘
```

### 7.2 交互协议

**输入协议**（从ClinicalState读取）：
- 必须包含`diagnosis`字段（Map类型）
- `treatmentPlan`字段可选（用于辅助编码和DRGs分组）

**输出协议**（写入ClinicalState）：
- `codingResult`: Map类型，包含完整编码结果
  - `primary_icd10`: 主诊断ICD-10编码
  - `secondary_icd10_codes`: 次要诊断编码数组
  - `drg_group`: DRGs分组信息
  - `coding_notes`: 编码说明
  - `coding_confidence`: 编码置信度

---

## 八、关键技术点与独特功能

### 8.1 ICD-10-CM编码体系

**核心设计**：通过System Prompt强制LLM输出标准化的ICD-10-CM编码结构：

```java
// Prompt中的关键指令
"primary_icd10 (code, description, confidence)"
"secondary_icd10_codes (array)"
```

**ICD-10编码结构**：
- `code`: 标准ICD-10-CM编码（如J06.9）
- `description`: 编码对应的中文/英文描述
- `confidence`: 编码置信度（0-1之间）

### 8.2 DRGs分组机制

**DRGs（Diagnosis Related Groups）**：疾病诊断相关分组，是医保支付的重要依据

**DRG输出结构**：
- `drg_code`: DRG分组编码
- `description`: 分组描述
- `weight`: 权重（用于费用计算）
- `mean_los`: 平均住院日（Mean Length of Stay）

### 8.3 置信度双重评估

| 置信度层级 | 字段 | 说明 |
|-----------|------|------|
| **编码级** | `primary_icd10.confidence` | 主诊断编码的单独置信度 |
| **整体级** | `coding_confidence` | 整个编码结果的综合置信度 |

**设计价值**：
- 支持编码质量分级
- 便于人工复核优先级排序
- 为系统优化提供反馈数据

---

## 九、与Treatment Agent的对比分析

| 维度 | Treatment Agent | Coding Agent |
|------|-----------------|--------------|
| **角色定位** | 临床药剂师 | 认证医学编码专家 |
| **核心输出** | 治疗方案 | ICD-10编码 + DRGs分组 |
| **输入依赖** | diagnosis（必填） | diagnosis（必填）+ treatmentPlan（可选） |
| **安全检查** | DDI和禁忌症检查 | 编码准确性和置信度评估 |
| **输出结构** | 药物详情、交互信息 | 编码、DRGs、置信度 |
| **医保关联** | 无直接关联 | 直接支持医保支付和统计 |

---

## 十、潜在优化空间

| 优化点 | 当前实现 | 优化建议 | 预期收益 |
|--------|----------|----------|----------|
| **编码数据库校验** | 仅依赖LLM知识 | 接入官方ICD-10-CM编码库API | 提升编码准确性和合规性 |
| **DRGs分组引擎** | LLM推断 | 集成专业DRGs分组系统 | 确保分组结果符合医保标准 |
| **编码规则验证** | 无 | 增加编码规则校验（如主次诊断顺序） | 减少编码错误 |
| **历史编码复用** | 无 | 相似病例编码推荐 | 提高编码效率和一致性 |
| **格式验证** | 仅清理markdown | 增加JSON Schema验证 | 确保输出结构完整性 |
| **多版本支持** | 无 | 支持ICD-10-CM不同年份版本 | 适应政策变化 |

### 优化示例代码

```java
// 优化1：ICD-10编码库校验
private boolean validateIcd10Code(String code) {
    // 调用官方ICD-10编码库API验证编码有效性
    return icd10Service.isValidCode(code);
}

// 优化2：编码规则校验
private boolean validateCodingRules(Map<String, Object> codingResult) {
    // 检查主次诊断顺序、编码完整性等规则
    return codingRuleService.validate(codingResult);
}

// 优化3：DRGs专业分组引擎集成
private Map<String, Object> getDrgGrouping(Map<String, Object> diagnosis, 
                                          Map<String, Object> treatment) {
    // 调用专业DRGs分组系统
    return drgService.group(diagnosis, treatment);
}
```

---

## 十一、总结

**CodingAgent** 作为临床决策Pipeline的编码转换组件，通过以下设计实现了专业的医学编码能力：

1. **角色专业化**：通过System Prompt引导LLM扮演认证医学编码专家
2. **标准化输出**：提供ICD-10-CM编码和DRGs分组，符合医疗管理标准
3. **质量评估**：双重置信度机制（编码级+整体级）支持质量管控
4. **灵活输入**：treatmentPlan可选，即使上游缺失也能正常执行
5. **松耦合设计**：通过ClinicalState与其他Agent解耦

**核心价值**：将临床诊断和治疗信息转化为标准化的医疗编码数据，是连接临床决策与医疗管理（计费、统计、医保支付）的关键桥梁，为医疗信息化提供标准化的数据支撑。

如需继续分析其他Agent（如AuditAgent），请告诉我。
