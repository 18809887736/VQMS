-- ============================================================
-- VQMS 电压质量监测系统 - 建表脚本（v5.0 对齐版，2026-08-20）
--
-- 注：v4.1 内容已并入 项目规划 v5.0（2026-08-18），判定口径并入 AVC考核核心算法_v1_0。
--   本脚本管理表 DDL 以 v5.0 §6.2 为准，判定口径以 正式v1_0 §2 为准（v4.1 仅为历史合入记录）。
--
-- ⚠️⚠️ 破坏性脚本，严禁对已有数据的环境重复执行 ⚠️⚠️
--   每张表开头都是 DROP TABLE IF EXISTS——重跑会清空重建：
--   vqms_busbar / vqms_busbar_group / vqms_busbar_threshold / vqms_yc_point_map / vqms_judge_param 等人工维护的配置表
--   会回到初始种子数据，现场已录入的母线（如 500kV）、调整过的容差/整定参数、补录的点位全部丢失
--   （vqms_command_ledger 为只增流水账，重跑会清空——可从外部源重抓，无人工数据损失）。
--   仅限全新部署首启执行；线上变更表结构请用 ALTER 或增量迁移脚本，勿整脚本重跑。
--
-- 与 RuoYi sys_* 表同库；库名由 docker MYSQL_DATABASE 决定，本脚本不含 CREATE DATABASE/USE
-- 首启执行顺序（docker-entrypoint-initdb.d 按文件名）：
--   00-create-app-user.sh → quartz.sql → ry_20260417.sql → vqms.sql → vqms_menu.sql
--   （vqms_menu.sql 最后跑，须在 ry 建好 sys_menu 后；vqms.sql 末尾 UPDATE 覆盖 ry 默认值）
--
-- 表结构权威来源：项目规划_v5_0.md（管理表 DDL 一律以 §6.2 为准；v4.1 已并入 v5.0）
--   §6.2.1 vqms_busbar            主母线元数据
--   §6.2.2 vqms_busbar_group      母线组（主母线判定单元）
--   §6.2.3 vqms_yc_point_map      yc_history 遥测点语义映射
--   §6.2.4 vqms_busbar_threshold  阈值（带生效区间；tolerance_v 占位/角色待定）
--   §6.2.5 vqms_judge_param  判定整定参数（t_fast / t_econ / 分档阈值）
--   §6.2.6 vqms_command_ledger  AVC 指令流水账（原始事实只增表，确定轨 D8）
--   §8.7  vqms_policy_param    数据不可用策略参数表（选套留空，确定轨 D9）
--   字典 vqms_v_grade → 本脚本第六节
--
-- 历史变更（相对 v3.2，v4.1 起并入 v5.0）：
--   * tolerance_v：int 伏特旧口径（220kV=300 / 500kV=500）→ decimal(10,3) kV 权威值 1.000 / 1.500
--     （v5.0 §6.2.4 / §7.1；旧口径 v3.2 起文档层已作废，物理 DDL 跟上）
--   * 删除 v3.x 分钟级统计表 voltage_quality_daily/monthly/yearly、组级空档审计、precompute_cursor：
--     判定已改指令级口径（AVC考核核心算法_v1_0 §2），分钟级表不能复用（v5.0 §6.3）；
--     统计表随搁置轨（§12.2 S2/S3/S4）解封后另出 DDL，勿从 v3.x 版本恢复旧表
--   * 新增 vqms_judge_param 判定整定参数表（确定轨 D7）
--   * vqms_busbar.v_grade 注释补 2=66kV及以下(预留)，与字典 vqms_v_grade 对齐
--   * 保留 2026-08-15 第六节 vqms_v_grade 字典，节号不动
--   * 新增 vqms_command_ledger 指令流水账（Leo 拍板：搁置期计数契约的
--     落库目标，存储切分铁律唯一有界例外，见 v4.1 §4/§6.2.6）
--   * 新增 vqms_policy_param 策略参数表（确定轨 D9：选套值留空待政策拍板，
--     甲/乙/丙/丁=同一纯函数四组配置，v5.0 §8.7）
-- ============================================================


-- ============================================================
-- 一、管理表（v4.1 §6.2；结构先行、数值后填，管理表不依赖算法定稿）
-- ============================================================

-- 1、母线组 — 主母线判定单元（§6.2.2）
--    命名消歧：本表是 VQMS 自建 vqms_busbar_group，与外部库 QHeatAvcRtdb.BUSBAR_GROUP（大写、废表不读）只是同名、毫无关系
drop table if exists vqms_busbar_group;
create table vqms_busbar_group (
  group_num               bigint(20)   not null                comment '母线组编号',
  group_name              varchar(64)  not null                comment '组名',
  v_grade                 tinyint      not null                comment '电压等级编码，同 vqms_busbar.v_grade',
  main_indicator_yc_num   bigint(20)   default null            comment '该组"当前主母线号"指示点，对齐 yc_history.yc_num；未接入前为空',
  default_main_busbar_num bigint(20)   default null            comment '指示点不可用时的兜底主母线号；NULL=不兜底→该组该分钟无主母线',
  max_staleness_minutes   int          not null default 30     comment '指示点陈旧窗口(分钟)',
  rated_capacity_kw       decimal(12,3) default null         comment '该组额定容量 kW（考核罚款单价基数：调节合格率与投运率缺额罚款共用，0.02 分/万千瓦；厂级口径=各组和；NULL=待现场补录，补录前不产罚款数）【S2/S3 设计稿决策⑤ 2026-08-24 Leo 拍板】',
  remark                  varchar(255) default null            comment '备注',
  create_time             datetime     default current_timestamp,
  update_time             datetime     default current_timestamp on update current_timestamp,
  primary key (group_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS 母线组（主母线判定单元）';

-- 初始数据：220kV 组已知；500kV 组占位待补，指示点号现场补录前该组不参与统计
-- 主母线号指示点 4001 = 合成库 points.yaml 体系（tools/avc-data-gen/config/points.yaml）；
-- 原 3008 为早期探查遗留、合成库不存在。真实现场点号到位后改（Leo 2026-08-17 拍板对齐）。
insert into vqms_busbar_group (group_num, group_name, v_grade, main_indicator_yc_num, default_main_busbar_num, max_staleness_minutes) values
  (0, '220kV母线组', 1, 4001, 0, 30),
  (1, '500kV母线组', 0, null, null, 30);


-- 2、主母线元数据（§6.2.1）
drop table if exists vqms_busbar;
create table vqms_busbar (
  busbar_num    bigint(20)    not null                comment '主母线编号，对齐 his_curve_sv.busbar_num',
  busbar_name   varchar(64)   not null                comment '母线名称，如 220kV 正母线',
  v_grade       tinyint       not null                comment '电压等级编码：0=500kV, 1=220kV, 2=66kV及以下(预留)，与字典 vqms_v_grade 严格对齐勿改',
  group_num     bigint(20)    default null            comment '所属母线组（逻辑FK → vqms_busbar_group.group_num）',
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
insert into vqms_busbar (busbar_num, busbar_name, v_grade, group_num, nominal_kv) values
  (0, '220kV 东母线', 1, 0, 220.000),
  (1, '220kV 西母线', 1, 0, 220.000);


-- 3、阈值（带生效区间，变更不回溯重算，§6.2.4）
--    ⚠️ tolerance_v 占位/角色待定：判定已改指令级包络（v4.1 §8），本列不再作判定核心；
--    表结构先建，数值与角色待算法定稿时重新定位
drop table if exists vqms_busbar_threshold;
create table vqms_busbar_threshold (
  threshold_id           bigint(20)    not null auto_increment comment '主键',
  busbar_num             bigint(20)    not null                comment '母线编号（逻辑 FK → vqms_busbar.busbar_num）',
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
insert into vqms_busbar_threshold (busbar_num, criterion_type, tolerance_v, effective_from) values
  (0, 'AVC', 1.000, '2026-01-01'),
  (1, 'AVC', 1.000, '2026-01-01');


-- 4、yc_history 遥测点语义映射（§6.2.3）
drop table if exists vqms_yc_point_map;
create table vqms_yc_point_map (
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
-- 真实号考据（2026-08-18，证据链见 docs/数据源头（草稿）.md 定号一览，落地前现场核对）：
--   主母线号 = yc3（对端 BUSBAR_GROUP.MainBarYcNum=3）；实时电压 = yc8 东母 / yc14 西母（BUSBAR.realVYcNum）；
--   总有功 = yc216 + yc316（GENERATOR.pYcNum，VQMS 求和）；AVC投退 = 候选 yx1001（AVC_INFO.AVCStatusYxNum）。
-- ⚠️ 撞号警示：合成占位 3009 在真实库 = 四号机组下闭锁总信号（JS_DATA js109），语义不同——
--   真实现场核对后必须换号，勿把 3009 配到真实环境当 AVC投退。
-- 3009（AVC投退）gate_enabled=0 保守默认：防全新部署真实环境误用合成点号开门控；
-- 合成库联调开门控时在测试环境手动置 1（4001 主母线号指示点无门控语义，不受影响）。
-- 2003（远方就地总）2026-08-18 定号：对端配置库现成派生点 yx2003 = OR(yx12 正母, yx23 副母)，
-- 1=远方/0=就地；warn_info 有 obj_num=2003「远方就地总合/分」事件佐证（docs/外部DB/JS计算引擎说明.md）。
-- gate_enabled=0 保守同 3009：真实现场部署核对后置 1；合成库联调时测试环境手动置 1。
insert into vqms_yc_point_map (yc_num, point_name, point_type, state_1_label, state_0_label, gate_enabled) values
  (4001, '主母线号', 'busbar_id', null, null, 0),
  (3009, 'AVC投退', 'yx', '投入', '退出', 0),
  (2003, '远方就地总', 'yx', '远方', '就地', 0);


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
  unique key uk_param_key (param_key),
  -- 值域/锁定结构性下沉（D7，测试方案 §4.6 两段断言之 DB 段；Service 层另留友好报错）：
  -- 蕴含式（非该键恒真，仅锁定键钉值/值域列）——非白名单：新 param_key 的行可插入（v4.0 A6 扩展路保留）。
  -- ① 行本地值域（min/max 任一为 NULL 则不设限——自定义行允许开放值域，必需键的值域列由 ② 钉死防 NULL 旁路）；
  -- ② 锁定行钉值+值域列（t_econ=5/阈值 1·5 政策值；t_fast 只钉值域列 [1,4] 不钉值——可整定）。
  -- 跨行 t_fast<t_econ 由 ①(t_fast∈[1,4]) + ②(t_econ 钉 5) 传导等价保证（CHECK 行本地写不了跨行）。
  -- ⚠️ 已知边界：CHECK 不拦 DELETE，必需行删除仅 Service 层拦（vqms.sql 注记 2026-08-22 D7 对抗验证吸收）。
  constraint ck_value_range check (value_min is null or value_max is null or param_value between value_min and value_max),
  constraint ck_locked_rows check (
    (param_key <> 't_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 'tier_threshold_fast' or (param_value <=> 1 and value_min <=> 1 and value_max <=> 1))
    and (param_key <> 'tier_threshold_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 't_fast' or (value_min <=> 1 and value_max <=> 4))
  )
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
--    幂等（2026-08-22 D8）：MySQL 唯一键视 NULL 互不相同——millisecond/obj_num 可空列直接进 uk 拦不住
--    NULL 重复行；经生成列把 NULL 归一（'' / -1）后进 uk，重复抓取由 DB 层结构性拦截（insert ignore 即幂等），
--    无需应用层 check-then-insert（无 TOCTOU 竞态）。-1 不与真实对象编号撞（现场编号恒正）。
--    millisecond/millisecond_uk 与源表同宽 varchar(255)（D8 对抗验证吸收）：源列无约束（save_time varchar(255)
--    同类病灶有先例），收窄会在 insert ignore 下把超宽脏值静默截断、截断值再进 uk 致假碰撞丢行——违背
--    「忠实原文摘录」定位；uk 键长 32*4+255*4+8=1156 字节 < 3072 InnoDB 上限，无代价。
drop table if exists vqms_command_ledger;
create table vqms_command_ledger (
  id              bigint(20)    not null auto_increment comment '主键',
  warn_time       varchar(32)   not null                comment '指令时间原文（外部源 warn_info.warn_time，varchar 原样保留，格式校验在读取层）',
  millisecond     varchar(255)  default null            comment '毫秒原文（warn_info.millisecond 原样摘录，与源同宽防截断失真）',
  warn_type       int           not null                comment '类型；电压指令=5（本账只收指令；全量告警是否入账随 §14-8 退出原因来源定）',
  obj_num         bigint(20)    default null            comment '对象编号（现场整定；非 VQMS 管理表引用，不参与逻辑 FK 校验）',
  warn_content    varchar(255)  default null            comment '指令文本原文（目标值/增量值编码在此文本内；解码随搁置轨 judge 实现）',
  fetched_at      datetime      default current_timestamp comment '抓取入库时间',
  millisecond_uk  varchar(255)  generated always as (coalesce(millisecond, '')) stored comment 'uk 键列：millisecond NULL 归一空串（应用不读写）',
  obj_num_uk      bigint(20)    generated always as (coalesce(obj_num, -1)) stored comment 'uk 键列：obj_num NULL 归一 -1（应用不读写）',
  primary key (id),
  unique key uk_cmd (warn_time, millisecond_uk, obj_num_uk)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='VQMS AVC 指令流水账（原始事实，只增）';


-- 7、数据不可用策略参数表（v5.0 §8.7 / §12.1 D9；确定轨骨架，2026-08-22）
--    甲/乙/丙/丁 = 同一策略评估纯函数的四组配置（不写四个类）；换策略 = 改本表几行、代码不动。
--    ⚠️ 选套值留空（Leo 2026-08-18 拍板）：整表零种子行、不预设处置值——政策拍板后写入即生效。
--    约定键（勿改）：undecodable_mode / invalid_tier_mode / partial_missing_mode ∈
--      {COUNT_NORMAL, EXCLUDE_REPORTED, COUNT_UNQUALIFIED, PEND_MARKED}（statistics.Disposition）；
--      partial_missing_threshold_pct = 整数百分比（partial_missing_mode=EXCLUDE_REPORTED 时必填，乙档建议 50）。
--    无 CHECK 钉值（D7 蕴含式教训的反向适用：此刻无政策值可钉，钉枚举白名单会堵死候选演进；
--    选套定稿拍板后再随 UI 一并加约束）。无 CRUD UI（D9 范围拍板），随选套定稿按 D7 同款补。
drop table if exists vqms_policy_param;
create table vqms_policy_param (
  param_id     bigint(20)   not null auto_increment comment '主键',
  param_key    varchar(64)  not null                comment '参数键（约定键见节注）',
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


-- ============================================================
-- 二~四、[已删除 2026-08-17] v3.x 分钟级统计表 —— 随搁置轨解封后另设计（v4.1 §6.3 / §12.2）
-- ============================================================
-- 原 v3.2 版本此处的 voltage_quality_daily / voltage_quality_monthly / voltage_quality_yearly、
-- voltage_quality_group_daily（注释块）、precompute_cursor 已整体删除：
--   * 判定口径已改为指令级（AVC考核核心算法_v1_0 §2：分母=发令次数、两档平行），
--     分钟级 / average_SV / plan_SV 口径的统计表不能复用作调节合格率落库（v4.1 §6.3）；
--   * 指令级统计表（指令明细 + 日/月/年 rollup）、投运率时间记账表、预计算游标
--     属搁置轨 S2/S3/S4，待算法定稿后设计并另出 DDL——勿从 v3.x 版本恢复旧表。
-- （第五、六节编号保留不动，CLAUDE.md / v4.1 §6.2.1/§9.3 对"第六节 vqms_v_grade 字典"的指引仍成立。）
--   ⬆ 指令级统计表与投运率记账表已于 2026-08-24 随 S2/S3 落地——见文末第七（S2）/第八（S3）节。


-- ============================================================
-- 五、部署定制（覆盖 RuoYi 默认值）
-- ============================================================
-- 关闭登录验证码（VQMS 定制：覆盖 ry_*.sql 的默认 true）
-- 按 CLAUDE.md "不改 RuoYi 原生模块"，不直接改 ry_20260417.sql，而在本脚本末尾覆盖
-- 首启执行顺序 00-create-app-user.sh → quartz.sql → ry_*.sql → vqms.sql，本 UPDATE 最后跑，覆盖 ry 初始值
UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled';


-- ============================================================
-- 六、VQMS 字典（2026-08-15 修订待办 A8：电压等级维度定位）
--   vqms_v_grade：编码与 vqms_busbar.v_grade / vqms_busbar_group.v_grade 严格对齐（0=500kV,1=220kV），勿改值
--   2=66kV及以下为预留档：本站暂无该等级母线，现场出现时录入 vqms_busbar 行即可启用，模型/字典结构不变
--   delete+insert 按 dict_type 幂等，可重复执行；dict_id/dict_code 走自增不写死
-- ============================================================
delete from sys_dict_data where dict_type = 'vqms_v_grade';
delete from sys_dict_type where dict_type = 'vqms_v_grade';
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
values ('电压等级', 'vqms_v_grade', '0', 'admin', sysdate(), 'VQMS 电压等级（编码对齐 vqms_busbar.v_grade：0=500kV,1=220kV,2=66kV及以下预留）');
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
values (1, '500kV',      '0', 'vqms_v_grade', '', 'danger',  'N', '0', 'admin', sysdate(), ''),
       (2, '220kV',      '1', 'vqms_v_grade', '', 'primary', 'N', '0', 'admin', sysdate(), ''),
       (3, '66kV及以下', '2', 'vqms_v_grade', '', 'info',    'N', '0', 'admin', sysdate(), '预留档：现场出现 66kV 母线时启用；容差口径为 ±1% 额定电压，异于固定 kV 档');


-- ============================================================
-- 七、调节合格率统计表（指令级，S2 落地 2026-08-24；设计稿四项决策 Leo 当日全批推荐项）
--     口径：正式 v1_0 §2.7——分母=发令总次数（含 undecodable/invalid，固定分母拍板）、两档平行、免考不剔分母。
--     列名与 statistics 包已交付纯函数同构：FinalTierState / Disposition / JudgeParams。
--     rollup 铁律：对计数求和、绝不平均率列；率与罚款由查询层/写回快照按计数重算。
-- ============================================================

-- 7.1 指令级明细（每条入判指令一行；判定+免考+策略处置结果）
drop table if exists vqms_regulation_cmd;
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

-- 7.2~7.4 日/月/年汇总（同构；rollup 对计数求和。率/罚款不落表——查询层或快照按计数重算；
--   两链结构（罚款缺额剔免考与否）属 S5 待拍板，counts 已够两条链各自推导，DDL 不预支结论）
drop table if exists vqms_regulation_daily;
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

drop table if exists vqms_regulation_monthly;
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

drop table if exists vqms_regulation_yearly;
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


-- ============================================================
-- 八、投运率记账表（S3 落地 2026-08-24；正式 v1_0 §1.5 时间记账口径）
--     分钟级分类结果不落主库（量大且可从外部源信号重算，存储切分铁律 §4），只落周期计数；
--     率/缺额/罚款快照由 RuntimeStatistics 纯函数算出写回（单一来源），月/年由合计分钟重算。
-- ============================================================

drop table if exists vqms_runtime_daily;
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

drop table if exists vqms_runtime_monthly;
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

drop table if exists vqms_runtime_yearly;
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
