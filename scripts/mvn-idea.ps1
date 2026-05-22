# 与 IntelliJ IDEA 一致的 Maven / JDK 环境（捆绑 Maven 3.9.9 + 项目 JDK ms-21）
# 用法: .\scripts\mvn-idea.ps1 compile -DskipTests
#       .\scripts\mvn-idea.ps1 -f java\pom.xml test

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$env:MAVEN_HOME = "D:\IDEA\IntelliJ IDEA 2025.1.1.1\plugins\maven\lib\maven3"
$env:JAVA_HOME = "D:\JDK21"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PomFile = Join-Path $ProjectRoot "java\pom.xml"

if ($MavenArgs.Count -eq 0) {
    $MavenArgs = @("-f", $PomFile, "compile", "-DskipTests")
} elseif ($MavenArgs -notcontains "-f") {
    $MavenArgs = @("-f", $PomFile) + $MavenArgs
}

& "$env:MAVEN_HOME\bin\mvn.cmd" @MavenArgs
