"""yc_history 行发射器：ScenarioBundle.yc_points + yx501_timeline -> 行字典列表。

时间戳仿真实 dump 偏离整点（57/58 秒 + 毫秒），判定侧就近取整后才回目标分钟。
注意 yc_history 有 UNIQUE(yc_num, yc_time)，生成器保证同点同分钟只一条。
yx501 实际存在 yx_history（开关量），本工具统一写入 yc_history（yc_data 列）以简化测试库。
"""
from __future__ import annotations

from ..scenarios.base import ScenarioBundle
from ..timeutil import jitter_save_time, format_yc_time


def emit_yc_history_rows(bundle: ScenarioBundle) -> list[dict]:
    rows = []
    seen = set()  # (yc_num, yc_time) 去重，守 UNIQUE 约束
    for yp in bundle.yc_points:
        raw = jitter_save_time(yp.t, variant=int(yp.yc_num) % 3)
        key = (yp.yc_num, format_yc_time(raw))
        if key in seen:
            continue
        seen.add(key)
        rows.append({
            "yc_num": yp.yc_num,
            "yc_time": format_yc_time(raw),  # 写入 jitter 原始时间（取整交给判定侧）
            "yc_data": yp.value,
        })
    # yx501 免考标志时间线
    p_exempt = 501
    for t, val in bundle.yx501_timeline:
        raw = jitter_save_time(t, variant=1)
        key = (p_exempt, format_yc_time(raw))
        if key in seen:
            continue
        seen.add(key)
        rows.append({
            "yc_num": p_exempt,
            "yc_time": format_yc_time(raw),
            "yc_data": float(val),
        })
    return rows
