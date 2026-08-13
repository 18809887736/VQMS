"""warn_info 行发射器：ScenarioBundle.commands -> 行字典列表。"""
from __future__ import annotations

from ..scenarios.base import ScenarioBundle
from ..timeutil import format_warn_time, format_millisecond


def emit_warn_info_rows(bundle: ScenarioBundle) -> list[dict]:
    rows = []
    for cmd in bundle.commands:
        rows.append({
            "warn_time": format_warn_time(cmd.raw_warn_time),   # 原始带亚秒（算法侧就近取整）
            "millisecond": format_millisecond(cmd.raw_warn_time),
            "warn_type": 5,  # 遥调指令固定 warn_type=5（真实库无此类型，全靠合成）
            "obj_num": cmd.obj_num,
            "warn_info": cmd.warn_info_text,
        })
    return rows
