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

## 第 1 步：开远程账号（最小权限，不开 root）

不建议把现有 `vqms_app` 改成 `@'%'`（会放宽为任意主机）。新建**专用远程账号**，host 限定到你的 LAN IP：

```sql
CREATE USER 'vqms_remote'@'<你的LAN IP>' IDENTIFIED BY '<强密码>';
GRANT SELECT ON ry_vqms.* TO 'vqms_remote'@'<你的LAN IP>';
-- 需要写权限再单独加；只读排查足矣
FLUSH PRIVILEGES;
```

- IP 会变：先用 `@'%'` 临时连，连上后缩回具体 IP。
- **认证插件**：MySQL 8.4 默认 `caching_sha2_password`。新版客户端（Workbench / DBeaver / Navicat）原生支持；老客户端报 `caching_sha2_password` 错误时，对该用户：
  ```sql
  ALTER USER 'vqms_remote'@'<IP>' IDENTIFIED WITH mysql_native_password BY '<强密码>';
  ```

## 第 2 步：主机防火墙（若 ufw 开着）

```bash
sudo ufw allow from <你的LAN IP> to any port 13306
```

## 安全红线（沿用 CLAUDE.md §Security）

- **不开 `root@'%'`**，绝不把 root 暴露到网络。
- 远程账号用只读 + 限定 host；密码走 `.env` 或口头，**不写进被 git 跟踪的文件**。

## 连接串（配置好后给客户端）

```
host:     10.0.0.9
port:     13306
user:     vqms_remote
password: <强密码>
database: ry_vqms
```
