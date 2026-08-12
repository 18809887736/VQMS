-- ============================================================
-- 新增 JS：正母单元信息 / 副母单元信息（按母线组织）
-- 依据：BUS_GEN_LINK_LOGIC_DEV 的 4 条机组-母线连接关系
--   1号机–正母: yx201(主断路器) AND yx202(正母刀闸)
--   1号机–副母: yx201(主断路器) AND yx203(副母刀闸)
--   2号机–正母: yx301(主断路器) AND yx302(正母刀闸)
--   2号机–副母: yx301(主断路器) AND yx303(副母刀闸)
-- 输出（2 个 yc，按母线，result_type=1）：
--   yc511 = 正母单元信息（母线号固定=0）
--   yc512 = 副母单元信息（母线号固定=1）
-- 编码: 值 = 母线带电(1/0) × 10 + 该母线上并网机组数(0/1/2)
--   正母: 10=仅1号机 ; 20=仅2号机... （见下注） ; 实际用 机号加和
--   ── 修正编码（见下）：用"机组数"更直观 ──
--   值 = 带电(1/0) × 10 + 并网机组数(0/1/2)
--   10 = 1台机 ; 11...  ← 数值不连续，改用下方案
-- ─────────────────────────────────────
-- 最终编码: 值 = 带电(1/0) × 10 + 并网机组数(0/1/2)
--   带电=0:  0 (空母线)
--   带电=1:  11(1台机) / 12(2台机)
-- 解码: 带电 = 值 ÷ 10 ; 机组数 = 值 % 10
-- 算子: gs5=与, gs6=或, gs1=累加, gs2=累乘
-- 变量类型: 0=常量, 2=遥信yx, 4=步骤(引用同 js 内 step_num)
-- ============================================================

-- ---------- JS 220：正母单元信息 → yc511 ----------
INSERT INTO "JS_DATA" VALUES (220, '正母单元信息', 5, 1, 511);

-- step0: 1号机正母连接 = yx201 AND yx202
INSERT INTO "JS_DATA_STEP" VALUES (220, 0, '1号正母连', 5, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 0, 0, 2, 201);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 0, 1, 2, 202);

-- step1: 2号机正母连接 = yx301 AND yx302
INSERT INTO "JS_DATA_STEP" VALUES (220, 1, '2号正母连', 5, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 1, 0, 2, 301);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 1, 1, 2, 302);

-- step2: 正母并网机组数 = step0 + step1  (0/1/2)
INSERT INTO "JS_DATA_STEP" VALUES (220, 2, '正母机数', 1, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 2, 0, 4, 0);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 2, 1, 4, 1);

-- step3: 正母带电 = step0 OR step1  (1/0)
INSERT INTO "JS_DATA_STEP" VALUES (220, 3, '正母带电', 6, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 3, 0, 4, 0);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 3, 1, 4, 1);

-- step4: 带电 × 10
INSERT INTO "JS_DATA_STEP" VALUES (220, 4, '带电x10', 2, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 4, 0, 4, 3);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 4, 1, 0, 10);

-- step5: 编码 = step4 + step2(机组数)  → yc511
INSERT INTO "JS_DATA_STEP" VALUES (220, 5, '正母编码', 1, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 5, 0, 4, 4);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (220, 5, 1, 4, 2);


-- ---------- JS 221：副母单元信息 → yc512 ----------
INSERT INTO "JS_DATA" VALUES (221, '副母单元信息', 5, 1, 512);

-- step0: 1号机副母连接 = yx201 AND yx203
INSERT INTO "JS_DATA_STEP" VALUES (221, 0, '1号副母连', 5, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 0, 0, 2, 201);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 0, 1, 2, 203);

-- step1: 2号机副母连接 = yx301 AND yx303
INSERT INTO "JS_DATA_STEP" VALUES (221, 1, '2号副母连', 5, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 1, 0, 2, 301);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 1, 1, 2, 303);

-- step2: 副母并网机组数 = step0 + step1  (0/1/2)
INSERT INTO "JS_DATA_STEP" VALUES (221, 2, '副母机数', 1, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 2, 0, 4, 0);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 2, 1, 4, 1);

-- step3: 副母带电 = step0 OR step1  (1/0)
INSERT INTO "JS_DATA_STEP" VALUES (221, 3, '副母带电', 6, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 3, 0, 4, 0);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 3, 1, 4, 1);

-- step4: 带电 × 10
INSERT INTO "JS_DATA_STEP" VALUES (221, 4, '带电x10', 2, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 4, 0, 4, 3);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 4, 1, 0, 10);

-- step5: 编码 = step4 + step2(机组数)  → yc512
INSERT INTO "JS_DATA_STEP" VALUES (221, 5, '副母编码', 1, 2);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 5, 0, 4, 4);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (221, 5, 1, 4, 2);
