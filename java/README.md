# Java 后端

主服务端口 **8080**。完整说明见 **[docs/HANDBOOK.md](../docs/HANDBOOK.md)**。

```powershell
cd infra && docker compose up -d postgres neo4j redis
cd ../java
Copy-Item .env.local.example .env.local   # 配置 DASHSCOPE_API_KEY
.\set-env.ps1
mvn spring-boot:run
```
