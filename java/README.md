# Java 版 — 多Agent临床辅助决策系统

基于 **Spring Boot 3.3 + LangGraph4j + Spring AI 1.0.0-M5** 构建。

## 环境要求

- Java 17+
- Maven 3.9+
- PostgreSQL 16
- Docker (用于运行基础设施)

## 快速开始

### 1. 安装依赖工具

如果尚未安装 Maven 和 Docker，请先安装：

**Maven 安装：**
- 访问 https://maven.apache.org/download.cgi 下载
- 或使用 Chocolatey: `choco install maven`

**Docker Desktop 安装：**
- 访问 https://www.docker.com/products/docker-desktop
- 下载并安装 Windows 版本

### 2. 配置环境变量

使用 PowerShell:
```powershell
cd java
.\set-env.ps1
```

或使用 CMD:
```cmd
cd java
set-env.bat
```

**重要提示：** 请在 `set-env.ps1` 或 `set-env.bat` 中替换 `OPENAI_API_KEY` 为您的真实阿里云 DashScope API 密钥。

### 3. 启动基础设施

```bash
cd ../python
docker-compose up -d postgres neo4j redis
cd ../java
```

### 4. 构建并运行

```bash
mvn clean spring-boot:run
```

或先打包再运行:
```bash
mvn clean package -DskipTests
java -jar target/clinical-decision-system-1.0.0.jar
```

### 5. 验证安装

健康检查:
```bash
curl http://localhost:8080/api/v1/clinical/health
```

运行完整分析:
```bash
curl -X POST http://localhost:8080/api/v1/clinical/analyze \
  -H "Content-Type: application/json" \
  -d "{\"patientDescription\": \"45岁男性，发热39.2℃，咳嗽有黄痰，右侧胸痛。有2型糖尿病和高血压病史。\"}"
```

## 配置说明

### Spring AI 1.0.0-M5 配置

本项目使用 Spring AI 1.0.0-M5，这是与 Spring Boot 3.3 兼容的稳定版本：

1. **依赖配置**: 使用 `spring-ai-openai-spring-boot-starter`
2. **BOM 管理**: 使用 `spring-ai-bom` 进行依赖版本管理
3. **配置属性**: 使用 `.options` 前缀格式

### 阿里云通义千问配置

项目已配置为使用阿里云通义千问（qwen-max）：
- API 端点: `https://dashscope.aliyuncs.com/compatible-mode/v1`
- 模型名称: `qwen-max`
- 温度: `0.2`

如需使用其他模型，请修改 `application.yml` 中的 `spring.ai.openai.chat.options.model` 配置。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/clinical/analyze` | 运行完整5-Agent Pipeline |
| GET  | `/api/v1/clinical/health` | 健康检查 |

## 项目结构

```
java/
├── src/main/java/com/medical/
│   ├── ClinicalDecisionApplication.java  # 主启动类
│   ├── agent/                             # 5个Agent实现
│   │   ├── IntakeAgent.java
│   │   ├── DiagnosisAgent.java
│   │   ├── TreatmentAgent.java
│   │   ├── CodingAgent.java
│   │   └── AuditAgent.java
│   ├── controller/                        # REST 控制器
│   │   └── ClinicalController.java
│   ├── graph/                             # Pipeline 编排
│   │   └── ClinicalPipeline.java
│   └── model/                             # 数据模型
│       └── ClinicalState.java
├── src/main/resources/
│   └── application.yml                    # 配置文件
├── pom.xml                                # Maven 配置
├── set-env.ps1                            # PowerShell 环境变量设置
├── set-env.bat                            # CMD 环境变量设置
└── README.md
```

## 故障排除

### Maven 命令未找到
确保 Maven 已安装并添加到 PATH 环境变量中。

### 端口被占用
修改 `application.yml` 中的 `server.port` 为其他端口。

### 数据库连接失败
确保 Docker 容器正在运行，检查 `POSTGRES_HOST` 和 `POSTGRES_PORT` 配置。

### 依赖下载失败
确保 `pom.xml` 中配置了 Spring Milestones 仓库（已配置）。

### API 调用失败
- 检查 `OPENAI_API_KEY` 是否正确配置
- 确保网络能访问阿里云 API 端点
- 检查模型名称是否正确

## 获取阿里云 DashScope API Key

1. 访问 https://dashscope.aliyuncs.com/
2. 注册/登录阿里云账号
3. 创建 API-KEY
4. 将 API 密钥替换配置文件中的值