# Docker 镜像源配置说明（2026年最新）

## 一、问题分析

### 当前状况
经过测试，大部分公开的Docker镜像源已失效或不稳定。

## 二、解决方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **方案A：临时拉取方式** | 无需配置Docker，即用即走 | 每个镜像都要加前缀 | ⭐⭐⭐⭐⭐ |
| **方案B：全局镜像源配置** | 一次配置，永久生效 | 镜像源失效后需重新配置 | ⭐⭐⭐⭐ |
| **方案C：自建镜像源** | 完全可控，安全可靠 | 部署维护成本高 | ⭐⭐⭐ |

---

## 三、推荐方案：临时拉取方式（已应用）

### ✅ 已修改文件
- `python/docker-compose.yml` - 所有镜像已添加 `docker.1ms.run` 前缀

### 2026年可用的镜像源

| 镜像源 | 状态 | 说明 |
|--------|------|------|
| `docker.1ms.run` | ✅ 可用 | 推荐，速度快 |
| `docker.xuanyuan.me` | ⚠️ 需验证 | 可能需要认证 |
| `docker.1panel.live` | ⚠️ 需验证 | 部分镜像可能403 |
| `docker.m.daocloud.io` | ⚠️ 需验证 | 道云镜像 |

### 使用方法

```bash
# 临时拉取单个镜像
docker pull docker.1ms.run/library/postgres:16-alpine

# 重命名为标准名称（可选）
docker tag docker.1ms.run/library/postgres:16-alpine postgres:16-alpine
```

### docker-compose.yml 修改示例

```yaml
services:
  postgres:
    image: docker.1ms.run/library/postgres:16-alpine  # 添加镜像源前缀
    # ... 其他配置

  neo4j:
    image: docker.1ms.run/library/neo4j:5-community
    # ... 其他配置

  redis:
    image: docker.1ms.run/library/redis:7-alpine
    # ... 其他配置
```

---

## 四、备选方案：全局镜像源配置

### Windows 配置步骤

1. 打开 **Docker Desktop**
2. 点击右上角设置图标 ⚙️
3. 选择 **Docker Engine**
4. 复制以下配置并替换原有内容：

```json
{
    "builder": {
        "gc": {
            "defaultKeepStorage": "20GB",
            "enabled": true
        }
    },
    "experimental": false,
    "features": {
        "buildkit": true
    },
    "registry-mirrors": [
        "https://docker.1ms.run",
        "https://docker.1panel.live",
        "https://docker.xuanyuan.me"
    ]
}
```

5. 点击 **Apply & Restart** 重启 Docker

### 验证配置

```bash
# 查看当前配置的镜像源
docker info | findstr "Registry Mirrors"

# 测试拉取镜像（无需加前缀）
docker pull postgres:16-alpine
```

---

## 五、启动步骤

### 1. 启动服务

```bash
cd d:\IDEA\multi-agent\medical-multi-agent-system\python
docker-compose up -d
```

### 2. 查看启动日志

```bash
# 查看所有容器状态
docker-compose ps

# 查看PostgreSQL日志
docker-compose logs postgres

# 实时查看所有日志
docker-compose logs -f
```

### 3. 验证服务健康状态

```bash
# 检查PostgreSQL健康状态
docker exec -it python-postgres-1 pg_isready -U postgres

# 检查Redis健康状态
docker exec -it python-redis-1 redis-cli ping
```

---

## 六、镜像源失效时的应急方案

如果当前镜像源失效，请按以下顺序尝试：

### 步骤1：更换 docker-compose.yml 中的镜像源

```yaml
# 尝试更换为其他镜像源
image: docker.xuanyuan.me/library/postgres:16-alpine
# 或
image: docker.1panel.live/library/postgres:16-alpine
```

### 步骤2：临时使用其他镜像源

```bash
# 手动拉取并重新标记
docker pull docker.1ms.run/library/postgres:16-alpine
docker tag docker.1ms.run/library/postgres:16-alpine postgres:16-alpine
docker-compose up -d
```

### 步骤3：获取最新镜像源信息

访问以下网站获取最新可用镜像源：
- https://www.dhzyw.com/archives/7680.html

---

## 七、常见问题

### Q1: 拉取镜像时提示 403 Forbidden
**A**: 尝试更换其他镜像源，或使用临时拉取方式。

### Q2: 镜像源突然失效
**A**: 这是正常现象，第三方镜像源可能随时停止服务，请及时更换。

### Q3: 可以使用官方 Docker Hub 吗？
**A**: 如果网络环境允许，可以直接访问官方源，这是最稳定可靠的方式。

### Q4: 如何自建镜像源？
**A**: 参考：https://www.dhzyw.com/archives/6861.html

---

## 八、文件变更记录

| 日期 | 文件 | 修改内容 |
|------|------|----------|
| 2026-05-12 | `python/docker-compose.yml` | 所有镜像添加 `docker.1ms.run` 前缀 |

---

**最后更新**: 2026-05-12
