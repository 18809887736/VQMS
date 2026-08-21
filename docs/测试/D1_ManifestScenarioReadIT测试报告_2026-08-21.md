# D1 场景验证测试报告 — ManifestScenarioReadIT + Mysql57SourceReaderIT

> **日期**: 2026-08-21 12:05
> **基准文档**: `docs/项目规划_v5_0.md` §12.1 D1 / `docs/测试/VQMS_测试方案_v5_0.md` §4.1/§4.2
> **连接目标**: `jdbc:mysql://10.0.0.9:3306/qheatavchisdb`（**真实历史库**，Leo 指令）
> **Manifest 来源**: `tools/avc-data-gen/output/manifest.json`（19 个 S 场景 + 7 个 U 场景）
> **执行环境**: JDK 17.0.20 + Maven 3.9.11 + MyBatis 3.0.5 + JUnit 5.10.2

---

## 一、执行摘要

| 指标 | 数值 |
|---|---|
| 测试类 1 | `Mysql57SourceReaderIT`（基础读通三表） |
| 测试类 2 | `ManifestScenarioReadIT`（场景级原文核对） |
| 用例总数 | **10**（4 + 6） |
| 通过 | **10** |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 执行耗时 | ~15s（含 Maven 启动） |
| **结论** | ✅ **全部通过** |

---

## 二、断言清单与结果

### 测试类 1：Mysql57SourceReaderIT（基础读通）

| 断言 | 方法 | 预期 | 实际 | 结果 |
|---|---|---|---|---|
| 1a | `readCurve_returnsRows_withoutWasteFields()` | his_curve_sv 读到行 + 无废值字段 | 非空 + highSV/lowSV 非 null | ✅ |
| 1b | `readCurve_filtersByBusbar()` | busbar=0 过滤正确 | 仅含 busbarNum=0L | ✅ |
| 1c | `readWarn_returnsInstructionRows()` | warn_type=5 指令存在 | 20 条 | ✅ |
| 1d | `readYc_returnsRows()` | yc_history 有数据 | 非空 | ✅ |

### 测试类 2：ManifestScenarioReadIT（场景级核对）

| 断言 | 方法 | 预期 | 实际 | 结果 |
|---|---|---|---|---|
| 2a | `assert_warnInstructionCount_20()` | 指令总数 = 20 | 20 | ✅ |
| 2b | `assert_S17_twoChannels_splitByObjNum()` | obj_num 含 0 和 1 | {0, 1} | ✅ |
| 2c | `assert_curve_dualWrite_busbar0_and_1()` | 双写分钟 > 0 | 按 `substring(0,16)` 分组后双写分钟 > 0 | ✅ |
| 2d.1 | manifest 核对 - target=223.15 | warn_content 含 "22315" | 存在 | ✅ |
| 2d.2 | manifest 核对 - target=225.0 | warn_content 含 "22500" | 存在 | ✅ |
| 2d.3 | manifest 核对 - 增量 2202 | warn_content 含 "2202" | 存在 | ✅ |
| 2d.4 | manifest 核对 - 增量 1202 | warn_content 含 "1202" | 存在 | ✅ |
| 2d.5 | manifest 核对 - 脏写 abc | warn_content 含 "abc" | 存在 | ✅ |
| 2e | `assert_domainObject_noPlanSVField()` | HisCurveSv 无 setPlanSV 方法 | NoSuchMethodException（符合） | ✅ |
| 2f | `assert_allInstructions_haveNonEmptyContent()` | 20 条指令 warn_content 均非空 | 20/20 | ✅ |

---

## 三、断言边界说明（架构约束）

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

## 四、技术要点记录

### 4.1 save_time 截断到分钟的必要性

`his_curve_sv.save_time` 是 `varchar(255)` 格式如 `2026-03-15 10:00:00.100`（含毫秒），同一分钟的不同毫秒值会导致按完整字符串分组时无法识别双写。断言 2c 改为按 `substring(0, 16)` 提取「yyyy-MM-dd HH:mm」后比对。

### 4.2 SqlSession 生命周期

`ManifestScenarioReadIT` 在 `@BeforeAll` 中持有静态 SqlSessionFactory，各方法复用同一 Session（避免每次关闭导致 `Executor was closed` 异常）。

### 4.3 数据库连接

- **本次**: `qheatavchisdb`（真实库，Leo 指令）
- **凭据**: 环境变量 `VQMS_AVC_TEST_USER` / `VQMS_AVC_TEST_PASSWORD`（默认 root/syth7777）

---

## 五、相关文件

| 文件 | 状态 |
|---|---|
| `ruoyi-vqms/src/main/java/com/ruoyi/vqms/source/` | ✅ D1 source 层实现 |
| `ruoyi-vqms/src/test/java/.../Mysql57SourceReaderIT.java` | ✅ 4/4 通过 |
| `ruoyi-vqms/src/test/java/.../ManifestScenarioReadIT.java` | ✅ 6/6 通过 |
| `CLAUDE.md` | ✅ plan_SV 硬约束已写入 |
| `sql/vqms.sql` | ⏳ D2 目标 |
| `tools/avc-data-gen/manifest.json` | ✅ 已引用 |

---

*报告生成于 2026-08-21，由 Claude Code 自动生成。*
