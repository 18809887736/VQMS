-- D9 增量迁移：新增 vqms_policy_param 数据不可用策略参数表（2026-08-22）
-- 背景：v5.0 §8.7 参数化策略骨架（确定轨 D9）——甲/乙/丙/丁 = 同一策略评估纯函数的四组配置，
-- 换策略 = 改本表几行、代码不动。选套值留空（Leo 2026-08-18 拍板）：零种子行、不预设处置值。
-- 约定键：undecodable_mode / invalid_tier_mode / partial_missing_mode ∈
--   {COUNT_NORMAL, EXCLUDE_REPORTED, COUNT_UNQUALIFIED, PEND_MARKED}（statistics.Disposition 枚举名）；
--   partial_missing_threshold_pct = 整数百分比（partial_missing_mode=EXCLUDE_REPORTED 时必填，乙档建议 50）。
-- 无 CHECK 钉值（此刻无政策值可钉，钉枚举白名单会堵死候选演进；选套定稿后再随 UI 加约束）。
-- 适用：未建本表的存量库；新初始化直接跑 vqms.sql（已含同款）。
-- ⚠️ 非幂等：表已存在时报 ERROR 1050 属预期，跳过即可。

create table vqms_policy_param (
  param_id     bigint(20)   not null auto_increment comment '主键',
  param_key    varchar(64)  not null                comment '参数键（约定键见上注）',
  param_value  varchar(255) default null            comment '参数值（字符串枚举/整数文本；选套前整表留空）',
  name         varchar(64)  not null                comment '参数名称',
  description  varchar(255) default null            comment '说明',
  create_by    varchar(64)  default ''              comment '创建者',
  create_time  datetime     default current_timestamp comment '创建时间',
  update_by    varchar(64)  default ''              comment '更新者',
  update_time  datetime     default null            comment '更新时间',
  primary key (param_id),
  unique key uk_policy_key (param_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 数据不可用策略参数表（D9 骨架，选套留空）';
