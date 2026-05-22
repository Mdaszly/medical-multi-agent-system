# 医疗多 Agent 系统

医院业务平台（预约、排班、处方、支付等）+ Java 多 Agent 问诊 + Neo4j 知识图谱。

## 技术栈

Spring Boot · Vue 3 · PostgreSQL · Neo4j · Redis · 阿里云百炼 DashScope

## 快速启动

```powershell
# 1. 基础设施
cd infra
docker compose up -d postgres neo4j redis

# 2. 首次初始化业务库（见手册）
# docker compose exec ... < docs/sql/schema.sql

# 3. 后端
cd ../java
Copy-Item .env.local.example .env.local   # 填入 DASHSCOPE_API_KEY
.\set-env.ps1
mvn spring-boot:run

# 4. 前端
cd ../medical-frontend
npm install && npm run dev
```

## 文档（唯一入口）

**[docs/HANDBOOK.md](docs/HANDBOOK.md)** — 环境、架构、业务、Agent、图谱、测试、部署

| 资源 | 路径 |
|------|------|
| 数据库脚本 | [docs/sql/](docs/sql/) |
| Apifox 接口 | [docs/apifox/](docs/apifox/) |

## 仓库结构

```
java/                 # 后端
medical-frontend/     # 前端
infra/                # Docker Compose（PG + Neo4j + Redis）
docs/HANDBOOK.md      # 项目手册
```

## 安全

API Key 只放在 `java/.env.local`（不提交 Git）。详见手册 [配置与 API 密钥](docs/HANDBOOK.md#6-配置与-api-密钥)。
