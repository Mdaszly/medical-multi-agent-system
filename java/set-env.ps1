# 设置环境变量 - Java版多Agent临床辅助决策系统 (PowerShell版本)
Write-Host "正在设置环境变量..." -ForegroundColor Green

# DashScope API配置（阿里云百炼 - 通过OpenAI兼容模式）
$env:DASHSCOPE_API_KEY = "sk-c4cf1c2cf37849baa21a536bc60ced8e"
$env:DASHSCOPE_MODEL = "qwen-max"
$env:DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

# PostgreSQL配置
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_PORT = "5432"
$env:POSTGRES_DB = "clinical_decision"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"

# Neo4j配置
$env:NEO4J_URI = "bolt://localhost:7687"
$env:NEO4J_USER = "neo4j"
$env:NEO4J_PASSWORD = "neo4jpass"

# Redis配置
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"

Write-Host "环境变量已设置完成！" -ForegroundColor Green
Write-Host ""
Write-Host "DASHSCOPE_API_KEY: $($env:DASHSCOPE_API_KEY.Substring(0,10))..." -ForegroundColor Yellow
Write-Host "DASHSCOPE_MODEL: $($env:DASHSCOPE_MODEL)" -ForegroundColor Yellow
Write-Host ""
Write-Host "现在可以运行 'mvn spring-boot:run' 启动应用" -ForegroundColor Cyan
