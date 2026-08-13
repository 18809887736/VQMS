"""his_curve_sv 行发射器：ScenarioBundle.curve -> 行字典列表。"""
from __future__ import annotations

from ..scenarios.base import ScenarioBundle
from ..timeutil import format_sv_save_time


def emit_his_curve_sv_rows(bundle: ScenarioBundle) -> list[dict]:
    rows = []
    for cp in bundle.curve:
        rows.append({
            "save_time": format_sv_save_time(cp.t),
            "busbar_num": cp.busbar_num,
            "high_SV": cp.high_sv,
            "low_SV": cp.low_sv,
            "average_SV": cp.average_sv if cp.average_sv is not None else cp.high_sv,
            "plan_SV": cp.plan_sv if cp.plan_sv is not None else 10245,
        })
    return rows
