---
name: vqms-deploy
description: 把 VQMS（基于 RuoYi-Vue 的电压质量管理系统）部署或更新到 10.0.0.9 (myubuntu) 服务器。当用户提到"部署 VQMS""把 VQMS 发到服务器""在 10.0.0.9 跑 VQMS""更新 VQMS 部署""重新部署 VQMS"等时使用。覆盖前置调查、传输源码、生成 .env、分阶段构建启动、验证、回滚。硬约束：不得干扰服务器上现有的 5 个容器（mysql57 / new-api / postgres / redis / portainer）。依赖 ssh-10-0-0-9 skill（本机 ~/.ssh/ssh9.sh）。
version: 1.2.0
---

# VQMS 部署到 10.0.0.9

## 适用场景

在 10.0.0.9 (myubuntu) 上部署或更新 VQMS（RuoYi-Vue 单体 + Vue3 前端，4 容器：mysql/redis/backend/nginx）。服务器上已跑 5 个容器，**部署全过程不得影响它们**。

## 前置依赖

- `ssh-10-0-0-9` skill —— 本机 `~/.ssh/ssh9.sh` 封装（Windows Git Bash 非交互密码 SSH）
- 仓库根有：`docker-compose.yml` + `RuoYi-Vue-springboot3/` + `RuoYi-Vue3/` + `.env.example`（根 `sql/` 目录下有 `00-create-app-user.sh` / `quartz.sql` / `ry_*.sql` / `vqms.sql`）

## 服务器关键信息（2026-08-03 调查，每次部署前复核）

| 项 | 值 |
|----|----|
| 主机 | 10.0.0.9（myubuntu，Ubuntu，4 核，**3.6 GiB 内存**偏紧） |
| 现有容器 | mysql57（★ 即 VQMS 外部数据源 `qheatavchisdb`）/ new-api / postgres / redis / portainer |
| VQMS host 端口 | mysql 13306 · redis 16379 · backend 7000 · nginx 8080（均空闲，不冲突） |
| 容器/网络/卷 | 用 `vqms-*` 前缀 + `vqms-net` 网络 + `mysql_data` 等命名卷，与现有无冲突 |
| 部署路径 | `/home/syth/vqms/` |

## ⚠️ 已知坑（必读，都是实战踩过的）

1. **MySQL 8.4 不支持 `--default-authentication-plugin=mysql_native_password`**——这是 8.0 的参数，8.4 把它移除了，启动直接 `unknown variable ... Aborting`，容器陷入 Restarting 循环。`docker-compose.yml` 的 mysql `command:` **绝不能带这行**。8.4 默认 `caching_sha2_password`，RuoYi 的 mysql-connector-j 原生支持，无需此参数。
2. **`ssh9.sh` 跑长时间后台命令会挂住**：`nohup CMD >log 2>&1 &` 缺 stdin 重定向时，ssh 等待 fd 不返回，触发本地 Bash 120s 超时被移后台。**实际不影响远端 build**（build 完成后 ssh 自动释放，本地任务收到 completed 通知）。两种处理：① 命令加 `< /dev/null`（ssh 不挂，但要自己轮询进度）；② 接受挂住，等后台任务的 completed 通知即可（更简单，构建类命令推荐）。
3. **内存偏紧**（available ~2.7 GiB）：mvn/npm 构建峰值 1–2 GB，可能挤压现有容器。务必 ① 给 mysql/backend 设 `mem_limit` + backend 设 `JAVA_TOOL_OPTIONS: "-Xmx512m"`；② **分阶段构建**（先起 mysql+redis，再 build backend，再 build nginx，串行降峰值，不要 `up --build` 一步并行构建）。
4. **★ 中文菜单/字典变乱码（双重编码 mojibake）**：docker mysql entrypoint 用 `mysql` CLI 导入 `/docker-entrypoint-initdb.d/*.sql` 时 CLI 默认 `character_set_client=latin1`（即使配了 `--character-set-server=utf8mb4` 也管不到 CLI），UTF-8 的 `ry_*.sql` 中文被当 latin1 解码再以 utf8mb4 存储 → 入库即脏（页面"系统管理"变 `è‹¥ä¾é¡¹ç®¡...`）。**判断**：`SELECT HEX(menu_name) FROM sys_menu LIMIT 3`——正常应 `E7B3BBE7BB9F...`，双重编码是 `C3A7C2B3...`。**修复**：挂载 `[mysql] default-character-set=utf8mb4` 客户端配置（见必备项），删 mysql 卷重新初始化（脏数据无法应用层修补）。注意 `--character-set-server=utf8mb4` 只让库表字符集正确，**挡不住导入 CLI 的 latin1**。
5. **bind mount 目录 chown 权限**：宿主普通用户（syth）无 `CAP_CHOWN`，`chown -R 999:999 data/mysql` 报 `Operation not permitted`。改用容器内 root chown：`docker run --rm -v ~/vqms/data:/d alpine sh -c "chown -R 999:999 /d/mysql /d/redis && chown -R 100:101 /d/upload /d/logs"`（uid 以 `docker exec <容器> id <user>` 确认为准：mysql/redis=999，ruoyi=100:101）。**mysql/redis 官方镜像 entrypoint 也能自动 chown 自己的 datadir，但 backend 的 upload/logs（USER ruoyi）不会，必须显式 chown。**
6. **迁移/更新漏传 `sql/` → mysql 不建账号不建表**：只传 `docker-compose.yml` 而不传 `sql/` 目录 → mysql 挂载空 `./sql`，entrypoint 没脚本可跑 → `vqms_app` 账号和 `sys_*`/`voltage_quality_*` 表全没建 → backend `Access denied for user 'vqms_app'` 反复 restart（**mysql 却 healthy，迷惑性强**）。**判断**：`docker compose exec mysql mysql -u$DB_USER -p$DB_PASSWORD ry_vqms -e "show tables;"` 返回空或 Access denied。**修复**：确保 `sql/`（4 脚本）在 compose 同目录，清空 `data/mysql` 后重导。首次部署按步骤 2 完整 tar 传不会漏（清单含 `sql`）；只在"单独传 compose"时踩。

## docker-compose.yml 必备项（本机源头要配对）

```yaml
mysql:
  mem_limit: ${MYSQL_MEM_LIMIT:-700m}     # 参数化到 .env，高性能机器改 .env 即可
  command:
    - --character-set-server=utf8mb4
    - --collation-server=utf8mb4_general_ci
    # 绝不要 --default-authentication-plugin=mysql_native_password（8.4 坑）
  volumes:                                 # ★ charset.cnf 必须挂（坑 4）；mysql 用 bind mount（坑 5 chown）
    - ./data/mysql:/var/lib/mysql          # bind mount 到部署目录（需 chown 999:999，坑 5）
    - ./sql:/docker-entrypoint-initdb.d:ro # ★ sql/ 必须同目录（坑 6，否则不建账号建表）
    - ./mysql-charset.cnf:/etc/mysql/conf.d/charset.cnf:ro
backend:
  mem_limit: ${BACKEND_MEM_LIMIT:-1g}
  environment:
    JAVA_TOOL_OPTIONS: "${BACKEND_JAVA_OPTS:--Xmx512m}"   # JVM 堆，参数化到 .env
    SPRING_DATASOURCE_DRUID_MASTER_USERNAME: ${DB_USER}   # 应用账号，非 root
```

`mysql-charset.cnf`（仓库根，与 compose 同目录，必备——挡住坑 4）：
```ini
[client]
default-character-set = utf8mb4
[mysql]
default-character-set = utf8mb4
```

并确保 `.gitignore` 排除根目录 `/.env`（保护凭据；勿用 `.env*`，会误伤前端的 `.env.development/.env.production`）。

## 部署步骤

### 1. 前置复核（只读）
```bash
~/.ssh/ssh9.sh 'docker ps --format "{{.Names}}: {{.Status}}" | sort; echo "---"; ss -tln | grep -E ":(13306|16379|7000|8080) " || echo "4 端口空闲"; echo "---"; free -h | head -2'
```

### 2. 传输源码（tar 流式；本机无 rsync）
```bash
cd C:/work/VQMS && tar czf - --exclude=node_modules --exclude=target --exclude=dist --exclude=.git --exclude='*.zip' --exclude='*.log' RuoYi-Vue-springboot3 RuoYi-Vue3 sql docker-compose.yml .env.example mysql-charset.cnf | ~/.ssh/ssh9.sh 'mkdir -p ~/vqms && tar xzf - -C ~/vqms && echo "--- sql/ ---" && ls ~/vqms/sql/ && echo "--- charset.cnf ---" && ls ~/vqms/mysql-charset.cnf'
```
预期：sql/ 下 4 个脚本（`00-create-app-user.sh` / `quartz.sql` / `ry_20260417.sql` / `vqms.sql`）。

### 3. 服务器生成 .env（强随机密码）
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && cp .env.example .env && \
 R=$(openssl rand -hex 12) && A=$(openssl rand -hex 12) && RD=$(openssl rand -hex 12) && J=$(openssl rand -hex 32) && \
 sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$R|" .env && \
 sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$A|" .env && \
 sed -i "s|^REDIS_PASSWORD=.*|REDIS_PASSWORD=$RD|" .env && \
 sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$J|" .env && \
 chmod 600 .env && (grep -q change_me .env && echo WARN || echo OK)'
```

### 4. 分阶段启动（核心：不影响现有）

**阶段 A — mysql+redis 先起（官方镜像，零构建峰值）**：
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose up -d mysql redis && \
 for i in $(seq 1 40); do h=$(docker inspect -f "{{.State.Health.Status}}" vqms-mysql); [ "$h" = "healthy" ] && break; sleep 3; done && \
 set -a && . ./.env && set +a && \
 docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" ry_vqms -e "show tables;" 2>&1 | grep -E "voltage_quality|sys_user|QRTZ_" | head'
```
预期：看到 `voltage_quality_daily/monthly/yearly` + `sys_user` + `QRTZ_*`（建库建表建账号成功）。

**阶段 B — build backend（mvn，后台 + 等通知）**：
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && nohup docker compose build backend > /tmp/backend-build.log 2>&1 & disown; echo started'
```
→ 轮询 `tail /tmp/backend-build.log` 到 `Image vqms-backend Built`（或等本地后台任务 completed 通知）。

**阶段 C — build nginx（npm，同上）**：
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && nohup docker compose build nginx > /tmp/nginx-build.log 2>&1 & disown; echo started'
```
→ 到 `Image vqms-nginx Built`。

**阶段 D — up 全部**：
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose up -d && sleep 35 && docker compose logs backend 2>&1 | grep -E "Started RuoYiApplication|启动成功"'
```
预期：`Started RuoYiApplication in X seconds` + 若依启动成功 ASCII art。

### 5. 验证（部署后必跑）
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && echo "=== compose ps ==="; docker compose ps; \
 echo "=== /prod-api/login 反代 ==="; curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/prod-api/login; \
 echo "=== mem（mem_limit 生效）==="; docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"; \
 echo "=== ★ 现有 5 容器复查 ==="; docker ps --format "{{.Names}}: {{.Status}}" | grep -E "mysql57|new-api|postgres|redis|portainer"'
```
通过标准：HTTP 200 + Started RuoYiApplication + backend≤1g/mysql≤700m + 现有 5 容器全 Up。

**中文乱码复核（坑 4，必做）**：
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && set -a && . ./.env && set +a && docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" ry_vqms -e "SELECT HEX(menu_name) FROM sys_menu ORDER BY menu_id LIMIT 2;" 2>/dev/null'
```
HEX 应以 `E7B3BBE7BB9F`（"系统管理"）开头；若看到 `C3A7C2B3...` 就是双重编码，按坑 4 重导。

## 访问
`http://10.0.0.9:8080`（RuoYi 默认 `admin / admin123`，建议尽快改密）。

## 回滚 / 清理（只动 vqms-* 资源）
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose down'      # 停删 vqms 4 容器 + vqms-net（保留卷）
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose down -v'   # 连 4 个数据卷一起删（彻底）
```
**完全不碰** mysql57 / new-api / postgres / redis / portainer。

## 更新已部署的代码
- 改本机文件 → 重跑步骤 2（tar 重传覆盖）→ `docker compose up -d --build <service>`（仅重建变更的服务）。
- 仅改 `.env` / compose：重传 → `docker compose up -d`（compose 检测变化自动重建相关容器）。
- mysql 初始化脚本变了（sql/ 改动）：需 `docker compose down -v` 删卷后重新 up（首启才会重跑 `/docker-entrypoint-initdb.d/`）。
