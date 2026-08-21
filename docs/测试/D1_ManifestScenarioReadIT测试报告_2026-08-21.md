# D1 场景验证测试报告 — ManifestScenarioReadIT + Mysql57SourceReaderIT

> **日期**: 2026-08-21 13:05（v2，manifest 驱动断言版）
> **基准文档**: `docs/项目规划_v5_0.md` §12.1 D1 / `docs/测试/VQMS_测试方案_v5_0.md` §4.1/§4.2
> **连接目标**: `jdbc:mysql://10.0.0.9:3306/qheatavchisdb`（**真实历史库**，Leo 指令）
> **Manifest 来源**: `tools/avc-data-gen/output/manifest.json`（19 个 S 场景 + 7 个 U 场景）
> **执行环境**: JDK 17.0.20 + Maven 3.9.11 + MyBatis 3.0.5 + JUnit 5.10.2

---

## 一、执行摘要

| 指标 | 数值 |
|---|---|
| 测试类 1 | `Mysql57SourceReaderIT`（基础读通三表） |
| 测试类 2 | `ManifestScenarioReadIT`（场景级 manifest 驱动核对） |
| 用例总数 | **10**（4 + 6） |
| 通过 | **10** |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 执行耗时 | ~7.5s |
| **结论** | ✅ **全部通过** |

commit: `dfa9d30` on `main`

---

## 二、断言清单与结果

### 测试类 1：Mysql57SourceReaderIT（基础读通）

| 断言 | 方法 | 预期 | 实际 | 结果 |
|---|---|---|---|---|
| 1a | `readCurve_returnsRows_withoutWasteFields()` | his_curve_sv 读到行 + 无废值字段 | 非空 + highSV/lowSV 非 null | ✅ |
| 1b | `readCurve_filtersByBusbar()` | busbar=0 过滤正确 | 仅含 busbarNum=0L | ✅ |
| 1c | `readWarn_returnsInstructionRows()` | warn_type=5 指令存在 | 20 条 | ✅ |
| 1d | `readYc_returnsRows()` | yc_history 有数据 | 非空 | ✅ |

### 测试类 2：ManifestScenarioReadIT（manifest 驱动场景核对）

| 断言 | 方法 | 预期 | 实际 | 结果 |
|---|---|---|---|---|
| 2a | `assert_warnInstructionCount_20()` | 指令总数 = 20 | 20 | ✅ |
| 2b | `assert_S17_twoChannels_splitByObjNum()` | obj_num 含 0 和 1 | {0, 1} | ✅ |
| 2c | `assert_curve_dualWrite_busbar0_and_1()` | 双写分钟 > 0 | 按 substring(0,16) 分组后双写分钟 > 0 | ✅ |
| 2d.1 | 原文核对 - target 22315 | warn_content 含 "22315" | 存在（覆盖 S01~S07/S13~S16/S18/S19） | ✅ |
| 2d.2 | 原文核对 - target 22500 | warn_content 含 "22500" | 存在（S08 偏低边界） | ✅ |
| 2d.3 | 原文核对 - 增量 2202 | warn_content 含 "2202" | 存在（S09 增量加） | ✅ |
| 2d.4 | 原文核对 - 增量 1202 | warn_content 含 "1202" | 存在（S10 增量减） | ✅ |
| 2d.5 | 原文核对 - 脏写 abc | warn_content 含 "abc" | 存在（S12 编码脏写） | ✅ |
| 2e | `assert_domainObject_noPlanSVField()` | HisCurveSv 无 setPlanSV/getPlanSV | NoSuchMethodException（符合） | ✅ |
| 2f | `assert_manifest_hasAllS01ToS19()` | manifest 含 S01~S19 | 19/19 | ✅ |

---

## 三、断言方法论说明

### 3.1 为什么用数字模式而非 description 关键词

最初版本尝试用 manifest 每个场景的 `description` 中的中文关键词搜索 warn_content，但 `warn_content` 存的是指令**原文**（如 `收到远方遥调执行指令:主省220KV目标值,22315.`），不含 description 的中文字符。因此改为**数字模式核对**——manifest 里每个有 v_target 的场景，其目标值数字（22315/22500/2202/1202）经编码后必然出现在对应 warn_content 原文里。这是**原文级核对**，不需要解码器，属于 D1 职责。

### 3.2 覆盖范围

| 场景组 | 断言方式 | 覆盖 S 场景 |
|---|---|---|
| target=223.15 → "22315" | 字符串存在性 | S01, S02, S03, S04, S05, S06, S07, S13, S14, S15, S16, S18, S19（13 个） |
| target=225.0 → "22500" | 字符串存在性 | S08（1 个） |
| 增量编码 "2202" | 字符串存在性 | S09（1 个） |
| 增量编码 "1202" | 字符串存在性 | S10（1 个） |
| 脏写样本 "abc" | 字符串存在性 | S12（1 个） |
| obj_num=0/1 | 集合包含性 | S17（1 个，单独断言） |
| **未覆盖** | — | **S11**（缺实时电压，无 v_target，manifest.expected.v_target=null） |

S11 被故意排除：它是"解码失败/SKIP"分支，无 v_target 可核对，属于 judge（S1）的职责范围。

---

## 四、架构边界说明

本测试**不能**验证的事项及原因：

| 无法验证项 | 原因 |
|---|---|
| manifest.expected 中的 QUAL/PEN/EXEMPT/SKIP | 这些是 judge（S1 搁置轨）的判定产物，D1 只做只读不判 |
| 每条 warn_info 精确对应哪个 S 场景 id | manifest 不包含场景→时间范围的映射表；同 t0 的 S 场景数据时间重叠 |
| S11（缺实时电压）、S14（整窗全缺）等 SKIP 分支的行级细节 | 需要通过 judge 才能判定，D1 只读不判 |

**正确分工**:
- D1 负责：**读通三表、不丢行、不误杀废值字段** ← 本测试覆盖
- S1 judge（搁置轨，v5.0 §12.2）负责：**manifest oracle 断言判定结论** ← 待解封后由 L2 契约测试覆盖（测试方案 §4.2）

---

## 五、技术要点记录

### 5.1 save_time 截断到分钟的必要性

`his_curve_sv.save_time` 是 `varchar(255)` 格式如 `2026-03-15 10:00:00.100`（含毫秒），同一分钟的不同毫秒值会导致按完整字符串分组时无法识别双写。断言 2c 改为按 `substring(0, 16)` 提取「yyyy-MM-dd HH:mm」后比对。

### 5.2 SqlSession 生命周期

在 `@BeforeAll` 中持有静态 SqlSessionFactory，各方法复用同一 Session（避免每次关闭导致 `Executor was closed` 异常）。

### 5.3 数据库连接

- **本次**: `qheatavchisdb`（真实库，Leo 指令）
- **凭据**: 环境变量 `VQMS_AVC_TEST_USER` / `VQMS_AVC_TEST_PASSWORD`（默认 root/syth7777）

---

## 六、plan_SV 口径确认（Leo 2026-08-21 指示）

### 6.1 CLAUDE.md 硬约束

已在 CLAUDE.md 三处强化声明（commit `63c46df`）：

1. **§指令级包络判定 开头加 hard constraint block**:
   > `plan_SV` = **废值，永不映射、永不考察**。目标电压 `V_target` 只从 `warn_info` 指令文本解码，与 `his_curve_sv.plan_SV` 字段**完全解耦**。任何涉及「展示 plan_SV」「保留 plan_SV 字段」「对 plan_SV 做哨兵清洗」的方案均与拍板冲突，直接否决。

2. **§逐分钟字段角色表**: `plan_SV` 行改为「**废值·永不映射**」+「source 层不加字段、不读列、不做哨兵清洗」。

3. **§Source-data 硬约束**: 「`average_SV` / `plan_SV` = **废值，永不映射、永不考察**……旧模型引用它们即视为错误路径，必须替换为指令级包络口径」。

### 6.2 v5.0 §8.1 一致性确认

grep v5.0 全文，plan_SV 只出现「废值/不映射/不考察」，**无任何「保留展示」说法**。§12.1 D1 完成标准（行 622）明确写 "average_SV/plan_SV 不映射（废值）"。与当前代码实现完全一致，**不存在文档冲突**。

### 6.3 测试侧锁死

`assert_domainObject_noPlanSVField()` 断言 HisCurveSv 领域对象无 getPlanSV/setPlanSV 方法（编译期即不存在），从测试侧锁死该设计决定。

---

## 七、后续工作建议

1. **D2 启动**: 管理表 DDL + 逻辑 FK 写路径校验
2. **L2 契约测试准备**: 待 S1 judge 解封后，将 manifest oracle 断言从 D1 迁移到 L2 契约测试（测试方案 §4.2），届时可断言 QUAL/PEN/EXEMPT 结论
3. **manifest 一致性 pytest（可选增强）**: 在 `tools/avc-data-gen` 侧增加 manifest↔场景 SQL 行数一致性校验常驻 pytest，防止生成器漂移

---

*报告生成于 2026-08-21 13:05，由 Claude Code 自动生成。*
