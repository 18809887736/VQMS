-- S2/S3 增量迁移：调节合格率统计表 + 投运率记账表 + 母线组容量列（2026-08-24）
-- 背景：搁置轨 S2/S3 落库 DDL 定稿——设计稿四项决策①~④ + 开放点⑤⑥ Leo 当日全批推荐项
-- （docs/S2_S3_统计落库DDL设计稿_2026-08-24.md；拍板记录见其 §六）。
-- 内容：
--   1) vqms_busbar_group 增列 rated_capacity_kw（决策⑤：罚款单价基数载体，NULL=待现场补录）
--   2) 新建 vqms_regulation_cmd / _daily / _monthly / _yearly（S2 指令级明细 + rollup，决策①②③④落列）
--   3) 新建 vqms_runtime_daily / _monthly / _yearly（S3 时间记账，分钟分类不落主库只落周期计数）
-- 适用：已初始化的存量库（新库直接跑 vqms.sql，已含同款 DDL）。
-- ⚠️ 非幂等：表已存在时报 ERROR 1050 / 列已存在报 1060 属预期，跳过即可。
-- 与 vqms.sql 的差异说明：本文件用 ALTER/CREATE 无 DROP（存量库防误清），
--   vqms.sql 为首初始化脚本保留 DROP IF EXISTS 风格。

-- 0) S4 Slice1 配置补缺（2026-08-24）：母线 t0 实时电压点位 + 免考旗点号种子
alter table vqms_busbar
  add column realtime_yc_num bigint(20) default null
  comment '该母线 t0 实时电压 yc 点（增量指令算 V_target 用，正式版 §2.1；NULL=未接入→增量指令按缺t0如实判不了）。合成 4002；真实候选 yc8 东母/yc14 西母 待现场核对【S4 Slice1 2026-08-24】';
update vqms_busbar set realtime_yc_num = 4002 where busbar_num in (0, 1);

insert into vqms_yc_point_map (yc_num, point_name, point_type, state_1_label, state_0_label, gate_enabled) values
  (501, '免考旗', 'yx', '免考', '考核', 0);
-- 501 免考旗：对端 JS 算好的全厂免考标志，阶段三后置读；gate_enabled=0（免考应用信号、非门控）。

-- 1) 母线组容量列（决策⑤）
alter table vqms_busbar_group
  add column rated_capacity_kw decimal(12,3) default null
  comment '该组额定容量 kW（考核罚款单价基数：调节合格率与投运率缺额罚款共用，0.02 分/万千瓦；厂级口径=各组和；NULL=待现场补录，补录前不产罚款数）【S2/S3 设计稿决策⑤ 2026-08-24 Leo 拍板】';

-- 2) S2 调节合格率：指令级明细
create table vqms_regulation_cmd (
  id                  bigint(20)   not null auto_increment comment '主键',
  stat_date           date         not null                comment '统计归属日（t0 所在日，北京历法）',
  warn_time           varchar(255) not null                comment '指令时间原文（对齐 vqms_command_ledger，忠实摘录）',
  millisecond         varchar(255) default null            comment '毫秒原文（同宽防截断假碰撞，D8 教训）',
  obj_num             bigint(20)   default null            comment '对象编号',
  obj_num_uk          bigint(20)   generated always as (coalesce(obj_num, -1)) stored comment 'uk 归一生成列（NULL 旁路结构性消除，D8 同款）',
  algorithm_id        varchar(16)  not null                comment '判定算法注册 ID（V1_0/STUB；§8.8.5 审计列）【决策④】',
  t_fast_snapshot     int          not null                comment '判定时 t_fast 快照（跨整定期重算可复现、可审计）【关联决策②】',
  fast_state          varchar(16)  not null                comment '快速档最终记账 QUALIFIED/PENALIZED/EXEMPTED/INVALID（FinalTierState）',
  econ_state          varchar(16)  not null                comment '经济档最终记账（同上，两档独立互不隶属）',
  completeness        decimal(5,4) not null                comment '窗口完整度 [0,1]（judge 如实上报原值）',
  invalid_tiers       varchar(16)  default null            comment 'judge 原始按档无效标记 FAST/ECON/FAST,ECON/NULL（成因粒度，区别于 state=INVALID）',
  undecodable_reason  varchar(32)  default null            comment '解码失败归因 CYCLE_CODE_INVALID/MISSING_T0_VOLTAGE/CORRUPTED_ENCODING；NULL=解码成功',
  yx501_sampled       tinyint(1)   default null            comment '免考旗采样值 0/1；NULL=未采样（Undecodable 指令不走免考）',
  disposition         varchar(32)  default null            comment '策略处置桶 COUNT_NORMAL/EXCLUDE_REPORTED/COUNT_UNQUALIFIED/PEND_MARKED（Disposition）；NULL=策略未生效（选套前只记不判）【决策①⑥】',
  fetched_at          datetime     default current_timestamp comment '写入时间',
  primary key (id),
  unique key uk_cmd_result (warn_time, millisecond, obj_num_uk)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 调节合格率指令级明细（判定+免考+策略处置结果）';

-- S2 日汇总
create table vqms_regulation_daily (
  stat_date          date          not null    comment '统计日',
  algorithm_id       varchar(16)   default null comment '周期内算法注册 ID：单一=该 ID / 混合=MIXED【决策④】',
  total_cmds         int           not null default 0 comment '发令总次数=分母（固定分母拍板口径）',
  qualified_fast     int           not null default 0,
  penalized_fast     int           not null default 0,
  exempted_fast      int           not null default 0,
  invalid_fast       int           not null default 0,
  qualified_econ     int           not null default 0,
  penalized_econ     int           not null default 0,
  exempted_econ      int           not null default 0,
  invalid_econ       int           not null default 0,
  undecodable_count  int           not null default 0 comment '解码失败指令数（归因分布看明细表）',
  pended_count       int           not null default 0 comment '丁档挂起标记数【决策①呈现列】',
  excluded_count     int           not null default 0 comment '乙档剔除披露计数——现行拍板不剔分母，此列仅披露；S5 若改分母口径再启用扣减【决策①呈现列】',
  completeness_sum   decimal(10,4) not null default 0 comment '完整度求和（均值=本列/total_cmds 加权还原，绝不存平均率）',
  primary key (stat_date)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 调节合格率日汇总（rollup 只存计数）';

-- S2 月汇总
create table vqms_regulation_monthly (
  stat_month         char(7)       not null    comment '统计月 yyyy-MM',
  algorithm_id       varchar(16)   default null comment '周期内算法注册 ID：单一=该 ID / 混合=MIXED【决策④】',
  total_cmds         int           not null default 0,
  qualified_fast     int           not null default 0,
  penalized_fast     int           not null default 0,
  exempted_fast      int           not null default 0,
  invalid_fast       int           not null default 0,
  qualified_econ     int           not null default 0,
  penalized_econ     int           not null default 0,
  exempted_econ      int           not null default 0,
  invalid_econ       int           not null default 0,
  undecodable_count  int           not null default 0,
  pended_count       int           not null default 0,
  excluded_count     int           not null default 0,
  completeness_sum   decimal(12,4) not null default 0,
  primary key (stat_month)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 调节合格率月汇总（由日表 rollup）';

-- S2 年汇总
create table vqms_regulation_yearly (
  stat_year          smallint      not null    comment '统计年 yyyy',
  algorithm_id       varchar(16)   default null comment '周期内算法注册 ID：单一=该 ID / 混合=MIXED【决策④】',
  total_cmds         int           not null default 0,
  qualified_fast     int           not null default 0,
  penalized_fast     int           not null default 0,
  exempted_fast      int           not null default 0,
  invalid_fast       int           not null default 0,
  qualified_econ     int           not null default 0,
  penalized_econ     int           not null default 0,
  exempted_econ      int           not null default 0,
  invalid_econ       int           not null default 0,
  undecodable_count  int           not null default 0,
  pended_count       int           not null default 0,
  excluded_count     int           not null default 0,
  completeness_sum   decimal(14,4) not null default 0,
  primary key (stat_year)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 调节合格率年汇总（由月表 rollup）';

-- 3) S3 投运率日记账
create table vqms_runtime_daily (
  stat_date          date          not null    comment '统计日',
  in_service_min     int           not null default 0 comment '投运分钟',
  exit_grid_min      int           not null default 0 comment '电网原因退出分钟（免责，出分母）',
  exit_nongrid_min   int           not null default 0 comment '非电网退出分钟（扣罚，在分母）',
  offline_min        int           not null default 0 comment '未并网分钟（不计任何账，透传核对用）',
  rated_capacity_kw  decimal(12,3) default null comment '计算时额定容量快照 kW（来源 vqms_busbar_group.rated_capacity_kw，厂级口径）【开放点⑤】',
  rate_pct           decimal(6,3)  default null comment '投运率快照 %；NULL=零并网分钟（无可考核基数，非真 0%）',
  shortfall_pct      decimal(6,3)  default null comment '缺额百分点快照 max(0,99-率)',
  penalty_score      decimal(12,3) default null comment '考核罚款快照 分（缺额×万千瓦×0.02）',
  primary key (stat_date)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS AVC 投运率日记账';

-- S3 月记账
create table vqms_runtime_monthly (
  stat_month         char(7)       not null    comment '统计月 yyyy-MM',
  in_service_min     int           not null default 0,
  exit_grid_min      int           not null default 0,
  exit_nongrid_min   int           not null default 0,
  offline_min        int           not null default 0,
  rated_capacity_kw  decimal(12,3) default null,
  rate_pct           decimal(6,3)  default null,
  shortfall_pct      decimal(6,3)  default null,
  penalty_score      decimal(12,3) default null,
  primary key (stat_month)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS AVC 投运率月记账（由日表对分钟计数求和后重算率）';

-- S3 年记账
create table vqms_runtime_yearly (
  stat_year          smallint      not null    comment '统计年 yyyy',
  in_service_min     int           not null default 0,
  exit_grid_min      int           not null default 0,
  exit_nongrid_min   int           not null default 0,
  offline_min        int           not null default 0,
  rated_capacity_kw  decimal(12,3) default null,
  rate_pct           decimal(6,3)  default null,
  shortfall_pct      decimal(6,3)  default null,
  penalty_score      decimal(12,3) default null,
  primary key (stat_year)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS AVC 投运率年记账（由月表对分钟计数求和后重算率）';

-- 4) S4 Slice3：Quartz 日任务种子（默认暂停，上线拍板后启用；存量库 sys_job 已有数据，不指定 job_id）
insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select 'VQMS 统计日重算（调节+投运率+rollup）', 'DEFAULT', 'vqmsStatsTask.recomputeYesterday()',
        '0 0 3 * * ?', '3', '1', '1', 'admin', sysdate(),
        '每日 03:00 重算昨日全链；默认暂停——统计上线拍板后由管理员启用（防上线前静默产数）'
where not exists (select 1 from sys_job where invoke_target = 'vqmsStatsTask.recomputeYesterday()');

-- 5) S5 策略参数页菜单（NOT EXISTS 防重，兼容 IT 迁移自应用重复执行）
insert into sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select '策略参数', '2030', '3', 'policyparam', 'vqms/policyparam/index', '', 'VqmsPolicyParam', 1, 0, 'C', '0', '0', 'vqms:policyparam:list', 'checkbox', 'admin', sysdate(), '', null, '数据不可用策略选套页（甲乙丙丁，选套值留空待政策拍板）'
where not exists (select 1 from sys_menu where perms = 'vqms:policyparam:list');
insert into sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select '策略选套应用', (select menu_id from sys_menu where perms = 'vqms:policyparam:list'), '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:policyparam:apply', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where perms = 'vqms:policyparam:apply');
insert into sys_role_menu (role_id, menu_id)
select '2', menu_id from sys_menu where perms in ('vqms:policyparam:list', 'vqms:policyparam:apply')
  and not exists (select 1 from sys_role_menu rm where rm.menu_id = sys_menu.menu_id and rm.role_id = '2');
