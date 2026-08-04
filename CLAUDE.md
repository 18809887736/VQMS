# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

## Voltage-quality algorithm & source-data quirks

These constraints come from the source schema and are non-obvious — they affect every query and calculation:

- **`save_time` is `varchar(255)`, not `datetime`**, carries milliseconds, and has no timezone. Interpret as `Asia/Shanghai`; validate format on read and **skip+log** malformed rows rather than failing the batch.
- **No primary key, no index** on `his_curve_sv`. Reads must dedup + sort by `(save_time, busbar_num)`.
- **Dual-write**: each minute writes one row for busbar `0` and one for busbar `1`. Group all statistics by `busbar_num`.
- **Per-minute field semantics (confirmed — see `md/外部数据源.md` §4)**: each `his_curve_sv` row is one minute-level acquisition window. `average_SV` = window mean — **the only value VQMS uses for qualification**. `high_SV`/`low_SV` = window observed max/min (**NOT qualification thresholds**; proof: sibling table `his_curve` uses the same `high_Q/low_Q/average_Q` naming for reactive power, which has no threshold concept). `plan_SV` = AVC dispatch setpoint.
- **Per-minute verdict** uses `average_SV` against a **static per-busbar qualification interval** `[low_limit, high_limit]` — NOT the row's `low_SV`/`high_SV`: `low_limit≤avg≤high_limit` = qualified, `avg>high_limit` = over-high, `avg<low_limit` = over-low. Boundary equality counts as qualified. **Threshold source**: a **VQMS-managed table** (RuoYi admin, operators can edit) — `BUSBAR_VRateParameter` is explicitly excluded and does not participate in any calculation (see `md/外部数据源.md` §3.2). Table design, first-batch values, and recalc-on-change policy still TBD; see `md/外部数据源.md` §5.
- **`plan_SV` = AVC dispatch setpoint** (the per-minute target voltage). The sample value `10245` is a **discard-value** (test-data garbage) — skip+log, **do not decode** (no `ori_code` investigation, no formula reverse-engineering). Real `plan_SV` (when available) is used directly as the center of the AVC band (`plan_SV` ± tolerance). Details: `md/外部数据源.md` §4.
- **Rollup weighting (critical)**: monthly stats roll up from daily, yearly from monthly, via MySQL `INSERT...SELECT...GROUP BY...ON DUPLICATE KEY UPDATE` summing **minute counts** (full DDL + rollup SQL in `项目规划_v3_1.md` §5.2 / §6.3). **Never average the rate columns directly** — recompute `qualification_rate = SUM(qualified_minutes)/SUM(total_minutes)*100` etc. `avg_SV` is weighted by `total_minutes`. (`VALUES(col)` in the UPSERT is deprecated since MySQL 8.0.20 but still works; see v3.1 §6.3 note for the alias-syntax alternative.)
- Sample data is degenerate steady-state (`high=low=avg`, no window swing); over-limit branches need real varying data to validate. Qualification depends on the TBD static threshold (not the row's high/low), so "all-qualified" does not follow from steady-state alone.

## Security

- `tmp.md` committed **plaintext DB credentials** (MySQL root password, host). Before this repo is ever made public: scrub the password from git history and rotate it. Do not add new secrets to tracked files.
- `.env` (planned) must never be committed — it holds external-source connection strings, MySQL root password (init only), app DB account credentials, Redis address, JWT secret. **App runtime connects via a least-privilege account (e.g. `vqms_app`), not root** — root is for first-time init only (create DB/tables, `CREATE USER` + `GRANT`); see `项目规划_v3_1.md` §9 for the account-split setup.
