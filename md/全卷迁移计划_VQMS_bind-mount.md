# VQMS 全卷迁移计划 — named volume → bind mount

> 目标：把 vqms 4 个数据卷从 docker **named volume**（深路径 `/var/lib/docker/volumes/`）改为 **bind mount** 到部署目录 `~/vqms/data/`，路径浅、可直接访问、备份直观（`tar data/`）。
> 状态：本机 `docker-compose.yml` 已改好（4 卷 bind mount + 顶级 `volumes:` 段移除）；本文档是**服务器执行计划，未执行**。

---

## 1. 背景：当前 named volume

| 卷 | 容器内挂载 | named volume 宿主路径（深、归 root） |
|---|---|---|
| mysql_data | `/var/lib/mysql` | `/var/lib/docker/volumes/vqms_mysql_data/_data` |
| redis_data | `/data` | `/var/lib/docker/volumes/vqms_redis_data/_data` |
| backend_upload | `/home/ruoyi/uploadPath` | `/var/lib/docker/volumes/vqms_backend_upload/_data` |
| backend_logs | `/home/ruoyi/logs` | `/var/lib/docker/volumes/vqms_backend_logs/_data` |

痛点：路径深、归 root、不便直接 `ls`/备份，生命周期由 docker 管不直观。

## 2. 目标：bind mount 到 ~/vqms/data/

| 服务 | 容器内挂载 | bind mount 宿主路径 | 宿主目录权限 |
|---|---|---|---|
| mysql | `/var/lib/mysql` | `~/vqms/data/mysql` | `999:999` |
| redis | `/data` | `~/vqms/data/redis` | `999:999` |
| backend | `/home/ruoyi/uploadPath` | `~/vqms/data/upload` | `ruoyi:ruoyi`（uid 见 §3） |
| backend | `/home/ruoyi/logs` | `~/vqms/data/logs` | `ruoyi:ruoyi` |

compose 改动（本机已完成）：`./data/<name>:<容器路径>`，顶级 `volumes:` 段已删（无命名卷声明）。

## 3. 前置：确认各 uid（必做）

```bash
~/.ssh/ssh9.sh 'echo "mysql:"; docker exec vqms-mysql id mysql; echo "redis:"; docker exec vqms-redis id redis; echo "ruoyi:"; docker exec vqms-backend id ruoyi'
```
预期：mysql / redis = **uid 999**；ruoyi = alpine `adduser -S` 动态分配（常见 100–999 区间）。**记下 ruoyi 的 uid/gid**，§5.1 要用。

## 4. 数据迁移策略

| 卷 | 策略 | 影响 |
|---|---|---|
| mysql | **重导（方案 A）** | bind mount 空目录 → mysql 首启重跑 `sql/`（`mysql-charset.cnf` 保 utf8mb4）。当前首次部署**无业务数据**，可接受 |
| redis | 重启清空 | token / 验证码 / 字典缓存清空 → **用户需重新登录**。无业务数据损失 |
| backend upload | 空目录 | 首次部署无上传文件 |
| backend logs | 空目录 | 丢历史日志（若要保留：`docker run --rm -v vqms_backend_logs:/from alpine cp -a /from/. ~/vqms/data/logs/`） |

## 5. 执行步骤（服务器，会短暂中断 vqms 服务 ~1–2 分钟）

### 5.1 建目录 + chown
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && mkdir -p data/{mysql,redis,upload,logs} && \
 chown -R 999:999 data/mysql data/redis && \
 chown -R <RUOYI_UID>:<RUOYI_GID> data/upload data/logs && \
 ls -la data/'
```
（`<RUOYI_UID>:<RUOYI_GID>` 用 §3 查到的 ruoyi uid:gid 替换）

### 5.2 传新 compose（本机改好的）
```bash
cd C:/work/VQMS && tar czf - docker-compose.yml | ~/.ssh/ssh9.sh 'cd ~/vqms && tar xzf -'
```

### 5.3 停删 4 容器 + 删旧 named volume
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose down && \
 docker volume rm vqms_mysql_data vqms_redis_data vqms_backend_upload vqms_backend_logs 2>&1'
```

### 5.4 重新启动（bind mount 生效）
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose up -d && echo "等启动..." && sleep 40 && docker compose ps'
```
mysql 首启重导 4 个 sql（~30–60s）；backend 等 mysql healthy 后起。

### 5.5 重启 backend（确保连新 mysql + 加载干净数据）
```bash
~/.ssh/ssh9.sh 'cd ~/vqms && docker compose restart backend && sleep 25 && docker compose logs backend 2>&1 | grep "Started RuoYiApplication" | tail -1'
```

## 6. 验证（迁移后必跑）

```bash
~/.ssh/ssh9.sh 'cd ~/vqms && set -a && . ./.env && set +a && \
 echo "=== mysql HEX（应 E7B3BB... 非 C3A7C2...）==="; \
 docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" ry_vqms -e "SELECT HEX(menu_name) FROM sys_menu LIMIT 2;" 2>/dev/null && \
 echo "=== bind mount 目录有数据 ==="; ls data/mysql/ | head -5; echo "redis:"; ls data/redis/ | head -3 && \
 echo "=== /prod-api/login 反代 ==="; curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/prod-api/login && \
 echo "=== ★ 现有 5 容器复查（无干扰）==="; docker ps --format "{{.Names}}: {{.Status}}" | grep -E "mysql57|new-api|postgres|redis|portainer"'
```
通过标准：HEX 正常 UTF-8 + `data/` 目录有文件 + HTTP 200 + 现有 5 容器全 Up。

## 7. 影响评估

| 项 | 影响 |
|---|---|
| vqms 4 容器 | 全部 recreate，服务中断 ~1–2 分钟（mysql 重导 + 启动） |
| 现有 5 容器 | **无干扰**（只动 vqms-* + vqms-net） |
| 用户会话 | redis 缓存清空 → 需重新登录 |
| mysql 数据 | 重导（首次部署无业务数据损失） |
| 磁盘 | 旧 4 个 named volume 删除腾空间；新数据在 `~/vqms/data/` |

## 8. 回滚

改回 named volume 版 compose（各 service `<svc>_data:<path>` + 顶级 `volumes:` 声明 4 个卷），`docker compose up -d`。或从 git 历史恢复旧 `docker-compose.yml`（bind mount 改动前的 commit）。

## 9. 待确认事项（执行前拍板）

1. **ruoyi uid/gid**：§3 查到后填入 §5.1。
2. **backend logs 是否迁移**：默认丢（空目录）；若要保留历史日志，用 §4 的 `docker run cp` 迁移。
3. **执行时机**：低峰（vqms 中断 ~2 分钟；现有服务不受影响）。
4. **本机 compose 是否先 commit**：建议执行前 commit，便于回滚。
