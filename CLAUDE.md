# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Address the user as "Leo" — every session, every response, by name.**

## Repository state

**Planning/docs-only — no VQMS application code exists yet except the RuoYi scaffolding under `RuoYi-Vue-springboot3/` (backend) and `RuoYi-Vue3/` (frontend).** The target below is captured in the planning docs. When code lands, update this file with real build/test commands.

- `项目规划_v3_1.md` — **authoritative** plan (v3.1, RuoYi base, **MySQL decided**). Work against this one.
- `backup/项目规划_v3.md` — introduced the RuoYi base but left main-DB choice open. Superseded by v3.1.
- `backup/项目规划_v2.md` — v2 "from-scratch monolith", PostgreSQL. Superseded by v3/v3.1.
- `backup/项目规划.md` — v1 microservices. Superseded.
- `his_curve_sv.md` / `his_curve_tables.md` — schema + sample data for the external source tables.
- `tmp.md` — scratch notes; **contains plaintext DB credentials** (see Security).
- `RuoYi-Vue-springboot3/` — backend scaffold (RuoYi-Vue, Spring Boot 3.5.14, Java 17). Base for the new `ruoyi-vqms` module.
- `RuoYi-Vue3/` — frontend scaffold (Vue 3 + Element Plus + Vite).
- Docs and UI are in Chinese; the domain is Chinese power-utility AVC / substation busbar voltage.

## Architecture (v3.1 — RuoYi-Vue base, MySQL decided)

Built on **若依/RuoYi-Vue** (separated front/back edition, Spring Security + JWT + Redis) — NOT the v2 from-scratch monolith, and NOT `y_project/RuoYi` (Shiro + Thymeleaf single-app). Deployed as **4 Docker Compose containers**: `mysql`, `redis`, `backend`, `nginx`.

- **MySQL 8.4** — the *only* persistent store (main DB). Holds RuoYi `sys_*` management tables **and** VQMS `voltage_quality_*` derived statistics. Must **never** store raw business data. (v2 chose PostgreSQL; v3.1 overrode it because RuoYi is MySQL-native — zero dialect/script migration.)
- **Redis** — required by RuoYi (login tokens, captcha, rate-limit, dict/config cache). v2 had no Redis; v3.1 accepts it as RuoYi's cost of entry.
- **External source** — read-only business data (`his_curve_sv` raw voltage curve). Currently MySQL 5.7 mirrored at `10.0.0.9 / qheatavchisdb`; designed to be swappable (see below). Reached via RuoYi's multi-datasource (`@DataSource(SLAVE)`) as a read-only slave, isolated from the main DB.
- **Auth** — RuoYi's Spring Security + JWT, reused as-is.
- **Backend** = RuoYi's 6 native modules (`ruoyi-admin/framework/system/quartz/generator/common`) **+ one new `ruoyi-vqms` business module** containing `source/` (external read layer), `statistics/` (qualification-rate algorithm), `ingestion/` (Quartz precompute jobs). VQMS controllers live under `com.ruoyi.web.controller.vqms`.
- Statistics module is called **in-process** (direct method call) by both query controllers and Quartz jobs. No Feign, no gateway.
- **Planned ports** (host:container): mysql `13306:3306`, redis `16379:6379`, backend `7000:7000`, nginx `8080:80`. API prefix is env-specific (`/dev-api` in dev via vite proxy, `/prod-api` in prod via Nginx; both stripped before proxying to `backend:7000`) — see `项目规划_v3_1.md` §7.

## RuoYi reuse vs VQMS build (don't reinvent the scaffold)

RuoYi solves the *generic admin system*; it does **not** solve voltage quality. Keep the boundary crisp:
- **Reuse as-is**: login/JWT, user/role/menu/dept/dict/config management, oper/login logs, Quartz scheduler (with UI), Excel via POI (`@Excel`), code-gen, Druid monitor, Swagger, multi-datasource routing, frontend shell (login/layout/menu/user-center). RuoYi's `sys_*` tables and `sql/ry_*.sql` init scripts are used directly — do not re-create management tables.
- **Build ourselves** (the actual VQMS value): `source/` (external read + dialect-swap), `statistics/` (per-minute verdict + daily/monthly/yearly weighted rollup), `ingestion/` (Quartz precompute jobs), and frontend `src/views/vqms/` dashboards (curve/daily/monthly/yearly + ECharts + export buttons).
- **Upgrade-friendliness**: keep all changes inside `ruoyi-vqms`; do not modify RuoYi's native 6 modules. Minimizes merge pain when tracking upstream RuoYi releases.

## Storage-split rule (easy to violate — read this)

Raw voltage curves are **never copied into the MySQL main DB**. The external source is the single source of truth for raw data; MySQL stores only recomputable derived statistics + management data. The `source/` layer (inside `ruoyi-vqms`) is the sole read path to the external DB and is read-only by design (prevents accidental writes). When in doubt about where data lives: raw = external source; management + stats = MySQL.

## External-source DB portability

This is about the **external source** — independent of the (decided) MySQL main DB. The external source type is expected to change (currently MySQL 5.7). The `source/` layer must stay dialect-agnostic:
- Access goes through a `HisCurveSvReader` **interface** returning JDBC-independent domain objects; one impl per dialect (`Mysql57CurveReader` today).
- `save_time` is `varchar` and date-parse syntax differs by DB (`STR_TO_DATE` / `strftime` / `to_timestamp`) — keep parsing inside the impl, or `SELECT` the raw string and parse in Java.
- Selection via config: `source.type=mysql57|mysql8|sqlite|postgres` + `source.driver` / `source.url` in `.env`/`sys_config`. Switching = change config + swap impl Bean; statistics and web code unchanged.
- Only generic SQL (`SELECT ... WHERE save_time BETWEEN ? AND ? AND busbar_num = ?`); no DB-specific constructs.

## 单位规约（Unit conventions）

VQMS 全项目统一采用以下规范单位，字段、计算、文档、UI 展示均以此为准：

| 量 | 规范单位 | 存储 | 说明 |
|---|---|---|---|
| 电压（`average_SV` / `high_SV` / `low_SV` / `plan_SV` / `nominal_kv` / `tolerance_v`） | **kV** | decimal | 源数据即为整数 kV（220kV 母线 `average_SV`≈234）；`tolerance_v` 用 `decimal(10,3)` 存（1.000 / 1.500） |
| 无功功率 | **kvar** | decimal | 引用 AVC 规定原文时照录"万千乏"（1 万千乏 = 10000 kvar） |
| 有功 / 容量 | **kW** | decimal | 引用原文时照录"万千瓦" |
| 合格率 / 投运率 | **%** | decimal | 聚合按分钟数加权，**绝不直接平均率列** |
| 时间（聚合） | **分钟数** | int | 各统计粒度记合格 / 不合格 / 剔除分钟计数 |

**关键一致性**：阈值公式 `|average_SV − plan_SV| ≤ tolerance_v` 三者必须同单位（kV）。源 SV 字段读入后**不做单位换算**（原值即 kV）。`yc_history` 模拟量按 `yc_point_map.unit` 标注，纳入统计前归一到规范单位。

## AVC 考核规定 —— VQMS 实现依据

VQMS 的考核功能以《东北区域电力并网运行管理实施细则》《东北区域电力辅助服务管理实施细则》**附件6「AVC 装置技术指标要求及考核规定」**（东北能源监管局 2024-09-04 印发，p47–48）为政策依据。原文存档于 `docs/政策口径/AVC 装置技术指标要求及考核规定.md`。三个考核维度均为 VQMS 实现目标：

1. **AVC 装置投运率** = 投运时间 / 并网运行时间 × 100%，合格 **≥99%**（扣除电网原因退出时间）。
2. **AVC 装置调节合格率** = 执行合格点数 / 发令次数 × 100%，合格 **≥100%**；调度电压/无功指令下达后，AVC 装置须在 **1 分钟内**调整到合格区间。
3. **免考**：已纳入 AVC 闭环的全部无功设备按最大发/吸能力参与仍不达标 → 该时段免于考核。

**考核单价**（投运率、调节合格率缺额通用）：每缺 1 个百分点 = 额定容量 × **0.02 分/万千瓦**，线性（非分档）。

**调节合格率分档（VQMS 细化）**：附件6 §二 的"1 分钟内"为合格线；不合格部分（>1 分钟）按响应时长分两档——**调节快速性考核** [1,5) 分钟（响应快慢 / 动态性能）、**调节经济性考核** ≥5 分钟（持续越限的经济代价）。**分档阈值（1、5 分钟）均为现场可整定**（上为默认值，运行人员可在阈值管理表调整）。命名统一用"性"（非"型"）；附件6 本身无 5 分钟分档，此为 VQMS 细化、5 分钟阈值外部依据待确认。详见 `docs/政策口径/调节合格率分档考核.md`。

**合格区间（电压调整允许偏差）= VQMS `tolerance_v` 容差权威值：**

| 电压等级 | 允许偏差 |
|---|---|
| 500 kV | ±1.5 kV (±1500 V) |
| 220 kV | ±1 kV (±1000 V) |
| 66 kV 及以下 | ±1% 额定电压 |

> ✅ v3.4 §5.2 `busbar_threshold.tolerance_v` 已据此更正并统一为 kV：原误值 220kV=300 / 500kV=500（int 伏特）已改为 `decimal(10,3)` kV，值 **1.000 / 1.500**。

## Voltage-quality algorithm & source-data quirks

These constraints come from the source schema and are non-obvious — they affect every query and calculation:

> ⚠️ **Verdict model (updated 2026-08-12, supersedes earlier design)**: qualification checks the row's `high_SV`/`low_SV` against a **static** per-busbar interval `[low_limit, high_limit]` from the VQMS threshold table. The earlier `|average_SV − plan_SV| ≤ tolerance_v` design (average_SV vs dynamic AVC band) is **abandoned**; `average_SV`/`plan_SV` no longer participate in qualification. **Open follow-ups**: threshold-table schema (absolute kV bounds vs center±tolerance), `tolerance_v`'s remaining role, and syncing `项目规划_v3_4.md` / `docs/外部数据源.md` / 核心算法流程图 (which still hold the old average_SV model).

- **`save_time` is `varchar(255)`, not `datetime``**, carries milliseconds, and has no timezone. Interpret as `Asia/Shanghai`; validate format on read and **skip+log** malformed rows rather than failing the batch.
- **No primary key, no index** on `his_curve_sv`. Reads must dedup + sort by `(save_time, busbar_num)`.
- **Dual-write**: each minute writes one row for busbar `0` and one for busbar `1`. Group all statistics by `busbar_num`.
- **Time principle: round to nearest minute (就近取整，秒 ≥ 30 进位)**. Raw timestamps in `his_curve_sv` and `yc_history` (millisecond-precision `save_time`) are **就近取整到分钟** before any per-minute aggregation — seconds ≥ 30 round up, < 30 round down. This shared rule is the alignment basis for every per-minute verdict and rollup; do **not** floor/truncate or group by raw seconds.
- **Per-minute field semantics (updated 2026-08-12)**: each `his_curve_sv` row is one minute-level acquisition window. `high_SV`/`low_SV` = window observed max/min — **the values VQMS now uses for qualification**. `average_SV` = window mean — **no longer used for qualification** (was the old design). `plan_SV` = AVC dispatch setpoint — **no longer used for qualification** (was the band center); sample value `10245` remains a discard-value (skip+log, do not decode).
- **Per-minute verdict (new model, 2026-08-12)**: checks the row's `high_SV`/`low_SV` against a **static per-busbar qualification interval** `[low_limit, high_limit]` (kV) from the VQMS threshold table: `low_limit ≤ low_SV AND high_SV ≤ high_limit` = qualified; `high_SV > high_limit` = over-high; `low_SV < low_limit` = over-low. Boundary equality counts as qualified. This is **stricter** than the old mean-band test (entire-minute swing must stay in band). `tolerance_v`'s role under this model is TBD (open). **Threshold source**: a **VQMS-managed table** (RuoYi admin, operators can edit) — `BUSBAR_VRateParameter` is explicitly excluded and does not participate in any calculation (see `docs/外部数据源.md` §3.2). Table design, first-batch values, and recalc-on-change policy still TBD; see `docs/外部数据源.md` §5.
- **`plan_SV` = AVC dispatch setpoint** (the per-minute target voltage) — **no longer used for qualification** under the new (2026-08-12) verdict model; retained as contextual/display data. The sample value `10245` is a **discard-value** (test-data garbage) — skip+log, **do not decode** (no `ori_code` investigation, no formula reverse-engineering). Details: `docs/外部数据源.md` §4.
- **Rollup weighting (critical)**: monthly stats roll up from daily, yearly from monthly, via MySQL `INSERT...SELECT...GROUP BY...ON DUPLICATE KEY UPDATE` summing **minute counts** (full DDL + rollup SQL in `项目规划_v3_1.md` §5.2 / §6.3). **Never average the rate columns directly** — recompute `qualification_rate = SUM(qualified_minutes)/SUM(total_minutes)*100` etc. `avg_SV` is weighted by `total_minutes`. (`VALUES(col)` in the UPSERT is deprecated since MySQL 8.0.20 but still works; see v3.1 §6.3 note for the alias-syntax alternative.)
- Sample data is degenerate steady-state (`high=low=avg`, no window swing); over-limit branches need real varying data to validate. Qualification depends on the TBD static threshold (not the row's high/low), so "all-qualified" does not follow from steady-state alone.

## Security

- `tmp.md` committed **plaintext DB credentials** (MySQL root password, host). Before this repo is ever made public: scrub the password from git history and rotate it. Do not add new secrets to tracked files.
- `.env` (planned) must never be committed — it holds external-source connection strings, MySQL root password (init only), app DB account credentials, Redis address, JWT secret. **App runtime connects via a least-privilege account (e.g. `vqms_app`), not root** — root is for first-time init only (create DB/tables, `CREATE USER` + `GRANT`); see `项目规划_v3_1.md` §9 for the account-split setup.
