@echo off
REM 设置环境变量 - Java版多Agent临床辅助决策系统
echo 正在设置环境变量...

REM OpenAI API配置（兼容阿里云通义千问）
set OPENAI_API_KEY=sk-c4cf1c2cf37849baa21a536bc60ced8e
set OPENAI_MODEL=qwen-max
set OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1

REM PostgreSQL配置
set POSTGRES_HOST=localhost
set POSTGRES_PORT=5432
set POSTGRES_DB=clinical_decision
set POSTGRES_USER=postgres
set POSTGRES_PASSWORD=postgres

REM Neo4j配置
set NEO4J_URI=bolt://localhost:7687
set NEO4J_USER=neo4j
set NEO4J_PASSWORD=neo4j

REM Redis配置
set REDIS_HOST=localhost
set REDIS_PORT=6379

echo 环境变量已设置完成！
echo.
echo OPENAI_API_KEY: %OPENAI_API_KEY:~0,10%...
echo POSTGRES_PASSWORD: %POSTGRES_PASSWORD%
echo.
echo 现在可以运行 mvn spring-boot:run 启动应用
pause
