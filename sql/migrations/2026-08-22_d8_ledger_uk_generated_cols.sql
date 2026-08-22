-- D8 增量迁移：vqms_command_ledger uk 幂等生成列 + 毫秒列与源同宽（2026-08-22）
-- 背景：原 uk_cmd (warn_time, millisecond, obj_num) 中 millisecond/obj_num 可空——MySQL 唯一键
-- 视 NULL 互不相同，NULL 键行重复抓取拦不住（测试方案 §4.7 去重键规格缺口，D8 起草拍板选
-- 生成列候选②：DB 层结构性拦截，无应用层 check-then-insert TOCTOU 竞态）。
-- 本脚本加两枚 STORED 生成列把 NULL 归一（'' / -1，-1 不与真实对象编号撞——现场编号恒正），
-- 并以归一列重建 uk；配合 insert ignore 即幂等。
-- millisecond/millisecond_uk 取 varchar(255) 与源表同宽（D8 对抗验证吸收）：源 warn_info.millisecond
-- 为 varchar(255) 无约束（save_time 同类病灶有先例），收窄会在 insert ignore 下把超宽脏值静默截断、
-- 截断值再进 millisecond_uk 致假碰撞丢行——违背「忠实原文摘录」定位；uk 键长
-- 32*4+255*4+8=1156 字节 < 3072 InnoDB 上限，放宽无代价。v5.0 §6.2.6 初版规约的 varchar(8) 由本迁移 supersede。
-- 同步 vqms.sql / sql/5.0/vqms_tables.sql 已含最终同款。
-- 适用：已按 D8 前 vqms.sql（或 v5.0 §6.2.6 初版规约 DDL）初始化的库；新初始化直接跑 vqms.sql（已含同款）。
-- 执行史：10.0.0.9 ry_vqms 已执行（先以初版窄列形态应用、当日对抗验证后补 modify 加宽——两步终态与本文件一致）。
-- ⚠️ 非幂等：重复执行时 add column 报 ERROR 1060 / modify 同值无害，属预期。

alter table vqms_command_ledger
  add column millisecond_uk varchar(255) generated always as (coalesce(millisecond, '')) stored
    comment 'uk 键列：millisecond NULL 归一空串（应用不读写）' after millisecond,
  add column obj_num_uk bigint(20) generated always as (coalesce(obj_num, -1)) stored
    comment 'uk 键列：obj_num NULL 归一 -1（应用不读写）' after obj_num,
  drop index uk_cmd,
  add unique key uk_cmd (warn_time, millisecond_uk, obj_num_uk),
  modify column millisecond varchar(255) default null comment '毫秒原文（warn_info.millisecond 原样摘录，与源同宽防截断失真）';
