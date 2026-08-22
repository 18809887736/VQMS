-- D7 增量迁移：vqms_judge_param 值域/锁定 CHECK（2026-08-22）
-- 背景：初版 ck_locked_rows 误写成键白名单（OR 链要求谓词 TRUE）——任何新 param_key 的 INSERT
-- 四支全 false 被拒，Add 端到端死路（D7 对抗验证 blocker）。本脚本替换为蕴含式（非该键恒真，
-- 仅锁定键钉值/值域列），并把必需键的值域列一并钉死（防 value_min/max 置 NULL 的两步旁路）。
-- 适用：已按 D7 前版 vqms.sql（或 v5.0 §6.2.5 规约 DDL）初始化的库。新初始化直接跑 vqms.sql（已含同款）。
-- ⚠️ 非幂等：MySQL 8.4 无 DROP CHECK IF EXISTS——约束已不存在时前两条 drop 报 ERROR 3821 中断，
--    属预期，重跑时跳过前两条仅执行末段 alter add；若 add 因存量脏行报 3819，先修行数据再重跑。

alter table vqms_judge_param drop check ck_value_range;
alter table vqms_judge_param drop check ck_locked_rows;

alter table vqms_judge_param
  add constraint ck_value_range check (value_min is null or value_max is null or param_value between value_min and value_max),
  add constraint ck_locked_rows check (
    (param_key <> 't_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 'tier_threshold_fast' or (param_value <=> 1 and value_min <=> 1 and value_max <=> 1))
    and (param_key <> 'tier_threshold_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 't_fast' or (value_min <=> 1 and value_max <=> 4))
  );

-- 注：比较一律用 NULL 安全等值 <=>——普通 = 遇 NULL 求值 NULL，MySQL CHECK 只拒 FALSE，
-- `FALSE OR NULL` 会放行（value_min 置 NULL 的两步旁路，D7 对抗验证实测）。
