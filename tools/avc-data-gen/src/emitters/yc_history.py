"""yc_history 行发射器：ScenarioBundle.yc_points + yx501_timeline -> 行字典列表。

注意 yc_history 有 UNIQUE(yc_num, yc_time)，生成器保证同点同分钟只一条。
yx501 实际存在 yx_history（开关量），但本工具统一写入 yc_history（yc_data 列）以简化测试库；
语义不变（0/1），算法侧读法一致。
"""
from __future__ import annotations

from ..scenarios.base import ScenarioBundle
from ..timeutil import format_yc_time


def emit_yc_history_rows(bundle: ScenarioBundle) -> list[dict]:
    rows = []
    seen = set()  # (yc_num, yc_time) 去重，守 UNIQUE 约束
    for yp in bundle.yc_points:
        key = (yp.yc_num, format_yc_time(yp.t))
        if key in seen:
            continue
        seen.add(key)
        rows.append({
            "yc_num": yp.yc_num,
            "yc_time": format_yc_time(yp.t),
            "yc_data": yp.value,
        })
    # yx501 免考标志时间线
    p_exempt = 501
    for t, val in bundle.yx501_timeline:
        key = (p_exempt, format_yc_time(t))
        if key in seen:
            continue
        seen.add(key)
        rows.append({
            "yc_num": p_exempt,
            "yc_time": format_yc_time(t),
            "yc_data": float(val),
        })
    return rows
