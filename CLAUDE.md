# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

VQMS (Voltage Quality Management System) — a voltage quality monitoring and statistics system for power utility substation busbars. Tracks voltage qualification rate, over-limit rate, under-limit rate, and time-over-limit on daily/monthly/yearly bases. The system also monitors generator reactive power for AVC (Automatic Voltage Control).

**Current state**: early documentation phase; no application code exists yet.

## Domain Context

- **Busbar (母线)**: The common electrical bus where voltage quality is measured. Two buses numbered `0` and `1`.
- **SV**: Voltage setpoint/measurement values.
- **AVC (Automatic Voltage Control)**: Automatic voltage regulation system that adjusts generator reactive power output.
- **Reactive power (Q)**: Measured in kVar. Generators output reactive power to maintain voltage targets.
- **Power factor (Cos φ)**: Stored as integer ×1,000,000 (e.g., `127400` = 0.1274).
- **plan_SV**: Remote telemetry encoded value, distinct from actual voltage. Requires decoding via `ori_code` mapping table (currently empty).
- **Voltage qualification rate**: Percentage of time voltage stays within [low_SV, high_SV] bounds.

## Database

MySQL 5.7 at `10.0.0.35`, database `qheatavchisdb`, with 7 tables:

| Table | Rows | Content |
|---|---|---|
| `yc_curve` | ~12k | Telemetry curve data |
| `his_curve_sv` | ~8k | Busbar voltage history (high, low, average, plan SV per minute) |
| `his_curve` | ~7.5k | Generator reactive power history (Q limits, average Q, line voltage, power factor) |
| `adjust_info` | ~1k | AVC adjustment records |
| `warn_info` | ~820 | Warning/alarm records |
| `yc_history` | ~800 | Real-time telemetry history |
| `ori_code` | 0 | Original code mapping table (empty) |

Key schema details:
- `his_curve_sv` and `his_curve` share the same pattern: one row per device per minute, devices numbered 0/1, `save_time` is `varchar(255)` (not datetime), no primary keys or indexes defined.
- `plan_SV` field stores raw telemetry-encoded value — not the actual voltage. The `ori_code` table is meant to decode these.

For full table structures, see [his_curve_tables.md](his_curve_tables.md).

## Nearby Sessions

The database exploration was done in a separate CCD session (`tmp.md` records it). Use `mcp__ccd_session_mgmt__search_session_transcripts` with query terms like "qheatavchisdb" or "10.0.0.35" to find related past work.
