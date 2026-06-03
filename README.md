# Multi-Agent 智慧门诊平台

对标真实门诊流程的全栈医院业务平台：**预约 → 签到 → 就诊 → 处方 → 审方 → 支付**，覆盖患者 / 医生 / 药师 / 管理员四类角色；在完整业务链路上集成 **Multi-Agent 在线问诊**、**Neo4j 知识图谱增强**、**Redis 并发 / 缓存**与 **RabbitMQ 预约领域事件**异步解耦能力。

> 仓库名 `medical-multi-agent-system` · 独立开发项目 · Spring Boot 3 + Vue 3

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **门诊业务** | 20 张业务表；排班、号源、预约状态机、到院签到、处方与审方、费用支付 |
| **预约抢号** | Redisson 按排班分布式锁 + `appointment_slot` 乐观锁，多实例防号源超卖 |
| **预约领域事件** | RabbitMQ：创建/取消/签到/过期/提醒/结算等 7 类事件；事务提交后发布；通知、审计、超时号源回补三队列 + DLQ |
| **站内通知** | 患者/医生端通知铃铛；`user_notification` + `GET /api/notification/list` |
| **患者端问诊** | `MedicalPipeline`：Router 分诊 6 类专精 Agent；Neo4j Graph RAG；SSE 流式 + `agentTrace` |
| **医生端临床** | `ClinicalPipeline`：Intake → Diagnosis → Treatment → Coding → Audit；DDI 检查 + 规则合规审计 |
| **知识图谱** | Symptom → Disease → ICD10 多跳 Cypher；口语症状经同义词 / 向量 / LLM 对齐；gold 集 **benchmark** 对比 Recall@K |
| **排班查询优化** | Redis 按科室+日期缓存；JMeter 50 并发压测：平均响应 **-21%**、P99 **-45%** |

默认端口：**后端 8080** · **前端 5173** · **Neo4j 7474 / 7687** · **RabbitMQ 5672**（管理台 **15672**）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 · Spring Boot 3 · MyBatis-Plus · Sa-Token · Spring AI |
| 数据 | PostgreSQL 16（业务）· Neo4j（知识图谱）· Redis（锁 / 缓存 / 会话 / MQ 幂等） |
| 消息 | RabbitMQ 3 · Spring AMQP（Topic 交换机 · 手动 ack · 死信队列） |
| AI | 阿里云百炼 DashScope（`qwen-max` / `qwen-turbo` / Embedding） |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · Pinia |
| 部署 | Docker Compose（PostgreSQL + Neo4j + Redis + RabbitMQ） |

---

## 系统架构

```
┌─────────────────┐     HTTP/SSE    ┌──────────────────────────────────────────┐
│ medical-frontend│ ──────────────► │ Spring Boot :8080                         │
│  患者 / 医生 /   │                 │  REST + Sa-Token · 业务 · Agent Pipeline │
│  药师 / 管理员   │                 │  预约事件 Bridge → AFTER_COMMIT → MQ     │
└─────────────────┘                 └───────┬──────────┬──────────┬────────────┘
                                            │          │          │
              ┌─────────────────────────────┼──────────┼──────────┼────────────┐
              ▼                             ▼          ▼          ▼            ▼
        PostgreSQL                      Neo4j      Redis    RabbitMQ    DashScope
      预约·处方·通知·审计                症状-疾病-ICD  锁·缓存·幂等  领域事件      LLM·Embedding
```

**预约异步副作用（RabbitMQ）**

```
AppointmentService / Scheduler
  → AppointmentEventBridge
  → @TransactionalEventListener(AFTER_COMMIT)
  → Topic Exchange
       ├─ appointment.notification  → 站内通知
       ├─ appointment.slot-restore  → 超时回补号源（仅 EXPIRED）
       └─ appointment.audit         → appointment_event_audit
```

实现包：`com.medical.messaging`（详见 [HANDBOOK §8.9](docs/HANDBOOK.md#89-预约领域事件rabbitmq)）

**Agent 编排（enhanced 模式，默认）**

```
患者输入 → MedicalRouterAgent（意图分类）
         → 专精 Agent（初诊/复诊/用药/报告/健康/挂号）
         → KnowledgeEnrichmentService（Neo4j Graph RAG）
         → 结构化 JSON + SSE 流式输出 + ICD Grounding 校验

医生诊间 → ClinicalPipeline 五 Agent 串联 → ClinicalState + DDI + 规则 Audit
```

实现入口：`com.medical.graph.MedicalPipeline` · `com.medical.graph.ClinicalPipeline` · `com.medical.service.kg.*` · `com.medical.service.kg.symptom.benchmark.*`

---

## 仓库结构

```
medical-multi-agent-system/
├── java/                    # Spring Boot 后端（Agent、业务、图谱、messaging）
├── medical-frontend/        # Vue 3 前端（四角色 + 通知中心）
├── infra/                   # docker-compose.yml（PostgreSQL + Neo4j + Redis + RabbitMQ）
├── docs/
│   ├── HANDBOOK.md          # 详细手册（架构、业务、MQ、Agent、部署）
│   ├── sql/                 # schema.sql、patch、压测脚本
│   └── apifox/              # 接口文档 JSON
├── docker/init-db.sql       # Compose 占位说明
└── README.md                # 本文件
```

---

## 快速开始

### 环境要求

JDK 17+ · Maven 3.9+ · Node.js 18+ · Docker Desktop

### 1. 启动基础设施

```powershell
cd infra
docker compose up -d postgres neo4j redis rabbitmq
```

RabbitMQ 管理台（可选）：`http://localhost:15672`（默认 `medical` / `medical`）

### 2. 初始化业务库（首次）

```powershell
docker compose exec -T postgres psql -U postgres -d clinical_decision < ../docs/sql/schema.sql
```

建表后通过前端注册或 Apifox 创建测试账号。已有库可执行 `docs/sql/patches/20260603_messaging_tables.sql`。图谱样本可在后端启动后导入（见 [手册 §5、§10](docs/HANDBOOK.md)）。

### 3. 启动后端

```powershell
cd ../java
Copy-Item .env.local.example .env.local   # 填入 DASHSCOPE_API_KEY
.\set-env.ps1
mvn spring-boot:run
```

验证：`http://localhost:8080/actuator/health`

本地暂不启 RabbitMQ 时可设环境变量 `MEDICAL_MESSAGING_ENABLED=false`。

### 4. 启动前端

```powershell
cd ../medical-frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，API 代理至 `8080`。

**推荐顺序**：Docker（含 RabbitMQ）→ `schema.sql`（仅首次）→ 配置 Key → Java → 前端

---

## 核心链路说明

### 门诊主流程

```
选科室/医生 → 抢号/预约（Redisson + 乐观锁扣减号源）
    → 到院签到 → 医生接诊 → 开具处方 → 药师审方 → 在线支付
    → （异步）预约事件 → 站内通知 / 审计 / 超时号源回补
```

### 在线问诊（患者端）

- 接口：`POST /api/v1/consult/stream`（SSE）或同步 consult
- 健康档案由前端 `PatientContextForm` 预填 `patientContext` 注入 Prompt
- 响应含风险等级、推荐科室、危急信号（`red_flags`）及 `agentTrace` 轨迹

### 配置开关

`java/src/main/resources/application.yml`：

| 配置 | 说明 |
|------|------|
| `medical.ai.chat-type: enhanced` | 启用 MedicalPipeline 线上问诊（默认） |
| `medical.ai.graph.enabled: true` | Neo4j 图谱预增强与 ICD 校验 |
| `medical.messaging.enabled: true` | 启用 RabbitMQ 发布与消费（环境变量 `MEDICAL_MESSAGING_ENABLED`） |
| `app.llm.provider: dashscope` | 百炼 DashScope 兼容 API |

更多 Agent、图谱、症状解析、MQ 细节见 **[docs/HANDBOOK.md](docs/HANDBOOK.md)**（§8 业务 · §8.9 预约事件 · §9–§10 Agent/图谱）。

---

## 文档与资源

| 文档 | 内容 |
|------|------|
| **[docs/HANDBOOK.md](docs/HANDBOOK.md)** | 环境、架构、全业务模块、RabbitMQ、Agent、图谱、测试、部署、FAQ |
| [docs/sql/](docs/sql/) | 建表脚本、MQ patch、号源生成、压测 seed |
| [docs/apifox/](docs/apifox/) | 接口定义（导入 Apifox 联调） |
| [docs/医院排班查询接口性能优化项目.md](docs/医院排班查询接口性能优化项目.md) | JMeter 压测方案与优化对比 |

---

## 安全说明

- **API Key** 仅放在 `java/.env.local`（已 `.gitignore`），模板见 `java/.env.local.example`
- 勿将百炼 Key、数据库密码提交至公开仓库；若误提交请在控制台轮换密钥

---

## 许可证

见 [LICENSE](LICENSE)。
