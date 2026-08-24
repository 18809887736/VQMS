-- S5 选套应用：乙（阈值剔除+计数）写入 vqms_policy_param 四约定键（2026-08-24 Leo 拍板）
-- 背景：政策选套拍板——Leo 2026-08-24 指定「乙（推荐）：缺失超阈值剔分母+计数」；
--   阈值取默认 50（可用度百分比，实现口径 = 可用 ≥50% 用剩余、<50% 剔除+计数，
--   ⚠ UNVERIFIED 边界方向随正式政策文件复核——两份权威文档表述相反，见 §8.7 UI 设计注）。
-- 与固定分母拍板的关系：正式版 §2.7 披露合格率仍按全量分母计算；EXCLUDE_REPORTED 为
--   罚款缺额链的剔除口径（推荐读法），逐行落 disposition 列供审计与重算切换。
-- 适用：本部署实例（ry_vqms）；全新部署仍从空表起步（选套属实例级配置，不入首初始化脚本）。
-- 幂等：可重复执行。

update vqms_policy_param set param_value='EXCLUDE_REPORTED', update_by='leo', update_time=now()
  where param_key='undecodable_mode';
insert into vqms_policy_param (param_key, param_value, name, description, create_by)
  select 'undecodable_mode', 'EXCLUDE_REPORTED', '数据不可用处置·undecodable_mode',
         '乙（阈值剔除+计数）选套写入 2026-08-24', 'leo'
  where not exists (select 1 from vqms_policy_param where param_key='undecodable_mode');

update vqms_policy_param set param_value='EXCLUDE_REPORTED', update_by='leo', update_time=now()
  where param_key='invalid_tier_mode';
insert into vqms_policy_param (param_key, param_value, name, description, create_by)
  select 'invalid_tier_mode', 'EXCLUDE_REPORTED', '数据不可用处置·invalid_tier_mode',
         '乙（阈值剔除+计数）选套写入 2026-08-24', 'leo'
  where not exists (select 1 from vqms_policy_param where param_key='invalid_tier_mode');

update vqms_policy_param set param_value='EXCLUDE_REPORTED', update_by='leo', update_time=now()
  where param_key='partial_missing_mode';
insert into vqms_policy_param (param_key, param_value, name, description, create_by)
  select 'partial_missing_mode', 'EXCLUDE_REPORTED', '数据不可用处置·partial_missing_mode',
         '乙（阈值剔除+计数）选套写入 2026-08-24', 'leo'
  where not exists (select 1 from vqms_policy_param where param_key='partial_missing_mode');

update vqms_policy_param set param_value='50', update_by='leo', update_time=now()
  where param_key='partial_missing_threshold_pct';
insert into vqms_policy_param (param_key, param_value, name, description, create_by)
  select 'partial_missing_threshold_pct', '50', '数据不可用处置·partial_missing_threshold_pct',
         '乙（阈值剔除+计数）选套写入 2026-08-24', 'leo'
  where not exists (select 1 from vqms_policy_param where param_key='partial_missing_threshold_pct');
