-- ----------------------------
-- VQMS 电压质量统计表（日 / 月 / 年 + 预计算游标）
-- 与 RuoYi sys_* 表同库；库名由 docker MYSQL_DATABASE 决定，本脚本不含 CREATE DATABASE/USE
-- 字段语义、源数据约束、rollup SQL 见 项目规划_v3_1.md §1.5 / §5.2 / §6
-- ----------------------------

-- ----------------------------
-- 1、电压质量-日统计表（粒度：日 × 母线）
-- ----------------------------
drop table if exists voltage_quality_daily;
create table voltage_quality_daily (
  id                 bigint          not null auto_increment    comment '主键',
  stat_date          date            not null                   comment '统计日期',
  busbar_num         smallint        not null                   comment '母线编号(0/1)',
  total_minutes      int             default null               comment '总分钟数',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限分钟数',
  over_low_minutes   int             default null               comment '超下限分钟数',
  over_limit_minutes int             default null               comment '超限分钟数(超上+超下)',
  qualification_rate decimal(5,2)    default null               comment '合格率(%)',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '当日最高电压',
  min_SV             decimal(10,0)   default null               comment '当日最低电压',
  avg_SV             decimal(12,2)   default null               comment '当日平均电压(按分钟数加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_daily (stat_date, busbar_num),
  key idx_vqd_busbar_date (busbar_num, stat_date)
) engine=innodb comment = '电压质量-日统计';

-- ----------------------------
-- 2、电压质量-月统计表（粒度：月 × 母线）
-- ----------------------------
drop table if exists voltage_quality_monthly;
create table voltage_quality_monthly (
  id                 bigint          not null auto_increment    comment '主键',
  stat_year          smallint        not null                   comment '年',
  stat_month         smallint        not null                   comment '月(1-12)',
  busbar_num         smallint        not null                   comment '母线编号(0/1)',
  total_minutes      int             default null               comment '总分钟数',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限分钟数',
  over_low_minutes   int             default null               comment '超下限分钟数',
  over_limit_minutes int             default null               comment '超限分钟数',
  qualification_rate decimal(5,2)    default null               comment '合格率(%)',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '月内最高电压',
  min_SV             decimal(10,0)   default null               comment '月内最低电压',
  avg_SV             decimal(12,2)   default null               comment '月平均电压(加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_monthly (stat_year, stat_month, busbar_num),
  key idx_vqm_year_busbar (stat_year, busbar_num)
) engine=innodb comment = '电压质量-月统计';

-- ----------------------------
-- 3、电压质量-年统计表（粒度：年 × 母线）
-- ----------------------------
drop table if exists voltage_quality_yearly;
create table voltage_quality_yearly (
  id                 bigint          not null auto_increment    comment '主键',
  stat_year          smallint        not null                   comment '年',
  busbar_num         smallint        not null                   comment '母线编号(0/1)',
  total_minutes      int             default null               comment '总分钟数',
  qualified_minutes  int             default null               comment '合格分钟数',
  over_high_minutes  int             default null               comment '超上限分钟数',
  over_low_minutes   int             default null               comment '超下限分钟数',
  over_limit_minutes int             default null               comment '超限分钟数',
  qualification_rate decimal(5,2)    default null               comment '合格率(%)',
  over_high_rate     decimal(5,2)    default null               comment '超上限率(%)',
  over_low_rate      decimal(5,2)    default null               comment '超下限率(%)',
  max_SV             decimal(10,0)   default null               comment '年内最高电压',
  min_SV             decimal(10,0)   default null               comment '年内最低电压',
  avg_SV             decimal(12,2)   default null               comment '年平均电压(加权)',
  computed_at        datetime        default current_timestamp  comment '本次计算时间',
  primary key (id),
  unique key uk_yearly (stat_year, busbar_num)
) engine=innodb comment = '电压质量-年统计';

-- ----------------------------
-- 4、预计算游标（各母线最后已计算日期，断点续算/全量回填用）
-- ----------------------------
drop table if exists precompute_cursor;
create table precompute_cursor (
  busbar_num smallint not null                                   comment '母线编号(0/1)',
  last_date  date     default null                               comment '最后已计算日期',
  updated_at datetime default current_timestamp on update current_timestamp comment '更新时间',
  primary key (busbar_num)
) engine=innodb comment = '预计算游标';

-- 关闭登录验证码（VQMS 定制：覆盖 ry_*.sql 的默认 true）
-- 按 CLAUDE.md "不改 RuoYi 原生模块"，不直接改 ry_20260417.sql，而在本脚本末尾覆盖
-- 首启执行顺序 00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql，本 UPDATE 最后跑，覆盖 ry 初始值
UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled';
