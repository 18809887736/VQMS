-- ============================================================
-- VQMS 管理表 DDL（v5.0 权威口径，从 项目规划_v5_0.md §6.2 提取）
-- 提取日期：2026-08-20
-- 权威来源：docs/项目规划_v5_0.md §6.2（管理表 DDL 一律以此为准；本文件是规划正文的忠实摘录）
--
-- 范围：六张管理表 + 一张指令流水账（§6.2.1 ~ §6.2.6）。
--   vqms_busbar / vqms_busbar_group / vqms_yc_point_map / vqms_busbar_threshold / vqms_judge_param / vqms_command_ledger
-- 不含：RuoYi sys_*（复用 ry_*.sql）、Quartz 表、派生统计表（§6.3 搁置轨待定）、字典 / 关验证码覆盖（见 sql/vqms.sql）。
--
-- 说明：结构先行、初始数值后填；判定口径已改指令级包络（草稿v5_0 §2），管理表字段与判定实现解耦，属确定轨。
--   逻辑 FK 不建物理外键（跨库 + RuoYi 风格），一致性靠应用层写路径存在性校验（§6.2 头部）。
-- ============================================================


-- ============================================================
-- 6.2.1 vqms_busbar（主母线元数据）
-- ============================================================
CREATE TABLE `vqms_busbar` (
  `busbar_num`    bigint(20)    NOT NULL                COMMENT '主母线编号，对齐 his_curve_sv.busbar_num',
  `busbar_name`   varchar(64)   NOT NULL                COMMENT '母线名称，如 220kV 正母线',
  `v_grade`       tinyint       NOT NULL                COMMENT '电压等级编码：0=500kV, 1=220kV, 2=66kV及以下(预留)，与字典 vqms_v_grade 严格对齐勿改',
  `group_num`     bigint(20)    DEFAULT NULL            COMMENT '所属母线组（逻辑FK → vqms_busbar_group.group_num）',
  `nominal_kv`    decimal(10,3) NOT NULL                COMMENT '标称电压 kV（220.000 / 500.000）',
  `status`        char(1)       NOT NULL DEFAULT '0'    COMMENT '状态：0=正常, 1=停用',
  `create_by`     varchar(64)   DEFAULT '',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP,
  `update_by`     varchar(64)   DEFAULT '',
  `update_time`   datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark`        varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`busbar_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS 主母线元数据';

-- 初始数据（220kV 东/西母线——对齐外部 BUSBAR 注册表样本；
--   warn_info 告警/状态文本对同一对母线称「正母线/副母线」，两套称呼矛盾，最终以现场确认为准；500kV 待现场补录）
INSERT INTO `vqms_busbar` (`busbar_num`, `busbar_name`, `v_grade`, `group_num`, `nominal_kv`) VALUES
  (0, '220kV 东母线', 1, 0, 220.000),
  (1, '220kV 西母线', 1, 0, 220.000);


-- ============================================================
-- 6.2.2 vqms_busbar_group（母线组——主母线判定单元）
--   命名消歧：本表是 VQMS 自建 vqms_busbar_group，与外部库 QHeatAvcRtdb.BUSBAR_GROUP（大写、废表不读）只是同名、毫无关系。
-- ============================================================
CREATE TABLE `vqms_busbar_group` (
  `group_num`               bigint(20)   NOT NULL                COMMENT '母线组编号',
  `group_name`              varchar(64)  NOT NULL                COMMENT '组名',
  `v_grade`                 tinyint      NOT NULL                COMMENT '电压等级编码，同 vqms_busbar.v_grade',
  `main_indicator_yc_num`   bigint(20)   DEFAULT NULL            COMMENT '该组"当前主母线号"指示点，对齐 yc_history.yc_num；未接入前为空',
  `default_main_busbar_num` bigint(20)   DEFAULT NULL            COMMENT '指示点不可用时的兜底主母线号；NULL=不兜底→该组该分钟无主母线',
  `max_staleness_minutes`   int          NOT NULL DEFAULT 30     COMMENT '指示点陈旧窗口(分钟)',
  `remark`                  varchar(255) DEFAULT NULL,
  `create_time`             datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_time`             datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS 母线组（主母线判定单元）';

-- 主母线号指示点 4001 = 合成库 points.yaml 体系；原 3008 为早期探查遗留（合成库不存在）。
-- 点号为合成库联调用，真实现场到位后改（Leo 2026-08-17 拍板对齐）；真实候选 yc3（对端 BUSBAR_GROUP.MainBarYcNum=3，2026-08-18 考据）。
INSERT INTO `vqms_busbar_group` (`group_num`, `group_name`, `v_grade`, `main_indicator_yc_num`, `default_main_busbar_num`, `max_staleness_minutes`) VALUES
  (0, '220kV母线组', 1, 4001, 0, 30),
  (1, '500kV母线组', 0, NULL, NULL, 30);  -- 指示点号待现场补录，补录前该组不参与统计


-- ============================================================
-- 6.2.3 vqms_yc_point_map（yc_history 遥测点映射）
-- ============================================================
CREATE TABLE `vqms_yc_point_map` (
  `yc_num`         bigint(20)    NOT NULL                COMMENT '遥测点编码，对齐 yc_history.yc_num',
  `point_name`     varchar(64)   NOT NULL                COMMENT '语义名称，如 主母线号',
  `point_type`     varchar(32)   DEFAULT NULL            COMMENT 'busbar_id=主母线号 / voltage=电压模拟量 / yx=开关量(配 state_1/0_label)',
  `unit`           varchar(32)   DEFAULT NULL            COMMENT '单位（yc 模拟量）',
  `state_1_label`  varchar(32)   DEFAULT NULL            COMMENT 'yx 点值=1 的语义',
  `state_0_label`  varchar(32)   DEFAULT NULL            COMMENT 'yx 点值=0 的语义',
  `gate_enabled`   tinyint(1)    NOT NULL DEFAULT 0      COMMENT '该 yx 点是否启用为考核门控：1=启用 / 0=不参与',
  `remark`         varchar(255)  DEFAULT NULL,
  `create_time`    datetime      DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`yc_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='yc_history 遥测点编码映射';

-- 点号对齐合成库 points.yaml（4001 主母线号 / 3009 AVC投退）；真实现场点号到位后改。
-- 3009 gate_enabled=0 保守默认（防全新部署真实环境误用合成点号开门控）；合成库联调时测试环境手动置 1。
-- 2003 远方就地总（2026-08-18 定号）：对端配置库现成派生点 yx2003 = OR(yx12 正母, yx23 副母)，1=远方/0=就地；
--   gate_enabled=0 保守同 3009，真实现场部署核对后置 1。
INSERT INTO `vqms_yc_point_map` (`yc_num`, `point_name`, `point_type`, `state_1_label`, `state_0_label`, `gate_enabled`) VALUES
  (4001, '主母线号', 'busbar_id', NULL, NULL, 0),
  (3009, 'AVC投退', 'yx', '投入', '退出', 0),
  (2003, '远方就地总', 'yx', '远方', '就地', 0);


-- ============================================================
-- 6.2.4 vqms_busbar_threshold（阈值，带生效区间）
--   ⚠️ 占位/角色待定，禁用旧口径：判定已改草稿 §2 指令级包络（§8）。本表 tolerance_v 在旧 v3.4 |average_SV−plan_SV|≤tolerance_v
--   口径下是判定核心，该旧口径已彻底废弃、严禁再引用。新口径下判定用 warn_info 目标 + 包络区间，不读 plan_SV/tolerance_v；
--   本列角色待算法定稿时重新定位。plan_sv_invalid_policy 同为旧模型遗留列，暂无消费方。
-- ============================================================
CREATE TABLE `vqms_busbar_threshold` (
  `threshold_id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `busbar_num`             bigint(20)    NOT NULL                COMMENT '母线编号（逻辑 FK → vqms_busbar.busbar_num）',
  `criterion_type`         varchar(8)    NOT NULL DEFAULT 'AVC'  COMMENT '口径：AVC=控制达标率 / GB=国标±10%',
  `tolerance_v`            decimal(10,3) DEFAULT NULL            COMMENT 'AVC 容差(kV)：220kV=1.000, 500kV=1.500；GB 口径为空。⚠️占位/角色待定，禁用旧 |average_SV−plan_SV|≤tolerance_v 口径',
  `plan_sv_invalid_policy` varchar(20)   NOT NULL DEFAULT 'SKIP' COMMENT 'plan_SV 废值策略：SKIP/COUNT_UNQUALIFIED/FALLBACK。⚠️旧模型遗留列，暂无消费方',
  `effective_from`         date          NOT NULL                COMMENT '生效起始日（含）',
  `effective_to`           date          DEFAULT NULL            COMMENT '生效结束日（含），NULL=至今有效',
  `create_by`              varchar(64)   DEFAULT '',
  `create_time`            datetime      DEFAULT CURRENT_TIMESTAMP,
  `update_by`              varchar(64)   DEFAULT '',
  `update_time`            datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark`                 varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`threshold_id`),
  KEY `idx_busbar_effective` (`busbar_num`, `effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS 母线电压合格阈值（带生效区间）';

-- AVC 口径，容差权威值：220kV=1.000 kV。
INSERT INTO `vqms_busbar_threshold` (`busbar_num`, `criterion_type`, `tolerance_v`, `effective_from`) VALUES
  (0, 'AVC', 1.000, '2026-01-01'),
  (1, 'AVC', 1.000, '2026-01-01');


-- ============================================================
-- 6.2.5 vqms_judge_param（判定整定参数）
--   自建表（不用 sys_config）：需初始值 + 运行时 CRUD + 改后即时生效 + 值域校验。
--   t_fast ∈ [1,5) 整数自由整定、默认建议 4；t_econ = 5 写死锁定；分档阈值 1/5 为附件6 政策值锁定。
--   判定时读表 + Redis 缓存 vqms:judgeParam:{key}；CRUD 改完 CacheEvict 刷新。
-- ============================================================
CREATE TABLE `vqms_judge_param` (
  `param_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_key`    varchar(64)  NOT NULL                COMMENT '参数键，如 t_fast / t_econ / tier_threshold_fast',
  `param_value`  int          NOT NULL                COMMENT '参数值（分钟数）',
  `name`         varchar(64)  NOT NULL                COMMENT '参数名称',
  `description`  varchar(255) DEFAULT NULL            COMMENT '说明',
  `value_min`    int          DEFAULT NULL            COMMENT '值域下限（含）',
  `value_max`    int          DEFAULT NULL            COMMENT '值域上限（含）',
  `status`       char(1)      NOT NULL DEFAULT '0'    COMMENT '状态：0=正常, 1=停用',
  `create_by`    varchar(64)  DEFAULT '',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_by`    varchar(64)  DEFAULT '',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark`       varchar(255) DEFAULT NULL,
  PRIMARY KEY (`param_id`),
  UNIQUE KEY `uk_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS 判定整定参数';

INSERT INTO `vqms_judge_param` (`param_key`, `param_value`, `name`, `description`, `value_min`, `value_max`) VALUES
  ('t_fast',              4, '快速性档窗口(分钟)',  '快速性档扫描窗口 [1, t_fast]，整数可整定', 1, 4),
  ('t_econ',              5, '经济性档窗口上限(分钟)', '写死=5（指令 5 分钟间隔），锁定不可改', 5, 5),
  ('tier_threshold_fast', 1, '快速性档分档阈值(分钟)', '附件6 政策值，锁定', 1, 1),
  ('tier_threshold_econ', 5, '经济性档分档阈值(分钟)', '附件6 政策值，锁定', 5, 5);


-- ============================================================
-- 6.2.6 vqms_command_ledger（AVC 指令流水账，2026-08-17 新增）
--   搁置期计数契约（§8.6「原始事实只记不判」）的落库目标——warn_info 电压指令（warn_type=5）原始字段只增摘录，
--   不含判定/解码结论。存储切分铁律唯一有界例外：仅此一张原始摘录表、只增不改，~288 行/天。
--   幂等（2026-08-22 D8）：可空键列经生成列 NULL 归一后进 uk（MySQL 唯一键拦不住 NULL 重复），
--   insert ignore 即幂等、无应用层 check-then-insert 竞态。millisecond/millisecond_uk 与源同宽 varchar(255)
--   （防 insert ignore 下超宽脏值静默截断失真——D8 对抗验证吸收）。
-- ============================================================
CREATE TABLE `vqms_command_ledger` (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `warn_time`       varchar(32)  NOT NULL                COMMENT '指令时间原文（对齐外部源 warn_info.warn_time，varchar 原样保留，格式校验在读取层）',
  `millisecond`     varchar(255) DEFAULT NULL            COMMENT '毫秒原文（warn_info.millisecond 原样摘录，与源同宽防截断失真）',
  `warn_type`       int          NOT NULL                COMMENT '类型；电压指令=5（本账只收指令；全量告警是否入账随退出原因来源定）',
  `obj_num`         bigint(20)   DEFAULT NULL            COMMENT '对象编号（现场整定；非 VQMS 管理表引用，不参与逻辑 FK 校验）',
  `warn_content`    varchar(255) DEFAULT NULL            COMMENT '指令文本原文（目标值/增量值编码在此文本内；解码随搁置轨 judge 实现）',
  `fetched_at`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '抓取入库时间',
  `millisecond_uk`  varchar(255) GENERATED ALWAYS AS (coalesce(`millisecond`, '')) STORED COMMENT 'uk 键列：millisecond NULL 归一空串（应用不读写）',
  `obj_num_uk`      bigint(20)   GENERATED ALWAYS AS (coalesce(`obj_num`, -1)) STORED COMMENT 'uk 键列：obj_num NULL 归一 -1（应用不读写）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cmd` (`warn_time`, `millisecond_uk`, `obj_num_uk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS AVC 指令流水账（原始事实，只增）';

-- 无种子数据：流水由 source 层抓取填充（筛 warn_type=5），幂等（uk 冲突跳过）。

-- ============================================================
-- 8.7 vqms_policy_param（数据不可用策略参数表，2026-08-22 D9 骨架）
--   甲/乙/丙/丁 = 同一策略评估纯函数的四组配置；换策略 = 改本表几行、代码不动。
--   ⚠️ 选套值留空（Leo 2026-08-18 拍板）：整表零种子行、不预设处置值。
--   约定键：undecodable_mode / invalid_tier_mode / partial_missing_mode ∈
--     {COUNT_NORMAL, EXCLUDE_REPORTED, COUNT_UNQUALIFIED, PEND_MARKED}；
--     partial_missing_threshold_pct = 整数百分比（EXCLUDE_REPORTED 时必填，乙档建议 50）。
--   无 CHECK 钉值（无政策值可钉）；无 CRUD UI（D9 范围拍板）。
-- ============================================================
CREATE TABLE `vqms_policy_param` (
  `param_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_key`    varchar(64)  NOT NULL                COMMENT '参数键（约定键见节注）',
  `param_value`  varchar(255) DEFAULT NULL            COMMENT '参数值（字符串枚举/整数文本；选套前整表留空）',
  `name`         varchar(64)  NOT NULL                COMMENT '参数名称',
  `description`  varchar(255) DEFAULT NULL            COMMENT '说明',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`param_id`),
  UNIQUE KEY `uk_policy_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VQMS 数据不可用策略参数表（D9 骨架，选套留空）';

-- 无种子数据：选套值留空待政策拍板（写入即生效）。
