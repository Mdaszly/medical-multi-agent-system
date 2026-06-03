# 医疗多 Agent 系统 — 项目手册

> **唯一文档入口**：本地用 VS Code / Cursor 打开本文件，左侧大纲即导航；GitHub 上点击目录锚点可跳转。  
> 数据库脚本：`docs/sql/` · 接口导入：`docs/apifox/预约系统接口文档.json`

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 技术栈与仓库结构](#2-技术栈与仓库结构)
- [3. 环境准备](#3-环境准备)
- [4. 快速启动（本地）](#4-快速启动本地)
- [5. 数据库初始化](#5-数据库初始化)
- [6. 配置与 API 密钥](#6-配置与-api-密钥)
- [7. 系统架构](#7-系统架构)
- [8. 业务模块](#8-业务模块)
  - [8.1 角色与权限](#81-角色与权限)
  - [8.2 用户 / 医生 / 管理员](#82-用户--医生--管理员)
  - [8.3 排班与号源](#83-排班与号源)
  - [8.4 预约挂号](#84-预约挂号)
  - [8.5 到院签到与就诊](#85-到院签到与就诊)
  - [8.6 处方与药房](#86-处方与药房)
  - [8.7 费用与支付](#87-费用与支付)
  - [8.8 健康档案](#88-健康档案)
  - [8.9 预约领域事件（RabbitMQ）](#89-预约领域事件rabbitmq)
- [9. Agent 与在线问诊](#9-agent-与在线问诊)
- [10. 知识图谱与症状解析](#10-知识图谱与症状解析)
- [11. 测试与接口调试](#11-测试与接口调试)
- [12. 前端开发](#12-前端开发)
- [13. 部署与运维](#13-部署与运维)
- [14. 常见问题](#14-常见问题)
- [15. 附录](#15-附录)

---

## 1. 项目概览

企业级**医院业务平台** + **多 Agent 临床辅助 / 在线问诊**：

| 能力 | 说明 |
|------|------|
| 传统业务 | 患者 / 医生 / 管理员 / 药师；排班、预约、签到、处方、发药、支付 |
| AI 能力 | Java 内嵌 Agent Pipeline（接诊→诊断→治疗→编码→审计）；增强问诊 `MedicalPipeline` |
| 知识图谱 | Neo4j 存储症状—疾病—ICD；口语症状经向量 + 同义词 + LLM 对齐后查询 |
| 基础设施 | `infra/docker-compose.yml` 启动 PostgreSQL、Neo4j、Redis、RabbitMQ |

默认端口：**后端 8080** · **前端 dev 5173**（Vite）· **Neo4j 7474/7687**

---

## 2. 技术栈与仓库结构

```
medical-multi-agent-system/
├── java/                 # Spring Boot 3.3 主后端
├── medical-frontend/     # Vue 3 + Vite + Element Plus
├── infra/                # Docker Compose（PG + Neo4j + Redis + RabbitMQ）
├── docker/init-db.sql    # Compose 自动执行（演示审计表，非业务全量）
├── docs/
│   ├── HANDBOOK.md       # ← 本文档
│   ├── sql/              # schema.sql、seed、迁移脚本
│   └── apifox/           # 接口 JSON
└── README.md             # 仓库首页（链到本手册）
```

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot、MyBatis-Plus、Sa-Token、Redis、RabbitMQ（Spring AMQP）、Spring AI（DashScope 兼容） |
| 图数据库 | Neo4j Driver、Cypher |
| 关系库 | PostgreSQL 16 |
| 前端 | Vue 3、TypeScript、Pinia、Vue Router |
| LLM | 阿里云百炼 DashScope（`qwen-max` 等） |

---

## 3. 环境准备

| 工具 | 版本建议 |
|------|----------|
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 18+（前端） |
| Docker Desktop | 用于 PG / Neo4j / Redis / RabbitMQ |
| Git | 任意 |

国内 Docker 镜像拉取失败时，见 [15.2 Docker 镜像加速](#152-docker-镜像加速)。

---

## 4. 快速启动（本地）

### 4.1 基础设施

```powershell
cd infra
docker compose up -d postgres neo4j redis rabbitmq
docker compose ps
```

RabbitMQ 管理台（可选）：`http://localhost:15672`（默认账号 `medical` / `medical`，与 compose 一致）。

等待 Postgres 就绪后，**首次**初始化业务表（见 [第 5 节](#5-数据库初始化)）。

### 4.2 后端

```powershell
cd java
Copy-Item .env.local.example .env.local
# 编辑 .env.local，填入 DASHSCOPE_API_KEY

.\set-env.ps1
mvn spring-boot:run
```

验证：`http://localhost:8080/actuator/health` 或登录接口。

**IDEA 运行 `ClinicalDecisionApplication`**：需配置环境变量 `DASHSCOPE_API_KEY`，或使用 EnvFile 插件加载 `java/.env.local`。

### 4.3 前端

```powershell
cd medical-frontend
npm install
npm run dev
```

浏览器访问控制台输出的地址（通常 `http://localhost:5173`），API 代理到 `8080`。

### 4.4 推荐启动顺序

```
Docker(PG+Neo4j+Redis+RabbitMQ) → 执行 schema.sql（仅首次）→ set-env.ps1 → Java → 前端
```

---

## 5. 数据库初始化

| 步骤 | 说明 |
|------|------|
| 库名 | `clinical_decision`（与 `application.yml` 一致） |
| Compose 自动脚本 | `docker/init-db.sql`：仅占位说明（业务 DDL 见 `schema.sql`） |
| **业务表（必做）** | 手动执行 `docs/sql/schema.sql`（20 张表，含症状三表、`user_notification`、`appointment_event_audit`） |
| 症状/ICD **数据** | 启动 Java 后 `POST /api/knowledge-graph/sync-to-rdb`（Neo4j → PG） |
| 运维/压测 SQL | 按需 `generate_slots.sql`、`seed_schedule_perf.sql`（见 `docs/sql/README.md`） |

**psql 示例（在 postgres 容器内）：**

```powershell
cd infra
docker compose exec -T postgres psql -U postgres -d clinical_decision < ../docs/sql/schema.sql
```

建表后需存在演示账号（与 [11.1](#111-环境与认证) 一致）；`schema.sql` **不含**用户 INSERT，请用前端注册或 Apifox 创建 `admin_test`、`patient001` 等账号。

---

## 6. 配置与 API 密钥

### 6.1 密钥存放位置（仅本地）

| 文件 | 是否提交 Git |
|------|----------------|
| `java/.env.local` | **否**（已 gitignore） |
| `java/.env.local.example` | 是（模板，无真实 Key） |

在 `.env.local` 中设置：

```env
DASHSCOPE_API_KEY=你的百炼API_KEY
```

其余变量见 `java/.env.local.example`（Postgres、Neo4j、Redis、RabbitMQ 默认值与 Docker 一致即可）。

### 6.2 环境变量一览

| 变量 | 用途 | 默认 |
|------|------|------|
| `DASHSCOPE_API_KEY` | 百炼 LLM / Embedding | 必填 |
| `DASHSCOPE_MODEL` | 对话模型 | qwen-max |
| `POSTGRES_*` | 业务库 | host localhost, db clinical_decision |
| `NEO4J_URI` | 图谱 | bolt://localhost:7687 |
| `NEO4J_PASSWORD` | 图谱密码 | neo4jpass（与 compose 一致） |
| `REDIS_HOST` | Sa-Token / 缓存 / 分布式锁 / MQ 消费幂等 | localhost:6379 |
| `RABBITMQ_HOST` | 预约领域事件 Broker | localhost |
| `RABBITMQ_PORT` | AMQP 端口 | 5672 |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ 认证 | medical / medical |
| `MEDICAL_MESSAGING_ENABLED` | 是否启用 RabbitMQ 发布与消费 | true |

本地不启 RabbitMQ 时，可设 `MEDICAL_MESSAGING_ENABLED=false`（业务仍走同步路径；站内通知与异步号源回补不会触发）。

### 6.3 安全提醒

- 不要将 `.env.local`、`set-env` 中写死的 Key 提交 GitHub。
- 若 Key 曾误提交，请在百炼控制台**轮换作废**旧 Key。

---

## 7. 系统架构

```
┌─────────────┐     HTTP      ┌──────────────────────────────┐
│ medical-    │ ────────────► │ Spring Boot :8080             │
│ frontend    │               │  Controller → Service → Mapper  │
└─────────────┘               │  Agent Pipeline / KG Facade   │
                              └───────┬──────────┬───────────┬──────────┐
                                      │          │           │          │
                    ┌─────────────────┼──────────┼───────────┼──────────┼─────────────┐
                    ▼                 ▼          ▼           ▼          ▼             ▼
              PostgreSQL          Neo4j       Redis     RabbitMQ   DashScope API
              (业务数据)          (图谱)   (会话/锁/幂等) (预约领域事件)  (LLM/Embedding)
```

**分层职责**

- **接入层**：REST + Sa-Token 鉴权  
- **业务层**：预约、排班、处方、支付等  
- **AI 层**：`ClinicalPipeline` / `MedicalPipeline`、症状解析、图谱增强  
- **数据层**：PostgreSQL 事务数据；Neo4j 关系推理；Redis 锁与缓存  
- **消息层**：预约领域事件经 RabbitMQ 异步通知、审计落库、超时号源回补（见 [8.9](#89-预约领域事件rabbitmq)）

---

## 8. 业务模块

### 8.1 角色与权限

| 角色 | 代码 | 典型能力 |
|------|------|----------|
| 患者 | `user` | 预约、签到、问诊、缴费、查看处方 |
| 医生 | `doctor` | 排班关联、接诊、开处方、诊间预约管理 |
| 管理员 | `admin` | 用户/医生/排班/预约/处方管理 |
| 药师 | `pharmacist` | 审方、发药 |

认证：**Sa-Token**，请求头 `satoken: <token>`（登录后返回）。

### 8.2 用户 / 医生 / 管理员

- 注册 / 登录：`/api/auth/*`  
- 用户、医生 CRUD：管理端页面 + 对应 Controller  
- 核心表：`user`、`doctor`（见 `docs/sql/schema.sql`）

### 8.3 排班与号源

- **排班** `schedule`：医生、日期、时段、限额  
- **号源** `appointment_slot`：细分时段库存；`available_slots` + 乐观锁版本号防超卖  
- 管理端：排班管理、号源生成（可参考 `docs/sql/generate_slots.sql`）

### 8.4 预约挂号

**流程概要**：选科室/医生 → 选排班时段 → 扣减号源 → 生成 `appointment`（状态机）。

**并发控制**

- Redis 分布式锁（同一号源串行）  
- 数据库乐观锁更新 `appointment_slot`  

**核心表**：`appointment`、`appointment_slot`、`schedule`

**状态**：待就诊 / 已签到 / 已完成 / 已取消等（以代码枚举为准）

**领域事件（RabbitMQ）**：创建、取消、签到、超时、提醒、结算等状态变更在事务提交后发布异步事件，用于站内通知、审计与超时号源回补。详见 [8.9 预约领域事件（RabbitMQ）](#89-预约领域事件rabbitmq)。

### 8.5 到院签到与就诊

- 签到：更新 `check_in_status` 等字段（以当前 schema 为准）  
- 医生诊间：查询当日预约、进入问诊（对接 Agent / 处方）

### 8.6 处方与药房

- `prescription`、`prescription_item`：诊断、药品、剂量、计费  
- 药师端：待审方、发药；单品价 × 数量计费  

### 8.7 费用与支付

- 预约费、处方费等汇总支付（延迟统一结算模式可按业务扩展）  
- 支付表结构见 `docs/sql` 中 `payment` 相关脚本  

### 8.8 健康档案

患者端维护长期健康信息，供诊间与线上问诊引用。

| 项 | 说明 |
|----|------|
| 数据表 | `health_profile`（见 `docs/sql/schema.sql`） |
| 接口 | `GET/POST /api/health-profile/get|create|update` |
| 患者页面 | 前端 `/patient/health-profile`（侧栏「健康档案」） |
| 权限 | 患者仅读写本人；医生诊间可读关联患者；管理员可管理 |

**主要字段**：慢性病史、过敏史、用药史、家族/手术/疫苗/体检记录、身高体重、血型、血压、备注。

**联动**：

- 医生预约详情页展示患者档案摘要  
- 线上问诊 `PatientContextForm` 在无本地缓存时自动从档案预填病史/过敏/用药  

### 8.9 预约领域事件（RabbitMQ）

预约业务在**数据库事务提交后**通过 RabbitMQ 解耦三类副作用：用户站内通知、事件审计、超时号源回补。实现包：`com.medical.messaging`（入口见 `package-info.java`）。

**事件类型**（`AppointmentEventType`）

| 类型 | 典型触发 |
|------|----------|
| `CREATED` | 预约创建成功 |
| `CANCELLED` | 用户/管理员取消 |
| `CHECKED_IN` | 到院签到 |
| `EXPIRED` | 定时任务将待就诊置为超时 |
| `REMINDER` | 就诊前提醒 |
| `SETTLED` / `BILL_UNPAID` | 账单结算联动 |

**发布链路**

```
AppointmentServiceImpl 等业务
  → AppointmentEventBridge（构造 Envelope）
  → Spring AppointmentDomainEvent
  → AppointmentEventAfterCommitListener（AFTER_COMMIT）
  → RabbitAppointmentEventPublisher → Topic Exchange
```

**消费队列**（`RabbitMqTopology`）

| 队列 | 消费者 | 职责 |
|------|--------|------|
| `appointment.notification` | `AppointmentNotificationConsumer` | 写入 `user_notification`（患者/医生通知） |
| `appointment.slot-restore` | `AppointmentSlotRestoreConsumer` | 仅 `EXPIRED`：回补 `appointment_slot.available_slots` |
| `appointment.audit` | `AppointmentAuditConsumer` | 写入 `appointment_event_audit` |

各业务队列绑定死信交换机，失败重试耗尽后进入 `.dlq` 队列（需人工排查）。

**幂等与并发**

- 消费端：`IdempotentConsumerExecutor` + Redis `SET NX`；业务失败 `release()` 键以便重试；Redis 不可用则 `nack` 重入队  
- 超时：`updateStatusIf(PENDING→EXPIRED)` 仅首实例发布事件；`eventId` 稳定为 `APPOINTMENT_EXPIRED:{id}`  
- 签到：`checkInIfPending` 条件更新，避免与超时任务竞态  
- 取消：同步 `increaseAvailableSlots`；超时：异步 slot-restore（双路径，取消不重复走 MQ）

**定时任务**

| 类 | 作用 |
|----|------|
| `AppointmentExpireScheduler` | 扫描待就诊超时 → 条件更新 → 发布 `EXPIRED` |
| `AppointmentReminderScheduler` | 扫描临近就诊 → 发布 `REMINDER`（去重在 Consumer 成功后写 Redis） |

**配置**

`application.yml` → `medical.messaging.enabled`（环境变量 `MEDICAL_MESSAGING_ENABLED`，默认 `true`）。关闭后使用 `NoOpAppointmentEventPublisher`，不注册 Listener/Consumer。

**相关表与接口**

| 项 | 说明 |
|----|------|
| `user_notification` | 站内通知 |
| `appointment_event_audit` | 事件审计 |
| `GET /api/notification/list` | 当前用户通知列表 |
| `PUT /api/notification/{id}/read` | 标记已读 |

已有库升级：执行 `docs/sql/patches/20260603_messaging_tables.sql`。

**深入阅读**（本地 `interview/`，通常 gitignore）：[09-RabbitMQ-预约领域事件.md](../interview/09-RabbitMQ-预约领域事件.md)

---

## 9. Agent 与在线问诊

### 9.1 模式切换

`application.yml` → `medical.ai.chat-type`：

| 值 | 说明 |
|----|------|
| `legacy` | 经典 5-Agent 临床 Pipeline（偏结构化分析） |
| `enhanced` | 线上问诊 **MedicalPipeline**（默认） |

### 9.2 Pipeline 节点（legacy 参考）

```
Intake → Diagnosis ⇄ Intake(信息不足时回退) → Treatment → Coding → Audit
```

- **Intake**：非结构化病情 → 结构化患者信息  
- **Diagnosis**：鉴别诊断 + `needs_more_info` 条件路由  
- **Treatment**：治疗方案 + 药物相互作用检查  
- **Coding**：ICD-10 提示与 DRG 参考  
- **Audit**：规则引擎合规扫描（非 LLM）  

实现包路径：`com.medical.agent.*`、`com.medical.graph.ClinicalPipeline` / `MedicalPipeline`。

### 9.3 与知识图谱的关系

问诊前/中通过 **症状解析** 将口语映射到标准症状节点，再由 Java **`KnowledgeGraphFacade`** 查询 Neo4j（Cypher 多跳），为诊断 Agent 提供证据对象 **`GraphEvidence`**（非 Python GraphRAG 服务）。

### 9.4 会话记忆（Redis List）

增强问诊下：**MySQL** 持久化会话消息；**Redis List**（`medical:chat:memory:{sessionId}`）供 Spring AI `ChatMemory` 缓存最近轮次；Pipeline 侧 `chatHistory` 仍从 DB 加载。选型说明见 [interview/06-数据结构选型-问诊会话记忆.md](../interview/06-数据结构选型-问诊会话记忆.md)（本地 `interview/`，已 gitignore）。

---

## 10. 知识图谱与症状解析

### 10.1 图谱模型（Neo4j）

| 节点 | 关系示例 |
|------|----------|
| `Symptom` | `(Symptom)-[:INDICATES]->(Disease)` |
| `Disease` | `(Disease)-[:CLASSIFIED_AS]->(ICD10)` |

| 组件 | 包路径 |
|------|--------|
| 图谱 CRUD / Cypher | `com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService` |
| 问诊前图谱增强 | `com.medical.service.kg.KnowledgeGraphFacade` |
| 症状解析链 | `com.medical.service.kg.symptom.*` |

启动时可 `medical.ai.graph.bootstrap-on-startup: true` 导入样本数据。

### 10.2 症状语义解析（口语 → 标准词）

```
用户原文
  → SymptomPhraseExtractor
  → 同义词表（毫秒级）
  → 向量 Top-K（DashScope embedding + 内存余弦）
  → LLM 封闭词表消歧（可选）
  → Neo4j 精确查询
```

**关键配置** `medical.ai.graph.symptom-resolver`（见 `application.yml`）：

| 配置项 | 默认 | 含义 |
|--------|------|------|
| vector-top-k | 5 | 召回数 |
| vector-min-score | 0.72 | 高置信阈值 |
| accept-min-confidence | 0.55 | 写入图谱最低置信 |
| llm-disambiguate | true | 模糊时 LLM 消歧 |

**API**

- `POST /api/v1/kg/symptom/resolve` — body: `{"text":"我最近经常头疼"}`  
- `POST /api/v1/kg/symptom/index/rebuild` — 重建向量索引（需 API Key）  

**评测 API**（包 `com.medical.service.kg.symptom.benchmark`，Controller `SymptomRecallBenchmarkController`）

- `POST /api/v1/kg/symptom/eval/vector-topk` — 向量 Top-K 召回评测  
- `POST /api/v1/kg/symptom/eval/synonym-only` — 同义词 baseline  
- `POST /api/v1/kg/symptom/eval/compare` — 并排对比 Recall 提升  

**评测资源**：`java/src/test/resources/kg/symptom_resolver_golden.json`、`java/src/main/resources/kg/vector_topk_eval.json`

---

## 11. 测试与接口调试

### 11.1 环境与认证

| 环境 | 地址 |
|------|------|
| 本地 API | http://localhost:8080 |

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | `admin_test` | `AdminTest@123456` | 与 Apifox / 登录页演示一致 |
| 医生 | `doctor001` | `Doctor@123` | 医生在 `doctor` 表登录 |
| 药师 | `pharmacist001` | `Pharmacist@123` | |
| 患者 | `patient001` | `Patient@123` | |

> 上述账号需在库中已存在（注册或导入种子后使用）。前端登录页可一键填入相同账号。

**登录接口** `POST /api/auth/login`：

```json
{
  "userAccount": "patient001",
  "password": "Patient@123"
}
```

响应 `data.token` → 后续请求头：`satoken: <token>`。

### 11.2 Apifox

1. 导入 `docs/apifox/预约系统接口文档.json`  
2. 环境 Base URL：`http://localhost:8080`  
3. 全局变量 `satoken` = 登录后的 token  

### 11.3 核心联调流程

```
管理员登录 → 创建排班 → 生成号源
    → 患者登录 → 创建预约 → 签到
    → 医生接诊 / 问诊 → 开处方 → 药师发药 → 支付（按模块实现）
    → （可选）患者端 GET /api/notification/list 查看预约通知
```

### 11.4 自动化测试

```powershell
cd java
mvn test
```

图谱 / 症状相关：`SymptomResolverSynonymTest`、`SymptomResolverGoldenEvaluatorTest`（后者需 Neo4j + API Key）。

消息相关：`IdempotentMessageHandlerTest`、`AppointmentEventFactoryTest`、`AppointmentExpireSchedulerTest`、`MessagingAutoConfigurationTest`（无需真实 Broker，Mock/切片测试为主）。

---

## 12. 前端开发

| 项 | 说明 |
|----|------|
| 目录 | `medical-frontend/` |
| 安装 | `npm install` |
| 开发 | `npm run dev` |
| 构建 | `npm run build`（产物 `dist/`，不提交 Git） |
| 部署 | 将 `dist` 交给 Nginx 静态托管，反向代理 `/api` → Java |

路由按角色分为患者 / 医生 / 管理员 / 药师布局，详见 `src/router` 与 `src/views`。

---

## 13. 部署与运维

### 13.1 推荐拓扑（云服务器 / 内网穿透）

```
用户 → Nginx (443)
         ├─ /        → 前端静态 (dist)
         └─ /api     → Java :8080

同机 Docker：postgres + neo4j + redis + rabbitmq（勿将 5432/7687/5672 暴露公网）
```

### 13.2 服务器步骤摘要

1. 安装 Docker、JDK 17、Nginx  
2. `cd infra && docker compose up -d postgres neo4j redis rabbitmq`  
3. 执行 `docs/sql/schema.sql`  
4. 配置 `java/.env.local`（或 systemd `EnvironmentFile`）  
5. `mvn -f java/pom.xml package -DskipTests && java -jar java/target/*.jar`  
6. `npm run build` 前端并配置 Nginx  

### 13.3 内网穿透

只穿透 **Nginx 的 HTTP/HTTPS 端口**；数据库端口仅内网访问。

### 13.4 运维命令

```powershell
cd infra
docker compose ps
docker compose logs -f postgres
docker compose logs -f rabbitmq
docker compose restart neo4j
```

数据卷目录（本地）：`../../persistence-data/`（已在 gitignore）。

---

## 14. 常见问题

| 现象 | 处理 |
|------|------|
| 端口 5432 被占用 | 停掉本机其他 Postgres 或改 compose 端口映射 |
| Java 连不上库 | `docker compose ps`；确认 `POSTGRES_*` 与 compose 一致 |
| Neo4j 认证失败 | 密码用 `neo4jpass`，与 `NEO4J_AUTH` 一致 |
| LLM 报错 | 检查 `DASHSCOPE_API_KEY`；IDEA 运行需单独配环境变量 |
| 缺业务表 | 执行 `docs/sql/schema.sql` |
| 向量索引未就绪 | 配置 Key 后调用 `POST /api/v1/kg/symptom/index/rebuild` |
| Docker 拉镜像慢 | 见附录镜像加速；或改用官方镜像名 |
| Java 启动报 RabbitMQ 连接失败 | `docker compose ps` 确认 `rabbitmq` 健康；或设 `MEDICAL_MESSAGING_ENABLED=false` 临时关闭 |
| 预约成功但无站内通知 | 检查 `MEDICAL_MESSAGING_ENABLED`、Rabbit 队列是否有堆积、Consumer 日志与 `user_notification` 表 |
| 升级库缺通知/审计表 | 执行 `docs/sql/patches/20260603_messaging_tables.sql` |

---

## 15. 附录

### 15.1 临时接口（开发用）

文档记录于历史 `TEMP-临时接口记录`：如 `POST /api/temp/admin/register` 仅用于开发快速建管理员，**上线前必须删除**对应 Controller。

### 15.2 Docker 镜像加速

国内可在 `infra/docker-compose.yml` 中为镜像添加前缀，例如：

- `docker.1ms.run/library/postgres:16-alpine`  
- `docker.1ms.run/library/redis:7-alpine`  

更多说明见仓库根目录 `docker-mirrors-config.md`（可选阅读，后续可合并进本节）。

### 15.3 SQL 与迁移脚本索引

| 文件 | 用途 |
|------|------|
| `docs/sql/README.md` | 脚本索引与部署顺序 |
| `docs/sql/schema.sql` | **权威**全量建表（20 表） |
| `docs/sql/patches/20260603_messaging_tables.sql` | 已有库追加通知/审计表 |
| `docs/sql/generate_slots.sql` | 号源批量生成（运维） |
| `docs/sql/seed_schedule_perf.sql` | 排班压测种子（开发） |

### 15.4 关键代码索引

| 模块 | 路径 |
|------|------|
| 启动类 | `java/.../ClinicalDecisionApplication.java` |
| 预约服务 | `java/.../service/impl/AppointmentServiceImpl.java` |
| 预约领域事件 / MQ | `java/.../messaging/` |
| 用户通知 | `java/.../controller/NotificationController.java` |
| Agent | `java/.../agent/`、`java/.../graph/` |
| 症状解析 | `java/.../service/kg/symptom/` |
| 症状召回评测 | `java/.../service/kg/symptom/benchmark/` |
| 图谱 | `java/.../knowledgegraph/` |
| 前端路由 | `medical-frontend/src/router/` |

### 15.5 面试 / 答辩材料（本地）

口述稿与简历索引在仓库根目录 **`interview/`**（通常已 `.gitignore`，不提交 Git）。公开可链文档：

| 文档 | 说明 |
|------|------|
| [../interview/README.md](../interview/README.md) | 本地面试索引（含 01–09 篇目） |
| [../interview/09-RabbitMQ-预约领域事件.md](../interview/09-RabbitMQ-预约领域事件.md) | 预约 MQ 架构、面试问答与 Code Review 对照（本地） |
| [医院排班查询接口性能优化项目.md](医院排班查询接口性能优化项目.md) | 排班接口 Redis 缓存与压测（可公开） |

---

*文档版本：2026-06，与当前仓库结构（Java + 前端 + Docker 基础设施 + RabbitMQ 预约事件）对齐。*
