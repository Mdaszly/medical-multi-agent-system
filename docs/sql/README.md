# SQL 脚本说明

## 文件清单（`docs/sql/`）

| 文件 | 用途 |
|------|------|
| **`schema.sql`** | 空库全量建表（18 张表 + 索引）。部署/新环境**只执行此文件**。 |
| **`generate_slots.sql`** | 运维：按现有排班批量插入 `appointment_slot`（不删表） |
| **`seed_schedule_perf.sql`** | 开发/压测：内科排班演示数据（固定日期，非建表脚本） |

## 数据初始化（非 SQL）

| 数据 | 方式 |
|------|------|
| 症状 / ICD | `POST /api/knowledge-graph/sync-to-rdb`（Neo4j → PostgreSQL） |
| 用户账号 | 注册接口或 Apifox（`schema.sql` 不含用户 INSERT） |
| 药品等 | 应用接口或管理端 |

## 部署顺序（推荐）

```text
1. Docker 启动 PostgreSQL / Neo4j / Redis
2. psql -f docs/sql/schema.sql
3. 启动 Java（Neo4j bootstrap + 可选 sync-to-rdb）
4. 按需：generate_slots.sql（有排班无号源时）
```

## 知识图谱与 RDB

```
Neo4j (Symptom)-[:INDICATES]->(Disease)-[:CLASSIFIED_AS]->(ICD10)
                    │
                    ▼  POST /api/knowledge-graph/sync-to-rdb
PostgreSQL: symptom, icd10_code, symptom_icd_rel
```

## 已移除的历史脚本

以下文件已删除（DDL 均已合并进 `schema.sql`）：`V2_symptom_icd.sql`、`seed_symptom_icd.sql`、`update_*.sql`、`fix_appointment_slots.sql`。

已废弃、勿再创建的表：`audit_log`、`audit_logs`、`clinical_sessions`。
