# RuoYi 原生表名清单（ry_20260417.sql + quartz.sql）

> 提取来源：`sql/5.0/backup/ry_20260417.sql`、`sql/5.0/backup/quartz.sql`
> 提取日期：2026-08-20
> 说明：VQMS 直接复用这些表，**不重建、不修改**（CLAUDE.md「不修改 RuoYi 原生模块」）。本清单为权威表名字典，供 VQMS 后端 / 前端联调时对照，避免误建重名表或错引列。

## 一、RuoYi 业务/系统表（ry_20260417.sql，20 张）

| 序号 | 表名 | 用途 |
|---|---|---|
| 1 | `sys_user` | 用户表 |
| 2 | `sys_dept` | 部门表 |
| 3 | `sys_role` | 角色表 |
| 4 | `sys_menu` | 菜单权限表（VQMS 业务菜单 vqms_menu.sql 追加于此） |
| 5 | `sys_role_menu` | 角色-菜单关联 |
| 6 | `sys_role_dept` | 角色-部门关联 |
| 7 | `sys_user_role` | 用户-角色关联 |
| 8 | `sys_user_post` | 用户-岗位关联 |
| 9 | `sys_post` | 岗位表 |
| 10 | `sys_dict_type` | 字典类型（含 VQMS 自建 `vqms_v_grade`） |
| 11 | `sys_dict_data` | 字典数据 |
| 12 | `sys_config` | 参数配置（验证码开关等，VQMS 关验证码 UPDATE 覆盖此处） |
| 13 | `sys_notice` | 通知公告 |
| 14 | `sys_notice_read` | 通知已读记录 |
| 15 | `sys_oper_log` | 操作日志 |
| 16 | `sys_logininfor` | 登录日志 |
| 17 | `sys_job` | 定时任务（Quartz 任务定义） |
| 18 | `sys_job_log` | 定时任务日志 |
| 19 | `gen_table` | 代码生成-业务表 |
| 20 | `gen_table_column` | 代码生成-业务表字段 |

## 二、Quartz 调度表（quartz.sql，11 张，大写）

| 表名 | 用途 |
|---|---|
| `QRTZ_JOB_DETAILS` | 任务详情 |
| `QRTZ_TRIGGERS` | 触发器 |
| `QRTZ_SIMPLE_TRIGGERS` | 简单触发器 |
| `QRTZ_CRON_TRIGGERS` | Cron 触发器 |
| `QRTZ_SIMPROP_TRIGGERS` | 复合属性触发器 |
| `QRTZ_BLOB_TRIGGERS` | Blob 触发器 |
| `QRTZ_FIRED_TRIGGERS` | 已触发触发器 |
| `QRTZ_PAUSED_TRIGGER_GRPS` | 暂停的触发器组 |
| `QRTZ_SCHEDULER_STATE` | 调度器状态 |
| `QRTZ_LOCKS` | 锁表 |
| `QRTZ_CALENDARS` | 日历 |

## 三、与 VQMS 自建表的关系

- VQMS 自建管理表（`vqms_busbar` / `vqms_busbar_group` / `vqms_yc_point_map` / `vqms_busbar_threshold` / `vqms_judge_param` / `vqms_command_ledger`，见 `../vqms_tables.sql`）与以上表**同库共存、互不重名**。
- VQMS 仅向 RuoYi 表**写入/追加数据**：`sys_menu` / `sys_role_menu`（业务菜单，见 `../../vqms_menu.sql`）、`sys_dict_type` / `sys_dict_data`（`vqms_v_grade` 字典，见 `../../vqms.sql` 第六节）；并 UPDATE `sys_config`（关验证码）。
- **切勿**对这些原生表改结构或加列；新增能力一律走 `ruoyi-vqms` 业务模块 + 上表自建表。
