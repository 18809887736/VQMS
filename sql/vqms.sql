-- ============================================================
-- VQMS 电压质量监测系统 - 建表脚本 (v3.2)
-- 与 RuoYi sys_* 表同库；库名由 docker MYSQL_DATABASE 决定，本脚本不含 CREATE DATABASE/USE
-- 首启执行顺序：00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql（末尾 UPDATE 覆盖 ry 默认值）
--
-- 表结构权威来源：项目规划_v3_2.md
--   §5.1   busbar              主母线元数据
--   §5.1.1 busbar_group        母线组（主母线判定单元）
--   §5.2   busbar_threshold    阈值（带生效区间，AVC/GB 双口径）
--   §5.4   yc_point_map        yc_history 遥测点语义映射
--   §5.7.1 voltage_quality_daily/monthly/yearly  日/月/年统计（v3.1 §5.2 列 + excluded_minutes）
--   §5.7.5 voltage_quality_group_daily           组级空档审计（M2+ 待补，本脚本暂注释不执行）
--   v3.1 §5.2 precompute_cursor                   预计算游标
--
-- v3.2 变更（相对 v3.1 vqms.sql）：
--   * 新增 busbar / busbar_group / busbar_threshold / yc_point_map（v3.2 §5.1~§5.4）
--   * voltage_quality_daily / monthly / yearly 三表均加 excluded_minutes 列（§5.7.1 对账等式 total + excluded）
--   * 所有 busbar_num 由 smallint 改为 bigint(20)，对齐外部源 his_curve_sv.busbar_num 真实类型
--   * voltage_quality_group_daily（§5.7.5）M2+ 待补，本脚本暂注释不执行
--   * precompute_cursor PK 由 busbar_num 改为 group_num（§5.7.3 rollup 按组推进；游标推进与组内日表 UPSERT 同事务，避免组内母线游标分叉）
-- ============================================================


-- ============================================================
-- 一、母线与阈值管理表（v3.2 §5.1~§5.4；统计跑起来前的 prerequisite）
-- ============================================================

-- 1、母线组 — 主母线判定单元（§5.1.1）
--    命名消歧：本表是 VQMS 自建配置表，与外部库 BUSBAR_GROUP（大写、废表不读，见 §6）只是同名、无关
drop table if exists busbar_group;
create table busbar_group (
  group_num               bigint(20)   not null                comment '母线组编号，参考对齐 QHeatAvcRtdb.BUSBAR.GroupNum（非物理FK；外部 BUSBAR_GROUP 废表不读）',
  group_name              varchar(64)  not null                comment '组名，如 220kV母线组',
  v_grade                 tinyint      not null                comment '电压等级编码，同 busbar.v_grade：0=500kV,1=220kV',
  main_indicator_yc_num   bigint(20)   default null            comment '该组"当前主母线号"指示点，对齐 yc_history.yc_num；未接入前为空',
  default_main_busbar_num bigint(20)   default null            comment '指示点不可用(无记录/超陈旧窗口/非法值)时的兜底主母线号；NULL=不兜底→该组该分钟无主母线(组级空档,不计任何母线 total/excluded,见 §5.7.3 层0)',
  max_staleness_minutes   int          not null default 30     comment '指示点陈旧窗口(分钟)：最近记录距统计时刻超过此值即视为不可用',
  remark                  varchar(255) default null            comment '备注',
  create_time             datetime     default current_timestamp,
  update_time             datetime     default current_timestamp on update current_timestamp,
  primary key (group_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 母线组（主母线判定单元）';

-- 初始数据：220kV 组已知，500kV 组占位待补
insert into busbar_group (group_num, group_name, v_grade, main_indicator_yc_num, default_main_busbar_num, max_staleness_minutes) values
  (0, '220kV母线组', 1, 3008, 0, 30),
  (1, '500kV母线组', 0, null, null, 30);  -- 指示点号待现场补录，补录前该组不参与统计


-- 2、主母线元数据（§5.1）
drop table if exists busbar;
create table busbar (
  busbar_num    bigint(20)    not null                comment '主母线编号，对齐 his_curve_sv.busbar_num',
  busbar_name   varchar(64)   not null                comment '母线名称，如 220kV 东母线',
  v_grade       tinyint       not null                comment '电压等级编码：0=500kV,1=220kV（可扩展）',
  group_num     bigint(20)    default null            comment '所属母线组（逻辑FK → busbar_group.group_num）',
  nominal_kv    decimal(10,3) not null                comment '标称电压 kV（220.000/500.000）；国标 ±10% 区间据此现算',
  status        char(1)       not null default '0'    comment '状态：0=正常,1=停用',
  create_by     varchar(64)   default ''              comment '创建者',
  create_time   datetime      default current_timestamp comment '创建时间',
  update_by     varchar(64)   default ''              comment '更新者',
  update_time   datetime      default current_timestamp on update current_timestamp comment '更新时间',
  remark        varchar(255)  default null            comment '备注',
  primary key (busbar_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 主母线元数据';

-- 初始数据（样例已知两条 220kV 母线；500kV 等待现场补录）
insert into busbar (busbar_num, busbar_name, v_grade, group_num, nominal_kv) values
  (0, '220kV 东母线', 1, 0, 220.000),
  (1, '220kV 西母线', 1, 0, 220.000);


-- 3、阈值（带生效区间，变更不回溯重算，§5.2）
drop table if exists busbar_threshold;
create table busbar_threshold (
  threshold_id           bigint(20)   not null auto_increment comment '主键',
  busbar_num             bigint(20)   not null                comment '母线编号（逻辑FK → busbar.busbar_num）',
  criterion_type         varchar(8)   not null default 'AVC'  comment '口径：AVC=控制达标率(窄区间,用 tolerance_v)/GB=国标±10%(按 busbar.nominal_kv 现算,tolerance_v 留空)',
  tolerance_v            int          default null            comment 'AVC 容差(伏特)：220kV=300,500kV=500；GB 口径为空',
  plan_sv_invalid_policy varchar(20)  not null default 'SKIP' comment 'plan_SV 废值策略：SKIP=剔除不计/COUNT_UNQUALIFIED=按不合格计/FALLBACK=回退标称±容差判一次',
  effective_from         date         not null                comment '生效起始日（含）',
  effective_to           date         default null            comment '生效结束日（含），NULL=至今有效',
  create_by              varchar(64)  default ''              comment '创建者',
  create_time            datetime     default current_timestamp comment '创建时间',
  update_by              varchar(64)  default ''              comment '更新者',
  update_time            datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark                 varchar(255) default null            comment '备注',
  primary key (threshold_id),
  key idx_busbar_effective (busbar_num, effective_from, effective_to)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 母线电压合格阈值（带生效区间）';

-- 初始数据（AVC 口径，容差按 §2.2 默认）
insert into busbar_threshold (busbar_num, criterion_type, tolerance_v, effective_from) values
  (0, 'AVC', 300, '2026-01-01'),
  (1, 'AVC', 300, '2026-01-01');


-- 4、yc_history 遥测点语义映射（§5.4）
drop table if exists yc_point_map;
create table yc_point_map (
  yc_num         bigint(20)    not null                comment '遥测点编码，对齐 yc_history.yc_num',
  point_name     varchar(64)   not null                comment '语义名称，如 主母线号',
  point_type     varchar(32)   default null            comment '分类：busbar_id=主母线号指示点/voltage=电压模拟量/yx=开关量(0/1遥信,配 state_1/0_label)',
  unit           varchar(32)   default null            comment '单位（yc 模拟量，如有）',
  state_1_label  varchar(32)   default null            comment 'yx 点值=1 的语义（如 远方/投入）；yc 模拟量留空',
  state_0_label  varchar(32)   default null            comment 'yx 点值=0 的语义（如 就地/退出）；yc 模拟量留空',
  gate_enabled   tinyint(1)    not null default 0      comment '该 yx 点是否启用为考核门控：1=启用(须 yc_history 真实有数据才生效)/0=不参与',
  remark         varchar(255)  default null            comment '备注',
  create_time    datetime      default current_timestamp comment '创建时间',
  update_time    datetime      default current_timestamp on update current_timestamp comment '更新时间',
  primary key (yc_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='yc_history 遥测点编码映射';

-- 初始数据（真实点位：3008 = 220kV 组主母线号指示点）
insert into yc_point_map (yc_num, point_name, point_type, state_1_label, state_0_label, gate_enabled) values
  (3008, '主母线号', 'busbar_id', null, null, 0);
-- ⚠️ 待现场补录两个 yx 门控点（远方就地总、AVC投退）的 yc_num，确认后插入并置 gate_enabled=1：
-- (<yc_num>, '远方就地总', 'yx', '远方', '就地', 1),
-- (<yc_num>, 'AVC投退',   'yx', '投入', '退出', 1);


-- ============================================================
-- 二、电压质量统计表（v3.1 §5.2 列结构 + v3.2 §5.7.1 excluded_minutes；busbar_num 对齐外部源 bigint(20)）
-- ============================================================

-- 5、日统计（粒度：日 × 母线）
drop table if exists voltage_quality_daily;
create table voltage_quality_daily (
  id                 bigint          not null auto_increment    comment '主键',
  stat_date          date            not null                   comment '统计日期',
  busbar_num         bigint(20)      not null                   comment '母线编号，对齐 his_curve_sv.busbar_num',
  total_minutes      int             default null               comment '参与考核分钟数(=qualified+over_high+over_low)',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限(偏高)分钟数',
  over_low_minutes   int             default null               comment '超下限(偏低)分钟数',
  over_limit_minutes int             default null               comment '超限分钟数(=over_high+over_low)',
  excluded_minutes   int             default null               comment '剔除分钟数(门控命中 §5.4 + SKIP缺值 §5.3)，不计入 total；均属"本在统计窗口内但被剔除"；主母线归属见 §5.7.3 层0',
  qualification_rate decimal(5,2)    default null               comment '合格率(%) = qualified/total',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '当日最高电压',
  min_SV             decimal(10,0)   default null               comment '当日最低电压',
  avg_SV             decimal(12,2)   default null               comment '当日平均电压(按分钟数加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_daily (stat_date, busbar_num),
  key idx_vqd_busbar_date (busbar_num, stat_date)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='电压质量-日统计';

-- 6、月统计（粒度：月 × 母线）
drop table if exists voltage_quality_monthly;
create table voltage_quality_monthly (
  id                 bigint          not null auto_increment    comment '主键',
  stat_year          smallint        not null                   comment '年',
  stat_month         smallint        not null                   comment '月(1-12)',
  busbar_num         bigint(20)      not null                   comment '母线编号，对齐 his_curve_sv.busbar_num',
  total_minutes      int             default null               comment '参与考核分钟数',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限(偏高)分钟数',
  over_low_minutes   int             default null               comment '超下限(偏低)分钟数',
  over_limit_minutes int             default null               comment '超限分钟数',
  excluded_minutes   int             default null               comment '月内剔除分钟数(=SUM daily.excluded_minutes)，不计入 total',
  qualification_rate decimal(5,2)    default null               comment '合格率(%) = qualified/total',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '月内最高电压',
  min_SV             decimal(10,0)   default null               comment '月内最低电压',
  avg_SV             decimal(12,2)   default null               comment '月平均电压(加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_monthly (stat_year, stat_month, busbar_num),
  key idx_vqm_year_busbar (stat_year, busbar_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='电压质量-月统计';

-- 7、年统计（粒度：年 × 母线）
drop table if exists voltage_quality_yearly;
create table voltage_quality_yearly (
  id                 bigint          not null auto_increment    comment '主键',
  stat_year          smallint        not null                   comment '年',
  busbar_num         bigint(20)      not null                   comment '母线编号，对齐 his_curve_sv.busbar_num',
  total_minutes      int             default null               comment '参与考核分钟数',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限(偏高)分钟数',
  over_low_minutes   int             default null               comment '超下限(偏低)分钟数',
  over_limit_minutes int             default null               comment '超限分钟数',
  excluded_minutes   int             default null               comment '年内剔除分钟数(=SUM monthly.excluded_minutes)，不计入 total',
  qualification_rate decimal(5,2)    default null               comment '合格率(%) = qualified/total',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '年内最高电压',
  min_SV             decimal(10,0)   default null               comment '年内最低电压',
  avg_SV             decimal(12,2)   default null               comment '年平均电压(加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_yearly (stat_year, busbar_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='电压质量-年统计';


-- ============================================================
-- 三、预计算游标（按组；§5.7.3 rollup 单元=组 → 游标 PK=group_num，非按母线；v3.1 §5.2 的按母线游标已废弃）
-- ============================================================
drop table if exists precompute_cursor;
create table precompute_cursor (
  group_num  bigint(20) not null                                   comment '母线组编号，对齐 busbar_group.group_num（§5.7.3 rollup 按组推进，非按母线）',
  last_date  date       default null                               comment '该组最后已计算日期（组内全部母线同批完成）',
  updated_at datetime   default current_timestamp on update current_timestamp comment '更新时间',
  primary key (group_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='预计算游标（按组）';
-- 游标推进须与"该组当天日表 UPSERT（组内全部母线）"包在同一事务：整批成功→游标前挪一天；失败→全回滚、游标不动、下次重跑整批。避免按母线记游标时组内两条母线 last_date 分叉的中间态。


-- ============================================================
-- 四、[M2+ 待补·暂不执行] 组级空档审计表（§5.7.5）
-- 纯审计增强，不影响合格率算法；indeterminate_minutes 是唯一不能从 daily 反推的量。
-- M2+ 审计迭代时取消注释启用，届时记得在 monthly/yearly 也考虑是否汇总。
-- ============================================================
-- drop table if exists voltage_quality_group_daily;
-- create table voltage_quality_group_daily (
--   stat_date             date       not null,
--   group_num             bigint(20) not null              comment '→ busbar_group.group_num',
--   indeterminate_minutes int        default null          comment '§5.1.1 判定失效(无主母线)空档分钟数',
--   computed_at           datetime   default current_timestamp comment '本次计算时间',
--   primary key (stat_date, group_num)
-- ) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='电压质量-组级空档审计(M2+)';


-- ============================================================
-- 五、部署定制（覆盖 RuoYi 默认值）
-- ============================================================
-- 关闭登录验证码（VQMS 定制：覆盖 ry_*.sql 的默认 true）
-- 按 CLAUDE.md "不改 RuoYi 原生模块"，不直接改 ry_20260417.sql，而在本脚本末尾覆盖
-- 首启执行顺序 00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql，本 UPDATE 最后跑，覆盖 ry 初始值
UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled';
