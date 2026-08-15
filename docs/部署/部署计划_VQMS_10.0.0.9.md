# VQMS 部署计划 — 10.0.0.9 (myubuntu)

> 依据：`docker-compose.yml`（仓库根）+ 服务器现状调查（2026-08-03）
> 硬约束：**不影响服务器现有 5 个容器**（mysql57 / new-api / postgres / redis / portainer）
> 状态：**仅计划，未执行**。执行前需逐项确认 §7。

---

## 1. 服务器现状（调查快照）

### 1.1 现有容器（全部 Up，不可干扰）

| 容器 | 镜像 | host 端口 | 用途 |
|---|---|---|---|
| **mysql57** | mysql:5.7 | **0.0.0.0:3306** | ★ **VQMS 的外部数据源**（库 `qheatavchisdb`） |
| new-api | calciumion/new-api | 3000 | API 服务 |
| postgres | postgres:15 | 仅容器内 5432 | new-api 的库 |
| redis | redis:7 | 仅容器内 6379 | new-api 的缓存 |
| portainer | portainer-ce | 8000 / 9443 | 容器管理 UI |

### 1.2 资源

| 项 | 值 | 评估 |
|---|---|---|
| 内存 | 3.6 GiB total，**available 2.7 GiB** | ⚠️ 偏紧，详见 §4 |
| 磁盘 | 115 G，剩 99 G | ✅ 充足 |
| CPU | 4 核 | ✅ 够 |
| Swap | 3.6 GiB（几乎未用） | 可兜底 |

### 1.3 VQMS 需用的 host 端口（均已确认空闲）

| 端口 | 服务 | 占用 | 结论 |
|---|---|---|---|
| 13306 | mysql | 空 | ✅ |
| 16379 | redis | 空 | ✅ |
| 7000 | backend | 空 | ✅ |
| 8080 | nginx | 空 | ✅ |

### 1.4 命名空间冲突（均无）

- **容器名**：现有 mysql57/new-api/postgres/redis/portainer；VQMS 用 `vqms-*` 前缀 → 无冲突
- **网络**：现有 mysql57_default / new-api_new-api-network；VQMS 用 `vqms-net` → 无冲突
- **卷**：现有 mysql57_mysql57-data / new-api_pg_data / portainer_data；VQMS 用 `mysql_data` / `redis_data` / `backend_upload` / `backend_logs` → 无冲突

---

## 2. 关键发现

### 2.1 ★ 外部数据源 = 本机 mysql57 容器

规划文档里的"外部只读源 `10.0.0.9:3306 / qheatavchisdb`"，**正是这台服务器上正在跑的 `mysql57` 容器**。部署到同机后，backend 读外部源是"本机回环"：

- `.env.example` 里 `SOURCE_HOST=10.0.0.9` 仍能通（容器出 vqms-net → host 网络 → 10.0.0.9:3306 → mysql57）
- 更优解：让 backend 跨 docker 网络直连 mysql57 容器（需把 backend 加入 `mysql57_default` 网络，或把 mysql57 挂到 vqms-net）——延迟更低
- **M0 不读外部源**（SOURCE_* 仅占位）；**M1 接入 `ruoyi-vqms/source` 时再定连接方式**。本计划先按 `SOURCE_HOST=10.0.0.9`。

### 2.2 ⚠️ 内存是主要瓶颈

3.6 GiB 总内存，现有容器占 ~630 MiB。VQMS 4 容器预估峰值：

| 容器 | 估算 |
|---|---|
| mysql 8.4 | 400–600 MiB |
| backend（Spring Boot + Druid + Quartz） | 500–900 MiB |
| redis 7 | ~50 MiB |
| nginx | ~20 MiB |
| **合计** | **~1.0–1.6 GiB** |

启动后 available 约 1.1–1.7 GiB；构建期（mvn 打包 + npm build）峰值更高，会和现有容器抢内存。**强烈建议加内存上限**（§3.4），并低峰部署。

---

## 3. 部署方案（待执行）

### 3.1 传输：源码到服务器

`build.context` 需要完整源码树（仅传 Dockerfile 不够）。传输内容：
- `RuoYi-Vue-springboot3/`（含 pom.xml + 各模块 + `sql/`）
- `RuoYi-Vue3/`（含 package.json）
- `docker-compose.yml` + `.env.example`

传输时排除（减体量）：`node_modules/`、`target/`、`.git/`、`*.zip`、`dist/`

方式（三选一，待定）：

| 方式 | 命令要点 | 备注 |
|---|---|---|
| **A. rsync 增量**（推荐） | `rsync -avz --exclude=node_modules --exclude=target ... syth@10.0.0.9:~/vqms/` | 增量、可重传 |
| B. 打包传 | 本机 `tar czf`（排除大目录）→ `scp` → 服务器解压 | 一次性 |
| C. git clone | 服务器 `git clone https://github.com/.../VQMS.git` | 需服务器能访 GitHub；含 .git 历史 |

**目标路径建议**：`/home/syth/vqms/`（家目录下，与现有部署隔离）。

### 3.2 配置：`.env`

```bash
cd ~/vqms
cp .env.example .env
nano .env   # 填强随机密码
```

填写要点：
- `MYSQL_ROOT_PASSWORD` / `DB_PASSWORD` / `REDIS_PASSWORD` / `JWT_SECRET`：各自生成强随机值（≥ 32 位 for JWT）
- `MYSQL_DATABASE=ry_vqms`（保持默认）
- `SOURCE_*`：M0 占位（`SOURCE_HOST=10.0.0.9` 或留空），M1 启用

⚠️ **`.env` 切勿提交 git**。仓库根 `.gitignore` 目前尚未排除 `.env`（坑 1，见 §7.6），部署前最好先加忽略规则。

### 3.3 启动

```bash
cd ~/vqms
docker compose up -d --build
```

compose 自动处理启动顺序：mysql/redis（等 healthy）→ backend → nginx。4 容器起在独立的 `vqms-net`，与现有 `mysql57_default` / `new-api_*` 网络隔离。

### 3.4 内存上限建议（降低对现有容器影响）

当前 `docker-compose.yml` 未设内存限制。执行前建议给 mysql/backend 加（**计划，暂不改 compose，执行时一并加**）：

```yaml
mysql:
  mem_limit: 700m
backend:
  mem_limit: 1g
  environment:
    JAVA_TOOL_OPTIONS: "-Xmx512m"   # JVM 原生识别，无需改 Dockerfile
```

> 当前 backend Dockerfile ENTRYPOINT 是 `["java","-jar","/app/app.jar"]`，不接 `JAVA_OPTS`。用 `JAVA_TOOL_OPTIONS` 环境变量可绕过（JVM 自动读取），无需改 Dockerfile。

### 3.5 端口暴露收窄（可选，最小暴露面）

为减少对 host 的占用面，可去掉 mysql/redis/backend 的 host 端口映射（仅 nginx 8080 对外，内部走 vqms-net）：

```yaml
# mysql/redis/backend 的 ports: 块注释掉，仅留 nginx 8080
```

调试时再临时开。**计划，暂不改。**

---

## 4. 风险与影响评估

| 风险 | 对现有系统影响 | 缓解 |
|---|---|---|
| **内存不足** | 现有容器 OOM（尤其 new-api/postgres） | §3.4 内存上限；低峰部署；`docker stats` 监控 |
| 构建期抢资源 | mvn/npm 构建峰值拖慢 new-api | 低峰执行；或本机构建镜像 `save`→`load`（本机无 docker，需先装） |
| 端口冲突 | 已查无（§1.3） | 启动前再跑 `ss -tln \| grep -E '13306\|16379\|7000\|8080'` 复核 |
| 容器/网络/卷同名 | 已查无（§1.4） | vqms- 前缀 + vqms-net 天然隔离 |
| 误碰外部源 mysql57 | backend 配错连错库 | SOURCE_* 指向 10.0.0.9:3306（mysql57）；VQMS 主库 `mysql_data` 独立卷，不碰 mysql57 的 `mysql57_mysql57-data` 卷 |
| .env 误提交 | 密码泄露 | `.env` 不入 git（先在 `.gitignore` 加规则） |

---

## 5. 验证步骤（部署后）

```bash
cd ~/vqms
docker compose ps                                   # 1. 4 容器全 Up
docker compose logs mysql | grep "ready for connections"  # 2. mysql 起来
docker compose exec mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" -e "use ry_vqms; show tables;"  # 3. 库 + 表齐全（sys_* + voltage_quality_*）
curl -I http://localhost:8080                       # 4. nginx 200
curl -i http://localhost:8080/prod-api/login        # 5. 后端响应（401/400 即通）
docker compose logs backend | tail                 # 6. Spring Boot 启动成功
docker stats --no-stream                           # 7. 各容器内存在预算内
docker ps                                          # 8. ★ 现有 5 容器仍 Up（无干扰）
```

第 8 步是"不影响现有"的最终确认。

---

## 6. 回滚 / 清理

```bash
cd ~/vqms
docker compose down        # 停删 4 容器 + vqms-net（保留卷）
docker compose down -v     # 连 4 个数据卷一起删（彻底清理）
```

`docker compose down` 只动 compose 管理的 vqms-* 资源，**完全不碰 mysql57 / new-api / postgres / redis / portainer**。

---

## 7. 待确认事项（执行前拍板）

1. **传输方式**：rsync / 打包 / git clone？（服务器能否访 GitHub？）→ 决定 §3.1
2. **目标路径**：`/home/syth/vqms/` 是否合适？→ §3.1
3. **内存上限**：是否采纳 §3.4 的 mem_limit？mysql 700m / backend 1g / JVM 512m 是否合理？
4. **外部源连接（M1）**：backend 直连 mysql57 容器，还是继续走 `10.0.0.9:3306`？→ §2.1
5. **构建时机**：低峰执行？
6. **端口暴露**：是否收窄到只露 nginx 8080？→ §3.5
7. **坑 1（`.env` 忽略）**：部署前是否先在仓库 `.gitignore` 加 `.env`？→ §3.2
