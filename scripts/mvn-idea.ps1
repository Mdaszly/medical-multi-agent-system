# 在 PowerShell 中运行 Maven（优先使用本机 PATH，可选本地路径配置）
# 用法:
#   .\scripts\mvn-idea.ps1
#   .\scripts\mvn-idea.ps1 test
#   .\scripts\mvn-idea.ps1 spring-boot:run
#
# 若 IDEA 捆绑 Maven / 自定义 JDK 不在 PATH 中，请复制:
#   scripts\mvn-idea.local.ps1.example -> scripts\mvn-idea.local.ps1
# 并填写本机路径（该文件已 gitignore，不会提交）。

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptDir
$PomFile = Join-Path $ProjectRoot "java\pom.xml"
$LocalConfig = Join-Path $ScriptDir "mvn-idea.local.ps1"

if (Test-Path $LocalConfig) {
    . $LocalConfig
}

function Resolve-MavenCommand {
    if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME "bin\mvn.cmd"))) {
        return (Join-Path $env:MAVEN_HOME "bin\mvn.cmd")
    }
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) {
        return $mvn.Source
    }
    return $null
}

function Ensure-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return
    }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        return
    }
    $javaExe = $java.Source
    $binDir = Split-Path -Parent $javaExe
    $candidateHome = Split-Path -Parent $binDir
    if (Test-Path (Join-Path $candidateHome "bin\java.exe")) {
        $env:JAVA_HOME = $candidateHome
    }
}

Ensure-JavaHome

$mvnCmd = Resolve-MavenCommand
if (-not $mvnCmd) {
    Write-Host "未找到 Maven。请任选其一：" -ForegroundColor Red
    Write-Host "  1. 将 Maven 加入系统 PATH" -ForegroundColor Yellow
    Write-Host "  2. 复制 scripts\mvn-idea.local.ps1.example 为 mvn-idea.local.ps1 并设置 MAVEN_HOME" -ForegroundColor Yellow
    exit 1
}

if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}
if ($env:MAVEN_HOME) {
    $env:PATH = "$env:MAVEN_HOME\bin;$env:PATH"
}

if ($MavenArgs.Count -eq 0) {
    $MavenArgs = @("compile", "-DskipTests")
}
if ($MavenArgs -notcontains "-f") {
    $MavenArgs = @("-f", $PomFile) + $MavenArgs
}

Write-Host "JAVA_HOME=$($env:JAVA_HOME)" -ForegroundColor DarkGray
Write-Host "mvn=$mvnCmd" -ForegroundColor DarkGray
& $mvnCmd @MavenArgs
