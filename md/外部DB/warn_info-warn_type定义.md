# warn_info 表 warn_type 字段定义

> 来源：`qheatavchisdb.warn_info`（外部数据源，MySQL 5.7）

## 表结构

```sql
CREATE TABLE `warn_info` (
  `warn_time`    varchar(255)  -- 告警时间（格式：yyyy-MM-dd HH:mm:ss.SSS）
  `millisecond`  varchar(255)  -- 毫秒
  `warn_type`    bigint(20)    -- 告警类型（见下方枚举）
  `obj_num`      bigint(20)    -- 对象编号
  `warn_info`    varchar(255)  -- 告警描述（中文）
);
```

无主键、无索引。

## warn_type 枚举（C 头文件 #define）

| 值 | 十六进制 | 宏名 | 中文含义 |
|---:|:--------:|------|---------|
| 0 | `0x00` | `WARN_INFO_TYPE_REDUNDANCY` | 冗余 |
| 1 | `0x01` | `WARN_INFO_TYPE_YC` | 遥测 |
| 2 | `0x02` | `WARN_INFO_TYPE_YX` | 遥信 |
| 3 | `0x03` | `WARN_INFO_TYPE_ALARM` | 告警 |
| 4 | `0x04` | `WARN_INFO_TYPE_YK` | 遥控 |
| 5 | `0x05` | `WARN_INFO_TYPE_YT` | 遥调 |
| 6 | `0x06` | `WARN_INFO_TYPE_YS` | 遥善 |
| 7 | `0x07` | `WARN_INFO_TYPE_BUSBAR` | 母线 |
| 8 | `0x08` | `WARN_INFO_TYPE_GENERATOR` | 机组 |
| 9 | `0x09` | `WARN_INFO_TYPE_MONITOR` | 监控 |
| 10 | `0x0A` | `WARN_INFO_TYPE_SVG` | SVG（静止无功发生器）|
| 11 | `0x0B` | `WARN_INFO_TYPE_CAPACITOR` | 电容器 |
| 12 | `0x0C` | `WARN_INFO_TYPE_VOLTCAPACITOR` | 电压电容器 |
| 13 | `0x0D` | `WARN_INFO_TYPE_BATTERY` | 电池 |
| 14 | `0x0E` | `WARN_INFO_TYPE_TRANSFORMER` | 变压器 |
| 15 | `0x0F` | `WARN_INFO_TYPE_ADJUST` | 调节 |
| 16 | `0x10` | `WARN_INFO_TYPE_WATCHDOG` | 看门狗 |
| 17 | `0x11` | `WARN_INFO_TYPE_BUSBAR_ADJUST` | 母线调节 |

## 样本数据中实际出现的 warn_type

| warn_type | 含义 | 示例 warn_info |
|:---------:|------|---------------|
| 2 | 遥信 (YX) | `一号机组系统电压越上限闭锁分`、`220KV正母线通讯故障闭锁分` |
| 7 | 母线 (BUSBAR) | `220Kv正母线检查AVC远方就地状态闭锁`、`220Kv副母线非主母线，调节退出!` |

> 当前样本数据仅包含 `warn_type = 2`（遥信）和 `warn_type = 7`（母线），其他类型暂无数据。
