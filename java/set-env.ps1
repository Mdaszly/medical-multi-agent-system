# 从 .env.local 加载环境变量（本地密钥，勿提交 Git）
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$envFile = Join-Path $root ".env.local"

function Set-DefaultEnv([string]$Name, [string]$Value) {
    if (-not $env:$Name) { Set-Item -Path "env:$Name" -Value $Value }
}

if (-not (Test-Path $envFile)) {
    Write-Host "未找到 $envFile" -ForegroundColor Red
    Write-Host "请执行: Copy-Item .env.local.example .env.local" -ForegroundColor Yellow
    Write-Host "然后在 .env.local 中填写 DASHSCOPE_API_KEY" -ForegroundColor Yellow
    exit 1
}

Write-Host "正在从 .env.local 加载环境变量..." -ForegroundColor Green
Get-Content $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim().Trim('"').Trim("'")
    Set-Item -Path "env:$name" -Value $value
}

Set-DefaultEnv "DASHSCOPE_MODEL" "qwen-max"
Set-DefaultEnv "DASHSCOPE_BASE_URL" "https://dashscope.aliyuncs.com/compatible-mode/v1"
Set-DefaultEnv "POSTGRES_HOST" "localhost"
Set-DefaultEnv "POSTGRES_PORT" "5432"
Set-DefaultEnv "POSTGRES_DB" "clinical_decision"
Set-DefaultEnv "POSTGRES_USER" "postgres"
Set-DefaultEnv "POSTGRES_PASSWORD" "postgres"
Set-DefaultEnv "NEO4J_URI" "bolt://localhost:7687"
Set-DefaultEnv "NEO4J_USER" "neo4j"
Set-DefaultEnv "NEO4J_PASSWORD" "neo4jpass"
Set-DefaultEnv "REDIS_HOST" "localhost"
Set-DefaultEnv "REDIS_PORT" "6379"

if (-not $env:DASHSCOPE_API_KEY -or $env:DASHSCOPE_API_KEY -like "*your-dashscope*") {
    Write-Host "请在 .env.local 中设置有效的 DASHSCOPE_API_KEY" -ForegroundColor Red
    exit 1
}

Write-Host "环境变量已加载" -ForegroundColor Green
Write-Host "DASHSCOPE_API_KEY: $($env:DASHSCOPE_API_KEY.Substring(0, [Math]::Min(10, $env:DASHSCOPE_API_KEY.Length)))..." -ForegroundColor Yellow
Write-Host "现在可以运行: mvn spring-boot:run" -ForegroundColor Cyan
