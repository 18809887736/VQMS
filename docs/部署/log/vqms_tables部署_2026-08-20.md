# 执行记录：vqms_tables.sql 部署到 10.0.0.9 ry_vqms 库

**日期**：2026-08-20（下午）
**来源**：`sql/5.0/vqms_tables.sql`（v5.0 §6.2 权威口径摘录，6 张管理表 + 种子）
**操作人**：Claude（Leo 指令）
**状态**：✅ 已完成，6 表 + 种子全量验证通过

---

## 目标与环境

| 项 | 值 |
|---|---|
| 目标库 | `ry_vqms` @ `vqms-mysql`（mysql:8.4 容器，healthy，host 端口 13306） |
| 服务器 | 10.0.0.9（myubuntu），compose 项目 `/home/syth/vqms` |
| 部署前库内现状 | 仅 RuoYi `sys_*`/`QRTZ_*`/`gen_*` + 旧 v3.4 残留 4 表（`voltage_quality_*` ×3 + `precompute_cursor`）——见当日 11:47 Navicat dump（`sql/5.0/backup/ry_vqms.sql`） |
| 旧残留处理 | **未动**（Leo 拍板清理时再清；旧 4 表与 v5.0 口径无关，见 `docs/遗留问题.md` 第四条） |

## 过程

### 1. 传输与校验

- `ssh9.sh 'cat > /tmp/vqms_tables.sql'` 传输本地文件 → md5 前后一致（`2c21c63140be1da52ccca68e3499a5d6`，13428 字节），UTF-8 完好。
- 注：`~/vqms/sql/`（容器 `/docker-entrypoint-initdb.d` 挂载点）对 syth 只读，故落 `/tmp`。

### 2. 认证绕坑（关键发现）

`docker exec vqms-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD` **Access denied**，两处密码均不符：

- `.env` 现值（今晨改密的 `root@'%'` 新密码）→ 命中 `root@'localhost'` 条目（exec 走 unix socket）；
- 容器环境变量值（容器创建时传入）→ 数据卷实际密码以首次初始化为准，同样不符。

**解法**：同网络临时客户端容器走 TCP，命中 `root@'%'`：

```bash
cd ~/vqms && PW=$(grep "^MYSQL_ROOT_PASSWORD=" .env | cut -d= -f2)
docker run --rm -i --network vqms_vqms-net mysql:8.4 \
  mysql -hvqms-mysql -uroot -p"$PW" --default-character-set=utf8mb4 ry_vqms \
  < /tmp/vqms_tables.sql
```

- 密码仅在服务器内存 / `.env` 间流转，未进任何 git 跟踪文件、未回显。
- ⚠️ **遗留**：`root@'localhost'` 实际密码已不可考（两个来源都不对）——以后 `docker exec` 内排查需先经 `root@'%'` TCP 路线，或某次维护窗口重置 localhost 条目密码。

### 3. 执行与验证

执行零报错。验证结果（`-t` 表格输出逐项核对）：

| 表 | 行数 | 种子核对 |
|---|---|---|
| `vqms_busbar` | 2 | 220kV 东/西母线，v_grade=1，nominal_kv=220.000 ✅ |
| `vqms_busbar_group` | 2 | 220kV 组指示点 4001；500kV 组 NULL（待补录）✅ |
| `vqms_yc_point_map` | 3 | 4001/3009/2003，`gate_enabled` 全 0（保守默认）✅ |
| `vqms_judge_param` | 4 | t_fast=4、t_econ=5、tier_threshold 1/5 ✅ |
| `vqms_busbar_threshold` | 2 | tolerance_v=1.000 kV，effective_from=2026-01-01 ✅ |
| `vqms_command_ledger` | 0 | 无种子（D8 抓取填充）✅ |

中文注释/名称经 utf8mb4 往返无乱码。`/tmp` 临时文件已清理。

## 本次不含（待办）

- **字典 `vqms_v_grade` + 菜单/权限**：在 `sql/vqms.sql`（及 `sql/vqms_menu.sql`），本次未部署——前端电压等级筛选依赖字典，需 Leo 确认后另行执行。
