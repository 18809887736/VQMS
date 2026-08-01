# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

**This is a planning/docs-only repo — no backend, frontend, or infrastructure code exists yet.** The implementation described below is the agreed target, captured in the planning docs. When code lands, update this file with real build/test commands.

- `项目规划_v2.md` — **authoritative** plan (v2 monolith). Work against this one.
- `项目规划.md` — v1 microservices plan, **superseded**. Kept for history; do not implement from it.
- `his_curve_sv.md` / `his_curve_tables.md` — schema + sample data for the external source tables.
- `tmp.md` — scratch notes; **contains plaintext DB credentials** (see Security).
- Docs and UI are in Chinese; the domain is Chinese power-utility AVC / substation busbar voltage.

## Architecture (v2 monolith — the target)

Single Spring Boot 3.x (Java 17) app, deployed as **3 Docker Compose containers**: `postgres`, `backend`, `nginx`. No microservices, no Redis, no second JVM.

- **PostgreSQL 15** — the *only* persistent store. Holds management data (users/roles/config/logs) **and** derived statistics (daily/monthly/yearly). It must **never** store raw business data.
- **External source** — read-only business data (`his_curve_sv` raw voltage curve). Currently MySQL 5.7 mirrored at `10.0.0.9 / qheatavchisdb`; designed to be swappable to MySQL 8 / SQLite / PostgreSQL later (see below).
- **JWT stateless auth** — login signs a token, Spring Security filter validates; no session store, no Redis, no hot cache.
- **Backend package layout** (`com.vqms`): `common` (config/entities/dto/exception) · `source` (external read layer) · `statistics` (algorithm, shared by web + ingestion) · `ingestion` (`@Scheduled` precompute) · `web` (controllers) · `security` (JWT).
- Statistics module is called **in-process** (direct method call, not network) by both the query controllers and the scheduler. No Feign, no gateway.
- **Planned ports** (host:container): postgres `15432:5432`, backend `7000:7000`, nginx `8080:80`.

## Storage-split rule (easy to violate — read this)

Raw voltage curves are **never copied into PG**. The external source is the single source of truth for raw data; PG stores only recomputable derived statistics. The `source/` package is the sole read path to the external DB and is read-only by design (prevents accidental writes). When in doubt about where data lives: raw = external source; management + stats = PG.

## External-source DB portability

The external source type is expected to change (currently MySQL 5.7). The `source` layer must stay dialect-agnostic:
- Access goes through a `HisCurveSvReader` **interface** returning JDBC-independent domain objects; one impl per dialect (`Mysql57CurveReader` today).
- `save_time` is `varchar` and date-parse syntax differs by DB (`STR_TO_DATE` / `strftime` / `to_timestamp`) — keep parsing inside the impl, or `SELECT` the raw string and parse in Java.
- Selection via config: `source.type=mysql57|mysql8|sqlite|postgres` + `source.driver` / `source.url` in `.env`/`sys_config`. Switching = change config + swap impl Bean; statistics and web code unchanged.
- Only generic SQL (`SELECT ... WHERE save_time BETWEEN ? AND ? AND busbar_num = ?`); no DB-specific constructs.

## Voltage-quality algorithm & source-data quirks

These constraints come from the source schema and are non-obvious — they affect every query and calculation:

- **`save_time` is `varchar(255)`, not `datetime`**, carries milliseconds, and has no timezone. Interpret as `Asia/Shanghai`; validate format on read and **skip+log** malformed rows rather than failing the batch.
- **No primary key, no index** on `his_curve_sv`. Reads must dedup + sort by `(save_time, busbar_num)`.
- **Dual-write**: each minute writes one row for busbar `0` and one for busbar `1`. Group all statistics by `busbar_num`.
- **Per-minute verdict** uses `average_SV` against `[low_SV, high_SV]`: `low≤avg≤high` = qualified, `avg>high` = over-high, `avg<low` = over-low. Boundary equality counts as qualified.
- **`plan_SV` is raw telemetry code** (e.g. `10245`), not a real voltage — needs `ori_code` decode, but that table is currently **empty**. Treat as placeholder TODO; read/display raw for now.
- **Rollup weighting (critical)**: monthly stats roll up from daily, yearly from monthly, via SQL `INSERT...SELECT...GROUP BY` summing **minute counts**. **Never average the rate columns directly** — recompute `qualification_rate = SUM(qualified_minutes)/SUM(total_minutes)*100` etc. `avg_SV` is weighted by `total_minutes`.
- Sample data is degenerate steady-state (`high=low=avg=234` → 100% qualified); algorithm branches for over-limit cases need real varying data to validate.

## Security

- `tmp.md` committed **plaintext DB credentials** (MySQL root password, host). Before this repo is ever made public: scrub the password from git history and rotate it. Do not add new secrets to tracked files.
- `.env` (planned) must never be committed — it holds external-source connection strings, PG password, JWT secret.
