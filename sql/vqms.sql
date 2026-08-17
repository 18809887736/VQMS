-- ============================================================
-- VQMS 电压质量监测系统 - 建表脚本（v4.1 对齐版，2026-08-17）
--
-- ⚠️⚠️ 破坏性脚本，严禁对已有数据的环境重复执行 ⚠️⚠️
--   每张表开头都是 DROP TABLE IF EXISTS——重跑会清空重建：
--   busbar / busbar_group / busbar_threshold / yc_point_map / vqms_judge_param 等人工维护的配置表
--   会回到初始种子数据，现场已录入的母线（如 500kV）、调整过的容差/整定参数、补录的点位全部丢失
--   （vqms_command_ledger 为只增流水账，重跑会清空——可从外部源重抓，无人工数据损失）。
--   仅限全新部署首启执行；线上变更表结构请用 ALTER 或增量迁移脚本，勿整脚本重跑。
--
-- 与 RuoYi sys_* 表同库；库名由 docker MYSQL_DATABASE 决定，本脚本不含 CREATE DATABASE/USE
-- 首启执行顺序：00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql（末尾 UPDATE 覆盖 ry 默认值）
--
-- 表结构权威来源：项目规划_v4_1.md（管理表 DDL 一律以 §6.2 为准）
--   §6.2.1 busbar            主母线元数据
--   §6.2.2 busbar_group      母线组（主母线判定单元）
--   §6.2.3 yc_point_map      yc_history 遥测点语义映射
--   §6.2.4 busbar_threshold  阈值（带生效区间；tolerance_v 占位/角色待定）
--   §6.2.5 vqms_judge_param  判定整定参数（t_fast / t_econ / 分档阈值）
--   §6.2.6 vqms_command_ledger  AVC 指令流水账（原始事实只增表，确定轨 D8）
--   字典 vqms_v_grade → 本脚本第六节
--
-- v4.1 对齐变更（2026-08-17，相对 v3.2 版本）：
--   * tolerance_v：int 伏特旧口径（220kV=300 / 500kV=500）→ decimal(10,3) kV 权威值 1.000 / 1.500
--     （v4.1 §6.2.4 / §7.1；旧口径 v3.2 起文档层已作废，本次物理 DDL 跟上）
--   * 删除 v3.x 分钟级统计表 voltage_quality_daily/monthly/yearly、组级空档审计、precompute_cursor：
--     判定已改指令级口径（AVC考核核心算法_草稿4_1 §2），分钟级表不能复用（v4.1 §6.3）；
--     统计表随搁置轨（§12.2 S2/S3/S4）解封后另出 DDL，勿从 v3.x 版本恢复旧表
--   * 新增 vqms_judge_param 判定整定参数表（修订待办 A6 / 确定轨 D7）
--   * busbar.v_grade 注释补 2=66kV及以下(预留)，与字典 vqms_v_grade 对齐
--   * 保留 2026-08-15 第六节 vqms_v_grade 字典（修订待办 A8），节号不动
--   * 2026-08-17 新增 vqms_command_ledger 指令流水账（Leo 拍板 review I1-b：搁置期计数契约的
--     落库目标，存储切分铁律唯一有界例外，见 v4.1 §4/§6.2.6）
-- ============================================================


-- ============================================================
-- 一、管理表（v4.1 §6.2；结构先行、数值后填，管理表不依赖算法定稿）
-- ============================================================

-- 1、母线组 — 主母线判定单元（§6.2.2）
--    命名消歧：本表是 VQMS 自建 busbar_group，与外部库 QHeatAvcRtdb.BUSBAR_GROUP（大写、废表不读）只是同名、毫无关系
drop table if exists busbar_group;
create table busbar_group (
  group_num               bigint(20)   not null                comment '母线组编号',
  group_name              varchar(64)  not null                comment '组名',
  v_grade                 tinyint      not null                comment '电压等级编码，同 busbar.v_grade',
  main_indicator_yc_num   bigint(20)   default null            comment '该组"当前主母线号"指示点，对齐 yc_history.yc_num；未接入前为空',
  default_main_busbar_num bigint(20)   default null            comment '指示点不可用时的兜底主母线号；NULL=不兜底→该组该分钟无主母线',
  max_staleness_minutes   int          not null default 30     comment '指示点陈旧窗口(分钟)',
  remark                  varchar(255) default null            comment '备注',
  create_time             datetime     default current_timestamp,
  update_time             datetime     default current_timestamp on update current_timestamp,
  primary key (group_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 母线组（主母线判定单元）';

-- 初始数据：220kV 组已知；500kV 组占位待补，指示点号现场补录前该组不参与统计
-- 主母线号指示点 4001 = 合成库 points.yaml 体系（tools/avc-data-gen/config/points.yaml）；
-- 原 3008 为早期探查遗留、合成库不存在。真实现场点号到位后改（Leo 2026-08-17 拍板对齐）。
insert into busbar_group (group_num, group_name, v_grade, main_indicator_yc_num, default_main_busbar_num, max_staleness_minutes) values
  (0, '220kV母线组', 1, 4001, 0, 30),
  (1, '500kV母线组', 0, null, null, 30);


-- 2、主母线元数据（§6.2.1）
drop table if exists busbar;
create table busbar (
  busbar_num    bigint(20)    not null                comment '主母线编号，对齐 his_curve_sv.busbar_num',
  busbar_name   varchar(64)   not null                comment '母线名称，如 220kV 正母线',
  v_grade       tinyint       not null                comment '电压等级编码：0=500kV, 1=220kV, 2=66kV及以下(预留)，与字典 vqms_v_grade 严格对齐勿改',
  group_num     bigint(20)    default null            comment '所属母线组（逻辑FK → busbar_group.group_num）',
  nominal_kv    decimal(10,3) not null                comment '标称电压 kV（220.000 / 500.000）',
  status        char(1)       not null default '0'    comment '状态：0=正常, 1=停用',
  create_by     varchar(64)   default ''              comment '创建者',
  create_time   datetime      default current_timestamp comment '创建时间',
  update_by     varchar(64)   default ''              comment '更新者',
  update_time   datetime      default current_timestamp on update current_timestamp comment '更新时间',
  remark        varchar(255)  default null            comment '备注',
  primary key (busbar_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 主母线元数据';

-- 初始数据（220kV 东/西母线——对齐外部 BUSBAR 注册表样本（docs/外部DB/外部数据源.md）；
--   warn_info 告警/状态文本对同一对母线称「正母线/副母线」，两套称呼矛盾，最终以现场确认为准；500kV 待现场补录）
insert into busbar (busbar_num, busbar_name, v_grade, group_num, nominal_kv) values
  (0, '220kV 东母线', 1, 0, 220.000),
  (1, '220kV 西母线', 1, 0, 220.000);


-- 3、阈值（带生效区间，变更不回溯重算，§6.2.4）
--    ⚠️ tolerance_v 占位/角色待定：判定已改指令级包络（v4.1 §8），本列不再作判定核心；
--    表结构先建，数值与角色待算法定稿时重新定位
drop table if exists busbar_threshold;
create table busbar_threshold (
  threshold_id           bigint(20)    not null auto_increment comment '主键',
  busbar_num             bigint(20)    not null                comment '母线编号（逻辑 FK → busbar.busbar_num）',
  criterion_type         varchar(8)    not null default 'AVC'  comment '口径：AVC=控制达标率 / GB=国标±10%',
  tolerance_v            decimal(10,3) default null            comment 'AVC 容差(kV)：220kV=1.000, 500kV=1.500；GB 口径为空。⚠️占位/角色待定，禁用旧 |average_SV−plan_SV|≤tolerance_v 口径',
  plan_sv_invalid_policy varchar(20)   not null default 'SKIP' comment 'plan_SV 废值策略：SKIP/COUNT_UNQUALIFIED/FALLBACK。⚠️旧 plan_SV 模型遗留列：plan_SV 已废值不读、暂无消费方，角色随 tolerance_v 一并待算法定稿重定',
  effective_from         date          not null                comment '生效起始日（含）',
  effective_to           date          default null            comment '生效结束日（含），NULL=至今有效',
  create_by              varchar(64)   default ''              comment '创建者',
  create_time            datetime      default current_timestamp comment '创建时间',
  update_by              varchar(64)   default ''              comment '更新者',
  update_time            datetime      default current_timestamp on update current_timestamp comment '更新时间',
  remark                 varchar(255)  default null            comment '备注',
  primary key (threshold_id),
  key idx_busbar_effective (busbar_num, effective_from, effective_to)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 母线电压合格阈值（带生效区间）';

-- 初始数据（AVC 口径，容差按 v4.1 §7.1 权威值：220kV=1.000 kV）
insert into busbar_threshold (busbar_num, criterion_type, tolerance_v, effective_from) values
  (0, 'AVC', 1.000, '2026-01-01'),
  (1, 'AVC', 1.000, '2026-01-01');


-- 4、yc_history 遥测点语义映射（§6.2.3）
drop table if exists yc_point_map;
create table yc_point_map (
  yc_num         bigint(20)    not null                comment '遥测点编码，对齐 yc_history.yc_num',
  point_name     varchar(64)   not null                comment '语义名称，如 主母线号',
  point_type     varchar(32)   default null            comment 'busbar_id=主母线号 / voltage=电压模拟量 / yx=开关量(配 state_1/0_label)',
  unit           varchar(32)   default null            comment '单位（yc 模拟量）',
  state_1_label  varchar(32)   default null            comment 'yx 点值=1 的语义',
  state_0_label  varchar(32)   default null            comment 'yx 点值=0 的语义',
  gate_enabled   tinyint(1)    not null default 0      comment '该 yx 点是否启用为考核门控：1=启用 / 0=不参与',
  remark         varchar(255)  default null            comment '备注',
  create_time    datetime      default current_timestamp comment '创建时间',
  update_time    datetime      default current_timestamp on update current_timestamp comment '更新时间',
  primary key (yc_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='yc_history 遥测点编码映射';

-- 初始数据：点号对齐合成库 points.yaml 体系（tools/avc-data-gen/config/points.yaml）；
-- 原 3008 为早期探查遗留、合成库不存在，真实现场点号到位后改。
-- 3009（AVC投退）gate_enabled=0 保守默认：防全新部署真实环境误用合成点号开门控；
-- 合成库联调开门控时在测试环境手动置 1（4001 主母线号指示点无门控语义，不受影响）。
insert into yc_point_map (yc_num, point_name, point_type, state_1_label, state_0_label, gate_enabled) values
  (4001, '主母线号', 'busbar_id', null, null, 0),
  (3009, 'AVC投退', 'yx', '投入', '退出', 0);
-- ⚠️ 待现场补录：远方就地总 yx 门控点 yc_num，确认后插入并置 gate_enabled=1：
-- (<yc_num>, '远方就地总', 'yx', '远方', '就地', 1);


-- 5、判定整定参数（§6.2.5，v4.1 新增；RuoYi 代码生成 CRUD → /vqms/judgeParam + Redis 缓存 vqms:judgeParam:{key}）
drop table if exists vqms_judge_param;
create table vqms_judge_param (
  param_id     bigint(20)   not null auto_increment comment '主键',
  param_key    varchar(64)  not null                comment '参数键，如 t_fast / t_econ / tier_threshold_fast',
  param_value  int          not null                comment '参数值（分钟数）',
  name         varchar(64)  not null                comment '参数名称',
  description  varchar(255) default null            comment '说明',
  value_min    int          default null            comment '值域下限（含）',
  value_max    int          default null            comment '值域上限（含）',
  status       char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by    varchar(64)  default ''              comment '创建者',
  create_time  datetime     default current_timestamp comment '创建时间',
  update_by    varchar(64)  default ''              comment '更新者',
  update_time  datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark       varchar(255) default null            comment '备注',
  primary key (param_id),
  unique key uk_param_key (param_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 判定整定参数';

-- 初始值（Leo 2026-08-14 定）：t_fast∈[1,5) 整数自由整定、默认建议 4；t_econ=5 写死锁定；
-- 分档阈值 1/5 为附件6 政策值、锁定。指令实际 5 分钟间隔 → 判定窗口 5 分钟、两档不重叠。
-- 值域/锁定校验在 Service 层（t_fast 须 < t_econ=5；锁定行拒绝修改），改值后 CacheEvict 刷新缓存。
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max) values
  ('t_fast',              4, '快速性档窗口(分钟)',     '快速性档扫描窗口 [1, t_fast]，整数可整定', 1, 4),
  ('t_econ',              5, '经济性档窗口上限(分钟)', '写死=5（指令 5 分钟间隔），锁定不可改',    5, 5),
  ('tier_threshold_fast', 1, '快速性档分档阈值(分钟)', '附件6 政策值，锁定',                       1, 1),
  ('tier_threshold_econ', 5, '经济性档分档阈值(分钟)', '附件6 政策值，锁定',                       5, 5);


-- 6、AVC 指令流水账（§6.2.6，2026-08-17 新增；确定轨 D8）
--    搁置期计数契约（§8.6「原始事实只记不判」）的落库目标：外部源 warn_info 电压指令（warn_type=5）的
--    原始字段只增摘录，不含任何判定/解码结论——undecodable 标志、窗口缺分钟数随搁置轨解封后从本表原文
--    + 外部源曲线重算。存储切分铁律的唯一有界例外：仅此一张原始摘录表、只增、~288 行/天（见 v4.1 §4/§6.2.6）。
drop table if exists vqms_command_ledger;
create table vqms_command_ledger (
  id           bigint(20)    not null auto_increment comment '主键',
  warn_time    varchar(32)   not null                comment '指令时间原文（外部源 warn_info.warn_time，varchar 原样保留，格式校验在读取层）',
  millisecond  varchar(8)    default null            comment '毫秒原文（warn_info.millisecond）',
  warn_type    int           not null                comment '类型；电压指令=5（本账只收指令；全量告警是否入账随 §14-8 退出原因来源定）',
  obj_num      bigint(20)    default null            comment '对象编号（现场整定；非 VQMS 管理表引用，不参与逻辑 FK 校验）',
  warn_content varchar(255)  default null            comment '指令文本原文（目标值/增量值编码在此文本内；解码随搁置轨 judge 实现）',
  fetched_at   datetime      default current_timestamp comment '抓取入库时间',
  primary key (id),
  unique key uk_cmd (warn_time, millisecond, obj_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS AVC 指令流水账（原始事实，只增）';


-- ============================================================
-- 二~四、[已删除 2026-08-17] v3.x 分钟级统计表 —— 随搁置轨解封后另设计（v4.1 §6.3 / §12.2）
-- ============================================================
-- 原 v3.2 版本此处的 voltage_quality_daily / voltage_quality_monthly / voltage_quality_yearly、
-- voltage_quality_group_daily（注释块）、precompute_cursor 已整体删除：
--   * 判定口径已改为指令级（AVC考核核心算法_草稿4_1 §2：分母=发令次数、两档平行），
--     分钟级 / average_SV / plan_SV 口径的统计表不能复用作调节合格率落库（v4.1 §6.3）；
--   * 指令级统计表（指令明细 + 日/月/年 rollup）、投运率时间记账表、预计算游标
--     属搁置轨 S2/S3/S4，待算法定稿后设计并另出 DDL——勿从 v3.x 版本恢复旧表。
-- （第五、六节编号保留不动，CLAUDE.md / v4.1 §6.2.1/§9.3 对"第六节 vqms_v_grade 字典"的指引仍成立。）


-- ============================================================
-- 五、部署定制（覆盖 RuoYi 默认值）
-- ============================================================
-- 关闭登录验证码（VQMS 定制：覆盖 ry_*.sql 的默认 true）
-- 按 CLAUDE.md "不改 RuoYi 原生模块"，不直接改 ry_20260417.sql，而在本脚本末尾覆盖
-- 首启执行顺序 00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql，本 UPDATE 最后跑，覆盖 ry 初始值
UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled';


-- ============================================================
-- 六、VQMS 字典（2026-08-15 修订待办 A8：电压等级维度定位）
--   vqms_v_grade：编码与 busbar.v_grade / busbar_group.v_grade 严格对齐（0=500kV,1=220kV），勿改值
--   2=66kV及以下为预留档：本站暂无该等级母线，现场出现时录入 busbar 行即可启用，模型/字典结构不变
--   delete+insert 按 dict_type 幂等，可重复执行；dict_id/dict_code 走自增不写死
-- ============================================================
delete from sys_dict_data where dict_type = 'vqms_v_grade';
delete from sys_dict_type where dict_type = 'vqms_v_grade';
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
values ('电压等级', 'vqms_v_grade', '0', 'admin', sysdate(), 'VQMS 电压等级（编码对齐 busbar.v_grade：0=500kV,1=220kV,2=66kV及以下预留）');
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
values (1, '500kV',      '0', 'vqms_v_grade', '', 'danger',  'N', '0', 'admin', sysdate(), ''),
       (2, '220kV',      '1', 'vqms_v_grade', '', 'primary', 'N', '0', 'admin', sysdate(), ''),
       (3, '66kV及以下', '2', 'vqms_v_grade', '', 'info',    'N', '0', 'admin', sysdate(), '预留档：现场出现 66kV 母线时启用；容差口径为 ±1% 额定电压，异于固定 kV 档');
