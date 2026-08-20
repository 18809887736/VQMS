# vqms-mysql (8.4) 远程连接配置规划

> 状态：**仅规划，未执行**。
> 缘起：业务侧需远程访问 10.0.0.9 上的 `vqms-mysql`（MySQL 8.4）以排查/核对统计。
> 关联：`部署计划_VQMS_10.0.0.9.md`（§1.3：vqms-mysql host 端口 `13306`，已映射 `0.0.0.0:13306`，IPv4+IPv6）。

## 现状（已确认）

- 容器 `vqms-mysql`（mysql:8.4）已运行 2+ 周，healthy。
- 端口映射：`3306/tcp -> 0.0.0.0:13306`（`[::]:13306`）——**网络通路已开**，远程连不上几乎都是 MySQL 用户 host 权限问题，不是网络。
- 现有 5 容器（mysql57/new-api/postgres/redis/portainer）未受干扰。

## 第 0 步：先只读确认卡点（用 `.env` 里的密码）

```bash
# 用户与 host 白名单
docker exec vqms-mysql mysql -u<user> -p<pass> -e "SELECT user,host FROM mysql.user;"
# 8.4 容器默认 bind_address=0.0.0.0，通常无需改
docker exec vqms-mysql mysql -u<user> -p<pass> -e "SHOW VARIABLES LIKE 'bind_address';"
# 主机防火墙（若 ufw 启用，且没放行 13306）
sudo ufw status | grep 13306   # 空 = 未放行
```

多数情况 `vqms_app` 是 `@'localhost'` 或 `@'vqms-net'`，远程客户端拿不到 `@'%'`。

## 第 1 步：开远程 root 账号（**Leo 拍板：覆盖安全红线**）

> ⚠️ **显式覆盖 `CLAUDE.md §Security`**：原规划是最小权限专用账号；Leo 要求直接开 `root@'%'`。这是内网服务器、本人拍板，照记。风险已记入 §安全红线，执行时务必用强随机密码 + 仅限内网访问。

```sql
-- 8.4 已默认有 root@'localhost'；追加一个任意主机可达的 root
CREATE USER 'root'@'%' IDENTIFIED BY '<强随机密码(≥32位)>';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

- **认证插件**：MySQL 8.4 默认 `caching_sha2_password`。新版客户端原生支持；老客户端报 `caching_sha2_password` 错误时：
  ```sql
  ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '<强随机密码>';
  ```
- 若 root 已有 `@'%'` 条目（`SELECT user,host FROM mysql.user WHERE user='root';` 可见），直接 `ALTER USER 'root'@'%' IDENTIFIED BY '<强随机密码>';` 改密即可，无需 CREATE。

## 第 2 步：主机防火墙（若 ufw 开着）

```bash
sudo ufw allow from <你的LAN IP> to any port 13306
```

## 安全红线（**本次被 Leo 拍板覆盖**，原 CLAUDE.md §Security）

- **已覆盖**：本次开 `root@'%'`，与 CLAUDE.md "不开 root 远程" 红线相悖——Leo 在内网亲自拍板，照办。
- **残余风险（执行时仍要守住）**：`root@'%'` 把超级权限暴露到任意主机。`docker-compose.yml` 主机端口当前是 `0.0.0.0:13306`（全网卡监听）。**强烈建议**执行后至少做到：
  - 密码用强随机 ≥32 位，不入任何 git 跟踪文件（走 `.env` / 口头）；
  - 若本机非公网、仅内网用，考虑 ufw 限定来源 IP：`sudo ufw allow from <你的LAN IP> to any port 13306`；
  - 该账号仅用于临时排查，事后可 `DROP USER 'root'@'%';` 收口。

## 连接串（配置好后给客户端）

```
host:     10.0.0.9
port:     13306
user:     root
password: <强随机密码>
database: ry_vqms
```
