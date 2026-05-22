 @echo off
REM 从 .env.local 加载（与 set-env.ps1 相同数据源）
if not exist "%~dp0.env.local" (
    echo 未找到 .env.local，请复制 .env.local.example 并填写 DASHSCOPE_API_KEY
    exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~dp0.env.local") do (
    if not "%%a"=="" set "%%a=%%b"
)
if "%DASHSCOPE_API_KEY%"=="" (
    echo 请在 .env.local 中设置 DASHSCOPE_API_KEY
    exit /b 1
)
echo 环境变量已从 .env.local 加载
echo DASHSCOPE_API_KEY: %DASHSCOPE_API_KEY:~0,10%...
echo 现在可以运行 mvn spring-boot:run
