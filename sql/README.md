# sql/ — VQMS 数据库初始化脚本

## 作用与触发机制

本目录只读挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d`（`docker-compose.yml`）。
**仅数据库首次初始化时按文件名顺序自动执行一次**；已初始化（数据卷已存在）后 docker 不重跑，改脚本不会影响运行中的库。

每次脚本都含 `DROP TABLE IF EXISTS` + 初始数据，**线上变更表结构请勿整脚本重跑**，用 ALTER / 增量迁移脚本。

## 执行顺序

```
00-create-app-user.sh   # 建最小权限应用账号 vqms_app（读 .env 的 DB_USER/DB_PASSWORD）
quartz.sql             # Quartz 调度表（QRTZ_*）
ry_20260417.sql        # RuoYi sys_* 管理表 + 初始数据
vqms.sql              # VQMS 管理表（busbar / busbar_group / busbar_threshold / yc_point_map /
                     #   vqms_judge_param / vqms_command_ledger）+ 字典 vqms_v_grade +
                     #   末尾 UPDATE 覆盖 ry 默认（关验证码）
vqms_menu.sql         # VQMS 业务菜单（sys_menu）+ 普通角色(role_id=2)授权，最后跑（依赖 sys_menu 已建）
```

## 文件说明

| 文件 | 角色 | 权威依据 |
|---|---|---|
| `00-create-app-user.sh` | 首启建库后建 `vqms_app` 最小权限账号（`SELECT/INSERT/UPDATE/DELETE` 仅 `ry_vqms.*`，无 DDL/GRANT） | CLAUDE.md §Security（账号分离） |
| `quartz.sql` | RuoYi Quartz 原生调度表，原样不动 | RuoYi 原生 |
| `ry_20260417.sql` | RuoYi `sys_*` 管理表 + 初始数据，原样不动 | RuoYi 原生 |
| `vqms.sql` | VQMS 自建管理表 + 字典 + 关验证码覆盖 | 项目规划_v5_0.md §6.2 / AVC考核核心算法_草稿v5_0 §2 |
| `vqms_menu.sql` | VQMS 业务菜单 + 角色授权 | 前端 `src/views/vqms/` 7 页 |

## 文档同步说明

- `vqms.sql` 管理表 DDL 以 **项目规划 v5.0**（原 v4.1 已于 2026-08-18 并入 v5.0）§6.2 为准；
  判定口径以 **AVC考核核心算法_草稿v5_0** §2 为准。
- `ry_*` / `quartz.sql` 为 RuoYi 原生，按 CLAUDE.md「不修改 RuoYi 原生模块」原样复用。

## 已知待清理（文档债，非 sql/ 范围）

- `docker-compose.yml` 注释声称 `vqms.sql` 建 `voltage_quality_* 四张统计表`——该表已在 vqms.sql §二~四 删除（改指令级口径），注释过时，待修正。
