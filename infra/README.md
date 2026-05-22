# 基础设施（Docker Compose）

启动 **PostgreSQL、Neo4j、Redis**。说明见 **[docs/HANDBOOK.md](../docs/HANDBOOK.md)**。

```powershell
cd infra
docker compose up -d postgres neo4j redis
```

业务建表：执行 `../docs/sql/schema.sql`。
