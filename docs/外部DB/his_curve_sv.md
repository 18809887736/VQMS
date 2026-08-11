# 鸡西 QCzt AVC 盒子（10.0.0.35）— his_curve_sv 表结构和数据

> MySQL 5.7 | 数据库: `qheatavchisdb` | 采集时间: 2026-07-30

---

## `his_curve_sv` — 历史曲线（母线电压 SV）

### 表结构

```sql
CREATE TABLE `his_curve_sv` (
  `save_time`    varchar(255) DEFAULT NULL,   -- 存盘时间 (例: 2026-07-30 16:06:00.4)
  `busbar_num`   bigint(20)   DEFAULT NULL,   -- 母线编号 (0 / 1)
  `high_SV`      decimal(10,0) DEFAULT NULL,  -- 电压上限
  `low_SV`       decimal(10,0) DEFAULT NULL,  -- 电压下限
  `average_SV`   decimal(10,0) DEFAULT NULL,  -- 电压平均值
  `plan_SV`      decimal(10,0) DEFAULT NULL   -- 计划电压值 (原始遥测编码)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

- **主键/索引**: 无
- **数据量**: 8,225 行 (2026-07-30)
- **写入频率**: 约 1 分钟/条，每次双写母线 0 和 1

### 字段说明

| 字段 | 含义 | 示例值 |
|------|------|--------|
| `save_time` | 存盘时间戳 | `2026-07-30 16:06:00.4` |
| `busbar_num` | 母线编号 | `0` / `1` |
| `high_SV` | 电压上限 | `234` |
| `low_SV` | 电压下限 | `234` |
| `average_SV` | 电压平均值 | `234` |
| `plan_SV` | 计划电压（原始编码） | `10245` |

### 最新数据（最近 5 条）

| save_time | busbar_num | high_SV | low_SV | average_SV | plan_SV |
|-----------|------------|---------|--------|------------|---------|
| 2026-07-30 16:06:00.4 | 0 | 234 | 234 | 234 | 10245 |
| 2026-07-30 16:06:00.4 | 1 | 234 | 234 | 234 | 10245 |
| 2026-07-30 16:05:00.3 | 0 | 234 | 234 | 234 | 10245 |
| 2026-07-30 16:05:00.3 | 1 | 234 | 234 | 234 | 10245 |
| 2026-07-30 16:04:00.2 | 0 | 234 | 234 | 234 | 10245 |
