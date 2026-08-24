# S4 测试报告 — Slice 3：Quartz 接线 + 三级 rollup（S4 收官）

- **日期**：2026-08-24
- **口径基准**：§12.2 S4（第一版按日期区间幂等重算、不建增量游标）；RuoYi Quartz 约定；决策④ MIXED
- **层级**：L0 任务委托 mock 测试 + L1 IT 扩展（mapper 权威 SQL 级联断言）
- **结果**：✅ 新增 4 用例全绿 + IT 扩展 1 例；全模块回归 212/212（vqms 208 + admin 4）。**S4 全部切片交付完毕**

## 一、交付物

| 项 | 内容 |
|---|---|
| `ingestion/VqmsStatsTask`（bean 名 `vqmsStatsTask`） | RuoYi Quartz 入口，纯委托无算术：`recomputeYesterday()` 每日无参全链（昨日调节明细 + 投运率日账 + 五级 rollup）；`recomputeRange(start,end)` 手动/回补（区间倒置抛错）；`recomputeRuntimeDay(day)` 单日 |
| `VqmsStatsRollupMapper` + XML | 五语句权威 SQL（从 IT 原生验证提升为单一来源）：调节日/月/年三级（计数求和 + algorithm_id 单一=ID/混合=MIXED）+ 投运月/年两级（分钟求和、率按合计分钟重算 99% 阈值、罚款按日累加——容量跨月变更亦记账正确；零基数率 NULL） |
| sys_job 种子（vqms.sql 九节 + 迁移） | `VQMS 统计日重算`，cron `0 0 3 * * ?`，misfire='3' 放弃、concurrent='1' 禁并发、**status='1' 默认暂停——防上线前静默产数**，上线拍板后管理员启用；种子带 NOT EXISTS 防重（兼容 IT 迁移自应用重复执行） |
| S2S3StatsTablesIT 扩展 | order(6)：mapper 级联五语句全覆盖——日级幂等重跑分母不变、月级 V1_0/5 条、年级 2026/5 条、投运月级与原生 SQL 同数（1100 分钟/rate 94.828%）、年级 shortfall=99−rate |

## 二、实现决策声明

1. **投运侧聚合层率重算下沉 SQL**（case when 零基数→NULL）：月/年行的率由合计分钟直接算出，免去「rollup 后 Java 读回写回」一个往返；99 为政策写死值与 RuntimeStatistics 常量同源同值——两处一致性由本报告声明 + 手算向量锚定。
2. **罚款按日累加而非按合计缺额×容量**：容量若跨期调整，逐日按当时容量记账才正确（审计口径），SQL `sum(penalty_score)` 天然承载。
3. **任务默认暂停**：sys_job 种子 status='1'——统计上线是拍板动作，防部署即静默产出正式考核数字（与 stub 护栏同一防线的调度侧延伸）。
4. 其余无偏离。

## 三、证据

| 文件 | 内容 |
|---|---|
| `docs/测试/证据/S4_Slice3_全模块回归212_2026-08-24.log` | `-pl ruoyi-vqms,ruoyi-admin -am test`：208+4 全绿 BUILD SUCCESS |

## 四、搁置轨收官状态

S1~S4 工程侧全部落地。剩余开放项（均非编码）：
1. 两枚 IT 跑批（外部源口令 → S1ScenarioContractIT 契约准入；主库口令 → S2S3StatsTablesIT 落库验收）
2. S4 两切片共七项默认口径确认（stub 拒写 / 门控不进分母 / yx501 缺失从严 / 并网 max 组合 / 原因 max 从严 / AVC 缺失从严 / 容量各组求和）
3. S5 政策选套拍板发起（Leo 侧，数周提前量）
4. 真实数据回放验收 + 循环码轮转实证（等现场样本）
