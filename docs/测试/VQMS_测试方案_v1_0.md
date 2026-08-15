# VQMS 测试方案 v1.0

> 基准：`docs/项目规划_v4_0.md`（2026-08-13）。落地节奏对齐 v4.0 §12 的**确定轨 D1–D6 / 搁置轨 S1–S4**——测试方案分两条轨同步推进，不在算法定稿前铺开无法验证的测试，也不因算法反复变更而反复重写测试。
> 创建日期：2026-08-15。

---

## 0. 现状盘点（先说清楚现在测什么）

对仓库现状的核查结论：

- **后端**：`RuoYi-Vue-springboot3` 仍是原生脚手架，v4.0 规划的 `ruoyi-vqms` 业务模块（`source/` / `statistics/` / `ingestion/`）**尚未落地任何 Java 代码**，仓库里零 VQMS 相关的 Java 测试（只有若依自带的 `TestController`）。
- **前端**：`RuoYi-Vue3/src/views/vqms/` 7 个页面骨架 + `src/api/vqms/*.js` 接口封装已建好，处于 v4.0 §9 的"阶段 3"（接后端真实数据，不用 mock）。
- **已有测试资产（这是本方案最大的杠杆）**：`tools/avc-data-gen`——
  - 26 个覆盖全部判定分支的合成场景（S01–S19 调节合格率、U01–U07 投运率），已有 `manifest.json` 作为**算法测试 oracle**；
  - 已有 `tests/test_l0_units.py`（Python 单测）；
  - 已有 `verify/run_verify.py`——只读连真实 `qheatavchisdb`，验证 `save_time` 解析/去重/排序/亚秒取整（"路 A"）；
  - 已有隔离库 `vqms_avc_test`（10.0.0.9 mysql57 容器内，与真实库、主库物理隔离）。

**结论**：现在没有业务代码可测，测试方案的价值在于——① 把 `avc-data-gen` 的 26 场景 + manifest 确立为后端唯一权威测试数据源；② 按 D1–D6 的实现顺序把测试铺上去，而不是憋一份憋不出来的"全量测试用例表"；③ 给 S1–S4（判定算法未定稿）设计一套"契约先行"的策略，使算法定稿后测试代码零改动。

---

## 1. 测试分层与总体策略

```
L0  纯函数单元测试     时间对齐 / decode.py 解码 / 格式闸门
L1  组件·Repository集成 source 层读取 / 管理表 CRUD（Testcontainers）
L2  契约测试           RegulationJudge 接口：Stub 实现与未来真实实现共用同一批断言
L3  端到端 / 前端测试   登录→选母线→曲线→日/月/年报表（真实后端，不 mock）
L4  数据一致性/回归     Java source 层结果 vs Python 路 A 验证脚本结果 vs manifest.json
L5  部署/环境验证       docker-compose 四容器起停、多数据源路由、方言切换
```

层次越靠下越先能测（不依赖算法定稿），越靠上越依赖实现进度。测试铺开顺序 = D1→D5→D6→（算法定稿后）S1→S2→S3→S4。

---

## 2. 分模块测试设计

### 2.1 D1 · `source/` 外部只读层

- **单测**：`Mysql57CurveReaderTest` 用 Testcontainers 起一次性 `mysql:5.7` 容器，灌入 `avc-data-gen` 生成的 `00-schema.sql` + 26 场景 SQL，断言读接口返回的领域对象字段、条数、排序与 `manifest.json` 一致。
- **容错边界**：`save_time` 是无索引 varchar，脏数据不可避免——单测覆盖"单行不可解析时跳过该行 + 记录 WARN，不中断整批读取"，不是简单抛异常。
- **方言 contract 测试**：定义 `HisCurveSvReaderContractTest` 抽象基类，`Mysql57CurveReader` 现在跑一遍；未来新增 `Sqlite/Mysql8` 实现时复用同一批用例，保证行为一致而不是各写各的。
- **真实库回归（非 CI）**：定期人工/定时跑 `verify/run_verify.py`（路 A，白名单 SELECT-only），并新增 Java 侧对同一批真实数据的读取结果做交叉比对，防止 Java 实现和 Python 验证脚本行为漂移。

### 2.2 D3 · 时间对齐工具

- 纯函数单测，边界值：`29.999s` 舍 / `30.000s` 进 / `30.5s` 进 / 跨分钟 / 跨小时 / 跨天。
- 场景 **S18**（29 舍）、**S19**（30 进）已经是现成的已知期望用例，直接复用，不用另造。

### 2.3 D4 · 格式校验三步闸门

- 反例注入：`null`、空串、非法字符、超范围日期，逐一验证"放宽边界 → 正则校验 → 解析后精确过滤"三步各自的拦截行为。
- 正例：26 场景数据全量应无损通过闸门，用 `manifest.json` 里的条数核对无异常丢弃。

### 2.4 D2 / D5 · 管理表 DDL + RuoYi 脚手架 CRUD

- `busbar` / `busbar_group` / `yc_point_map` / `busbar_threshold` 的 Repository 层用 Testcontainers `mysql:8.4` 跑 DDL + CRUD，结构现在就能测（数值待现场确认，不影响结构测试）。
- 权限/菜单/字典联动沿用 RuoYi 标准测试套路，不必自造——这块是"原样复用"，测试成本应压到最低。

### 2.5 D6 · 前端阶段 1–3

- 组件级：Vitest + `@vue/test-utils`，覆盖 7 个页面里表格/筛选组件的 props、事件、空数据态。
- 端到端：Playwright 覆盖关键路径（登录 → 选母线 → 看曲线 → 看日/月/年报表），**后端接真实 API，不 mock**——这与 v4.0 §9 阶段 3 的原则一致，测试和实现用同一份数据契约。

### 2.6 S1–S4 搁置轨（判定算法 / 落库 / Quartz 编排）

这是本方案的关键设计点：**算法草稿（`docs/AVC考核核心算法_草稿.md`）未定稿，反复写测试会被反复推翻**。对策——

- **现在**：针对 `RegulationJudge` 接口写参数化契约测试（26 场景 × 期望结论，来自 `manifest.json`），先跑在 `StubRegulationJudge` 上——即便结论都是占位值，也要先确认"数据从 `source` 层读入 → 判定接口调用 → 结果输出"整条链路能跑通、字段对得上。
- **算法定稿后**：同一批参数化测试换 Bean 指向真实 `RegulationJudgeImpl`，直接断言 `manifest.json` 的期望结论（QUAL/PEN/EXEMPT/SKIP）——**测试代码零改动，只换实现**，这是把"确定轨代码零改动"的原则复用到测试上。
- 落库 DDL（S2/S3）和 Quartz 编排（S4）**暂不写集成测试**，等 DDL 定稿后再补，避免在表结构都没定的情况下反复重写测试脚手架。

---

## 3. 测试数据策略

- **主力数据源**：`tools/avc-data-gen` 的 26 个场景（S01–S19 + U01–U07）+ `manifest.json`——这是仓库里唯一覆盖全部判定分支、且有已知期望结论的数据集。真实 dump（`qheatavchisdb`）已知三处缺口（`his_curve_sv` 退化稳态、`warn_info` 无 warn_type=5、`yc_history` 0 条），**无法用于验证判定分支**，因此后端所有集成/契约测试都应基于合成数据，而不是手造或直连真实库。
- **CI 内隔离**：CI 里用 Testcontainers 现起一次性 `mysql:5.7` 容器 + `avc-data-gen` 生成的 SQL 灌入，**不依赖 10.0.0.9 常驻的 `vqms_avc_test` 库**，保证 CI 可重复、不受现场环境影响。`vqms_avc_test` 继续作为人工验收/联调环境使用。
- **真实数据**（`_qheatavchisdb`）：仅用于人工验收阶段的路 A 只读验证，不进 CI——数据量大、未脱敏、且已知无法覆盖判定分支。
- **一致性校验**：新增一条 Python 测试（`avc-data-gen` 内），校验 26 场景生成的 SQL 与 `manifest.json` 保持同步（场景增删/期望结论修改时不会漏更新）。

---

## 4. 工具链

| 层 | 工具 |
|---|---|
| Java 单元/集成 | JUnit5 + Spring Boot Test + Testcontainers（`mysql:5.7` 模拟外部源、`mysql:8.4` 管理库）+ Mockito + AssertJ |
| Python（`avc-data-gen` 自身） | 沿用现有 pytest（`tests/test_l0_units.py`），新增 manifest 一致性测试 |
| 前端单元/组件 | Vitest + `@vue/test-utils` |
| 前端端到端 | Playwright，跑关键路径，接真实 `dev-api` |
| CI | GitHub Actions，Java / Node / Python 三个 job；Java job 需要 Docker-in-Docker 以跑 Testcontainers |

---

## 5. 分阶段里程碑（与 D1–D6 / S1–S4 对齐）

| 阶段 | 交付 | 对应测试铺开 |
|---|---|---|
| 1 | D1 `source/` 只读层落地 | L0（时间对齐/闸门）+ L1（Testcontainers 读接口）+ L4（路 A 交叉比对，非 CI） |
| 2 | D2/D5 管理表 + RuoYi CRUD 空壳 | L1（Repository 层） |
| 3 | D6 前端阶段 1–3 | 前端组件测试 + 关键路径 E2E |
| 4 | 算法草稿定稿 | S1 契约测试从 Stub 切到真实实现（L2），26 场景全量断言通过为准入门槛 |
| 5 | S2/S3 落库 DDL 定稿 | 补落库层集成测试 |
| 6 | S4 Quartz 编排 | 编排层集成测试 + 端到端定时任务验证 |

---

## 6. 风险与对策

| 风险 | 对策 |
|---|---|
| 算法未定稿导致测试反复推翻 | 2.6 节的"契约先行 + Stub 占位"策略，只换 Bean 不改测试 |
| 生产库 10.0.0.9 不可作 CI 依赖 | Testcontainers 现起临时库 + `avc-data-gen` 灌数据，CI 与现场环境解耦 |
| `save_time` varchar 无索引、脏数据 | D4 三步闸门测试 + source 层"跳过并记录"容错单测 |
| 26 场景数据与 `manifest.json` 走漂 | 新增一致性校验测试（3 节末） |
| 真实库三处已知缺口误导判定分支验证 | 判定分支验证只信 `avc-data-gen` 合成数据，真实库仅做只读读取层验证 |

---

## 7. 验收标准（Definition of Done）示例

- **L0**：所有边界值（含 S18/S19）通过，无遗漏分支。
- **L1**：Testcontainers 起库 + 灌入 26 场景后，读接口返回条数、字段与 `manifest.json` 描述一致。
- **L2**（算法定稿后）：`RegulationJudgeImpl` 在 26 场景全量断言下与 `manifest.json` 期望结论一致，方可视为算法实现达标。
- **L3**：关键路径 Playwright 用例在真实后端（非 mock）下稳定通过。
- **L4**：Java 读取结果与路 A 验证脚本结果交叉比对无差异。
- **L5**：`docker-compose up` 四容器（mysql/redis/backend/nginx）健康检查全绿，多数据源路由（`SLAVE` 指向外部源）连通性验证通过。
