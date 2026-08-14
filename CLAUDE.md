# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Address the user as "Leo" — every session, every response, by name.**

## Repository state

**规划 + 前端骨架阶段。** RuoYi 脚手架已就位（`RuoYi-Vue-springboot3/` 后端、`RuoYi-Vue3/` 前端），VQMS 前端业务页面骨架已落地（`RuoYi-Vue3/src/views/vqms/` 下 7 个页面：curve / daily / monthly / yearly / avc-runtime / avc-regulation / threshold）；后端 `ruoyi-vqms` 业务模块尚未落地。后端代码落地后，在此补真实 build/test 命令。

### 权威文档与版本演进链

规划文档按版本迭代，**只看最新一版**。演进链：v1（微服务）→ v2（自研单体/PG）→ v3（RuoYi 底座，主库未定）→ v3.1（RuoYi + MySQL 定案）→ v3.2（辨析国标区间 vs AVC 控制目标）→ v3.3（统计单元/桶，**已撤销**）→ v3.4（撤销桶）→ **v4.0（现行权威：自洽整合 + 算法搁置 + 前端并入）**。v4.0 把 v3.1 底座、v3.4 管理表、草稿口径、前端三阶段整合成一份自洽落地依据。

| 文档 | 位置 | 状态 |
|---|---|---|
| **项目规划_v4_0.md** | `docs/项目规划_v4_0.md` | ✅ **权威**，以此为准（自洽整合版，含算法搁置策略） |
| 项目规划 v3.4 / v3.1 / v3.2 / v3.3 | `docs/` 或 `backup/` | Superseded（v4.0 已整合其内容） |
| 项目规划 v1 / v2 | `backup/` | Superseded（微服务 / 自研单体+PG） |
| AVC考核核心算法_草稿.md | `docs/AVC考核核心算法_草稿.md` | ✅ **算法口径权威**（2026-08-13 Leo 拍板以此为准），含投运率(§一)+调节合格率(§二)+两档平行模型(§2.4–2.7)。草稿自述仍待真实数据定稿，但**判定口径以此为准**。v4.0 §8 把判定实现搁置为接口+stub |
| 外部数据源.md | `docs/外部数据源.md` | 外部库字段语义权威 |
| 核心算法流程图（含通俗版） | `docs/核心算法流程图/` | 辅助理解，以规划文档为准 |
| 政策口径（附件6、分档考核、免考） | `docs/政策口径/` | AVC 考核政策原文 |
| 部署 / 迁移 / 品牌去除计划 | `docs/` | 运维与改造 |
| 外部 DB 表 schema + 样例 | `docs/外部DB/` | 含 `his_curve_sv.md` 等 |

> ⚠️ `docs/` 下另有 `tmp.md`（scratch 笔记，**含明文 DB 凭证**，见 Security）、`数据源头（草稿）.md`、`外部源表优化建议.md` 等——非权威，仅供参考。

## Architecture (v4.0 — RuoYi-Vue base, MySQL decided)

底座自 v3.1 起不变。Built on **若依/RuoYi-Vue**（前后端分离版，Spring Security + JWT + Redis）——不是 v2 自研单体，也不是 `y_project/RuoYi`（Shiro + Thymeleaf 单体）。**4 个 Docker Compose 容器**：`mysql` / `redis` / `backend` / `nginx`。

- **MySQL 8.4** — 唯一持久化主库。存 RuoYi `sys_*` 管理表 + VQMS 管理表（`busbar` 等）+ 派生统计；**绝不存原始业务数据**。（v2 选 PostgreSQL；v3.1 改为 MySQL——RuoYi 是 MySQL 原生，零方言/脚本迁移。）
- **Redis** — RuoYi 必需（登录 token、验证码、限流、字典/配置缓存）。
- **External source** — 只读业务数据（`his_curve_sv` 原始曲线、`yc_history` 遥测/门控、`warn_info` 指令）。当前 MySQL 5.7 @ `10.0.0.9`，按可替换设计。经 RuoYi 多数据源（`@DataSource(SLAVE)`）作只读从库接入。
- **Auth** — RuoYi 的 Spring Security + JWT，原样复用。
- **Backend** = RuoYi 6 个原生模块 **+ 新增 `ruoyi-vqms` 业务模块**：`source/`（外部只读层）、`statistics/`（合格率算法，**判定搁置为接口+stub**）、`ingestion/`（Quartz 预计算作业，**搁置**）。VQMS 控制器位于 `com.ruoyi.web.controller.vqms`。
- 统计模块被查询控制器和 Quartz 作业**进程内直调**（无 Feign、无网关）。
- **端口**（host:container）：mysql `13306:3306`、redis `16379:6379`、backend `7000:7000`、nginx `8080:80`。API 前缀随环境（dev `/dev-api`、prod `/prod-api`；均剥离后转发到 `backend:7000`）——见 v4.0 §10。

## 工程决策：核心算法搁置预留（v4.0 核心）

判定算法是最不稳定的一块（草稿 §2 待真实数据定稿）。v4.0 把工程拆**确定轨 / 搁置轨**：
- **确定轨**（不依赖算法定稿，现在就做）：`source/` 外部只读层、管理表 DDL（`busbar`/`busbar_group`/`yc_point_map`/`busbar_threshold`）、时间对齐工具（就近取整到分钟）、格式校验三步闸门（反制 varchar `save_time`）、RuoYi 脚手架对接（菜单/权限 + 7 个前端页面）、前端三阶段。
- **搁置轨**（等算法定稿）：判定逻辑 → `RegulationJudge` 接口 + `StubRegulationJudge` 占位；调节合格率/投运率落库 DDL；Quartz 预计算编排。
- **关键**：判定抽象为接口（输入=指令+窗口逐分钟 high_SV/low_SV+参数，输出=两档三态结论），算法定型后**只换实现、不动调用方**。详见 v4.0 §8/§12。**没定 DDL 前别硬写统计表**——留空壳接口即可。

## RuoYi reuse vs VQMS build（别重造脚手架）

RuoYi 解决*通用后台管理系统*，**不解决电压质量**。边界要清晰：
- **原样复用**：登录/JWT、用户/角色/菜单/部门/字典/配置、操作/登录日志、Quartz（含 UI）、POI 导出（`@Excel`）、代码生成、Druid 监控、Swagger、多数据源路由、前端外壳（登录/布局/菜单/个人中心）。RuoYi `sys_*` 表与 `sql/ry_*.sql` 初始化脚本直接用——**不重建管理表**。
- **自建**（VQMS 核心价值）：`source/`（外部读取 + 方言切换）、`statistics/`（逐分钟判定 + 日/月/年加权汇总）、`ingestion/`（Quartz 预计算）、前端 `src/views/vqms/` 仪表盘（曲线/日/月/年 + ECharts + 导出）。
- **升级友好**：所有改动限制在 `ruoyi-vqms` 内，**不修改 RuoYi 原生 6 模块**，跟踪上游 RuoYi 发布时合并成本最小。

## Storage-split rule（极易违反——必读）

原始电压曲线**绝不复制进 MySQL 主库**。外部源是原始数据的唯一真相源；MySQL 只存可重算的派生统计 + 管理数据。`source/` 层（在 `ruoyi-vqms` 内）是通往外部库的**唯一读路径**，且设计为只读（防误写）。拿不准数据在哪时：**raw = 外部源；管理 + 统计 = MySQL**。

## External-source DB portability

此处只针对**外部源**（与已定案的 MySQL 主库无关）。外部源类型预计会变（当前 MySQL 5.7），`source/` 层须保持方言无关：
- 经 `HisCurveSvReader` **接口**访问，返回 JDBC 无关的领域对象；每种方言一个实现（当前 `Mysql57CurveReader`）。
- `save_time` 是 `varchar`，日期解析语法因库而异（`STR_TO_DATE` / `strftime` / `to_timestamp`）——把解析留在实现内，或 `SELECT` 原始字符串在 Java 里解析。
- 配置选择：`source.type=mysql57|mysql8|sqlite|postgres` + `source.driver` / `source.url`（`.env`/`sys_config`）。切换 = 改配置 + 换实现 Bean；统计与前端代码不变。
- 只用通用 SQL（`SELECT ... WHERE save_time BETWEEN ? AND ? AND busbar_num = ?`），不用方言专属语法。

## 单位规约（Unit conventions）

VQMS 全项目统一采用以下规范单位，字段、计算、文档、UI 展示均以此为准：

| 量 | 规范单位 | 存储 | 说明 |
|---|---|---|---|
| 电压（`average_SV` / `high_SV` / `low_SV` / `plan_SV` / `nominal_kv` / `tolerance_v`） | **kV** | decimal | 源数据即整数 kV（220kV 母线 `average_SV`≈234）；`tolerance_v` 用 `decimal(10,3)`（1.000 / 1.500） |
| 无功功率 | **kvar** | decimal | 引用 AVC 原文时照录"万千乏"（1 万千乏 = 10000 kvar） |
| 有功 / 容量 | **kW** | decimal | 引用原文时照录"万千瓦" |
| 合格率 / 投运率 | **%** | decimal | 聚合按分钟数加权，**绝不直接平均率列** |
| 时间（聚合） | **分钟数** | int | 各统计粒度记合格 / 不合格 / 剔除分钟计数 |

**单位一致性**：源 SV 字段读入后**不做单位换算**（原值即 kV）。无论最终采用哪种判定模型（见下），参与比较的电压量必须同单位（kV）。`yc_history` 模拟量按 `yc_point_map.unit` 标注，纳入统计前归一到规范单位。

**时区（全项目统一北京时间）**：所有时间一律 **北京时间（UTC+8 / `Asia/Shanghai`）**——与 Leo 沟通的时间表述、文档、日志、统计周期边界（日/月/年起止）、UI 展示、`save_time` 解释均以此为准。代码里日期解析/格式化与 JVM 默认时区按 `Asia/Shanghai`，**不做 UTC 中转再换算**，避免周期边界错位。

## 编码与中文显示规约（全项目统一 UTF-8）

VQMS 含大量中文（母线名、菜单、备注、政策原文、考核结论），全链路统一 **UTF-8**，杜绝乱码：

- **MySQL 字符集**：主库 + 所有表/列 + JDBC 连接用 **`utf8mb4`**（**不用** MySQL 的 `utf8`——那是 3 字节 `utf8mb3`，存 emoji/部分生僻字会失败或截断）；排序规则 MySQL 8.4 用 `utf8mb4_0900_ai_ci`。外部源读取含中文的 varchar（如母线名）同样按 `utf8mb4` 解码。
- **文件编码**：所有源码 / SQL 脚本 / 配置 / 文档存 **UTF-8 无 BOM**；新建文件默认 UTF-8，**不混入 GBK**。
- **导出与传输**：Excel/CSV 导出（RuoYi `@Excel` / POI）的中文文件名与单元格内容、HTTP 响应（`Content-Type; charset=utf-8`）、前端（`<meta charset="utf-8">`，RuoYi 默认）均按 UTF-8。
- **Windows 开发陷阱**：本项目在 Windows 11 开发，编辑器与终端统一 UTF-8；Docker 容器设 `LANG=C.UTF-8`，避免日志/控制台中文乱码。

## AVC 考核规定 —— VQMS 实现依据

VQMS 考核功能以《东北区域电力并网运行管理实施细则》《东北区域电力辅助服务管理实施细则》**附件6「AVC 装置技术指标要求及考核规定」**（东北能源监管局 2024-09-04 印发，p47–48）为政策依据。原文存档于 `docs/政策口径/AVC 装置技术指标要求及考核规定.md`。三个考核维度均为 VQMS 实现目标：

1. **AVC 装置投运率** = 投运时间 / 并网运行时间 × 100%，合格 **≥99%**（扣除电网原因退出时间）。
2. **AVC 装置调节合格率** = 执行合格点数 / 发令次数 × 100%，合格 **≥100%**；调度电压/无功指令下达后，AVC 装置须在 **1 分钟内**调整到合格区间。
3. **免考**：已纳入 AVC 闭环的全部无功设备按最大发/吸能力参与仍不达标 → 该时段免于考核。

**考核单价**（投运率、调节合格率缺额通用）：每缺 1 个百分点 = 额定容量 × **0.02 分/万千瓦**，线性（非分档）。

**调节合格率两档分档**：附件6 §二 的"1 分钟内"为合格线；不合格部分（>1 分钟）按响应时长分两档——**调节快速性考核** [1,5) 分钟、**调节经济性考核** ≥5 分钟。分档阈值（1、5 分钟）均**现场可整定**。命名统一用"性"（非"型"）。详见 `docs/政策口径/调节合格率分档考核.md` 与 `docs/AVC考核核心算法_草稿.md` §2.4–2.7。

**合格区间（电压调整允许偏差）= `tolerance_v` 容差权威值：**

| 电压等级 | 允许偏差 |
|---|---|
| 500 kV | ±1.5 kV (±1500 V) |
| 220 kV | ±1 kV (±1000 V) |
| 66 kV 及以下 | ±1% 额定电压 |

> ✅ `busbar_threshold.tolerance_v` 已据此统一为 kV：原误值 220kV=300 / 500kV=500（int 伏特）已改为 `decimal(10,3)` kV，值 **1.000 / 1.500**。

## Voltage-quality 判定模型 —— 草稿为准（2026-08-13 Leo 拍板）

**判定口径以 `docs/AVC考核核心算法_草稿.md` 为准。** 该草稿定义了**指令级包络判定**，与 v3.4 §1.1 的旧逐分钟均值模型（`|average_SV − plan_SV| ≤ tolerance_v`）不同——草稿已 supersede 旧模型；动手前若发现 v3.4 / `外部数据源.md` / 核心算法流程图仍写旧模型，以草稿为准。

### 指令级包络判定（草稿 §2.1–2.4，权威）

- **判定对象**：一条 AVC 指令（`warn_info` 表 `warn_type=5` 记录，含目标值/增量值），**不是逐分钟**。
- **目标电压 `V_target`（kV）**——从指令解码，**不从 `his_curve_sv.plan_SV` 取**（plan_SV 不参与判定）：
  - 目标值形态：文本数值 `÷100` → kV（如 `22315` → 223.15 kV）；
  - 增量值形态：解码 4 位编码 → 幅值 × 100V × 方向，**+ t₀ 时刻当前实时母线电压** → 绝对目标（如 `2202`=+200V；234.25 + 0.2 → 234.45 kV）。
- **区间聚合（包络并集）**：N 分钟窗口内每分钟一组 `(high_SV_k, low_SV_k)`，聚合为综合区间 `[L_N, H_N] = [min(low_SV_1..N), max(high_SV_1..N)]`——窗口内电压摆动**曾覆盖的最宽范围**。
- **判定**：`V_target ∈ [L_N, H_N]` = 该窗合格。**逐分钟原始判定被取代**——不再是"每分钟单独判均值是否落在带内"。

### 逐分钟字段在判定中的角色

| 字段 | 角色 | 说明 |
|---|---|---|
| `high_SV` / `low_SV` | **判定用**（聚合进包络区间） | 窗口观测极值，取 max/min 构成 `[L,H]` |
| `average_SV` | **废值**（不考察） | Leo 2026-08-14 定：当废值处理，source 层不映射；旧模型用，已彻底弃 |
| `plan_SV` | **废值**（不考察） | 目标来自 `warn_info`；样例值 `10245` 是废值，跳过+记日志，不解码 |

### 两档关系（平行独立，不 fall-through）

快速性档与经济性档是**两个并列的独立考核项，平行考察、互不隶属、不 fall-through**。两档**并行**各判一次（各自综合区间是否夹住 `V_target`）、**各档不合格逐档判免考**（免考结论不跨档）、**各出各的合格率、各算各的罚款，相加得总罚款**。一条指令可有两个独立结论（如「快速性不合格 + 经济性合格」合法）。旧的串联 fall-through 模型（短窗没夹住才进长窗、合成单一合格率）**已废弃**。

| 档 | 扫描窗口 | 综合区间 | 罚的是 |
|---|---|---|---|
| 快速性档 | 第 1 ~ `T_fast` 分钟 | `[L_fast, H_fast]` | 调得慢（动态性能） |
| 经济性档 | 第 `T_fast+1` ~ `T_econ` 分钟 | `[L_econ, H_econ]` | 持续越限（经济代价） |

整定参数（2026-08-14 Leo 定）：`T_fast` ∈ [1,5)（整数 1–4，自由整定，默认建议 4）、`T_econ` = **5**（写死）；指令实际 5 分钟间隔 → 窗口 = 5 分钟、两档不重叠。权威定义见草稿 §2.4–2.7；落地见 `docs/项目规划_v4_0_修订待办.md` A6。

## Source-data 硬约束（来自外部源 schema，每次查询/计算都适用）

这些约束来自外部源表结构，非显而易见，影响每一条查询与计算：

- **`save_time` 是 `varchar(255)` 而非 `datetime`**，带毫秒，无时区。按 `Asia/Shanghai` 解释；读取时校验格式，**坏行跳过 + 记日志**，不让整批失败。
- **`his_curve_sv` 无主键、无索引**。读取须自行去重 + 按 `(save_time, busbar_num)` 排序。
- **双写**：每分钟写一条 busbar `0` + 一条 busbar `1`。所有统计按 `busbar_num` 分组。
- **时间原则：就近取整到分钟（秒 ≥ 30 进位）**。`his_curve_sv` 和 `yc_history`（毫秒精度 `save_time`）的原始时间戳在任何逐分钟聚合前**就近取整到分钟**——秒 ≥ 30 进位，< 30 舍去。这是所有逐分钟判定与汇总的对齐基础；**不要 floor/截断，也不要按原始秒分组**。
- **逐分钟字段语义**：每行 `his_curve_sv` 是一个分钟级采集窗口。`high_SV`/`low_SV` = 窗口观测最大/最小值——**判定用**（聚合进包络区间 `[min(low), max(high)]`，见上「判定模型」）；`average_SV` / `plan_SV` = **废值，不考察**（Leo 2026-08-14 定：当废值处理，source 层不映射、判定/统计不依赖）；旧模型用它们，已彻底弃。
- **Rollup 加权（关键）**：月统计由日表汇总、年由月表，经 MySQL `INSERT...SELECT...GROUP BY...ON DUPLICATE KEY UPDATE` 对**分钟计数**求和（完整 DDL + rollup SQL 见 v3.4 §5.2 / v3.1 §6.3）。**绝不直接平均率列**——重算 `qualification_rate = SUM(qualified_minutes)/SUM(total_minutes)*100` 等；`avg_SV` 按 `total_minutes` 加权。（UPSERT 中 `VALUES(col)` 自 MySQL 8.0.20 起废弃但仍可用；别名写法见 v3.1 §6.3。）

### Source-data 验证注意（非硬约束，影响测试设计）

- 样例数据（10.0.0.9 `qheatavchisdb`，**合成测试库**）**已有窗口摆幅**——2026-08-14 实查：1324 行 100% `high≠low`（222~225 波动，非退化稳态），包络「夹住」分支可测；**越限分支与增量第 2 位循环码仍需生产数据**（合成样本第 2 位全=2、零变化，验证不了语义）。样例导出见 `docs/外部DB/qheatavchisdb_样本导出.md`；生产样本须向对端 AVC 系统要。连接须显式 `utf8mb4`，否则中文变 `?`（已实证）。
- `BUSBAR_VRateParameter` 表**明确排除**，不参与任何计算（见 `docs/外部数据源.md` §3.2）。判定用阈值来自 VQMS 自管表（RuoYi 后台可编辑），其 schema、首批值、变更回算策略**待定**（见下 Open follow-ups）。

## ⚠️ Open follow-ups（待办，非既定事实）

以下为未决事项，**不要当现有约束引用**，动手前找 Leo 确认：

- **✅ 判定口径 + 落库方向已定（2026-08-13 Leo 拍板：以草稿 §2 为准）**：指令级统计，分母=发令次数，两档平行；`V_target` 来自 `warn_info` 解码（不用 `plan_SV`）；判定 = `V_target ∈ [high_SV/low_SV 包络并集]`。v3.4 分钟级 `voltage_quality_*` 表不复用作调节合格率落库。
- **✅ 工程策略：算法搁置预留（v4.0 §8/§12）**：判定实现为 `RegulationJudge` 接口 + `StubRegulationJudge` 占位；调节/投运率落库 DDL + Quartz 编排留空壳。确定轨（source 只读层/管理表/时间工具/闸门/脚手架/前端）照常推进，不被算法推翻。
- **✅ `T_econ` 已定（2026-08-14 Leo）= 5 min**（写死）。现场 AVC 指令实际 5 分钟间隔 → 判定窗口 = 5 分钟，经济性档窗口 [`T_fast`+1, 5]；**「无上限」隐患消除、指令重叠不发生**。落地见 `docs/项目规划_v4_0_修订待办.md` A6。
- **`tolerance_v` 新角色**：判定已改指令级包络，v3.4 `busbar_threshold.tolerance_v` 不再作判定核心，角色待重新定位。表结构可先建（§6.2.4），数值/角色待算法定。
- **草稿本身待定稿**（草稿 §2.8）：目标值/增量解码的位定义（增量第 2 位"循环码"含义）、缺数据策略、退出原因来源——待真实数据验证。（✅ 多条指令时间重叠已定：5 分钟间隔 + 窗口 5 分钟 → 不重叠。）
- **文档同步债**：v3.4 / `docs/外部数据源.md` / 核心算法流程图 仍写旧 `average_SV` + `plan_SV` 模型，与草稿冲突——以草稿为准。
- **500kV 母线数据缺失**：`busbar` / `busbar_threshold` / `yc_point_map` 均待现场补录。
- **门控点未补录**：远方就地总/AVC投退 `yc_num` 未拿到，门控关当前空转。
- **后端落地**：`ruoyi-vqms` 模块、build/test 命令、真实数据验证均未开始（确定轨可立即推进 D1~D6，见 v4.0 §12.1）。

## Security

- `docs/tmp.md`（scratch 笔记）**含明文 DB 凭证**（MySQL root 密码、主机）。仓库公开前：从 git 历史擦除密码并轮换。**不要再往受跟踪文件写新秘密。**
- `.env`（规划中）绝不提交——含外部源连接串、MySQL root 密码（仅初始化用）、应用 DB 账号凭证、Redis 地址、JWT secret。**应用运行时用最小权限账号（如 `vqms_app`）连接，不用 root**——root 仅首次初始化（建库/建表、`CREATE USER` + `GRANT`）；账号拆分见 v4.0 §11。
