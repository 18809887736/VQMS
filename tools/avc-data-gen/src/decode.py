"""指令解码参考实现（契约级稳定，算法未定稿但解码规则已定）。

来源：docs/数据源头（草稿）.md §二.1 + docs/AVC考核核心算法_草稿.md §2.1。

warn_info.warn_info 文本两种形态：
- 目标值：'收到远方遥调执行指令:主省220KV目标值,22315.'  → 文本内数值 ÷ 100 = kV
- 增量值：'收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.'
          4 位编码：第1位方向(2=加/1=减) · 第2位循环码(含义待定) · 第3-4位幅值(×100V)
          → V_target = t₀ 实时电压 ± 幅值（需外部提供 realtime_v_kv）

生成器内部反向使用：先 decode 出 V_target，再据此排布 high/low 让场景落到目标分支。
"""
from __future__ import annotations

import re

# 抓取 warn_info 文本末尾 ",<数字>." 里的编码
_TRAILING_CODE = re.compile(r",(\d+)\.\s*$")


def _extract_code(text: str) -> str | None:
    m = _TRAILING_CODE.search(text.strip())
    return m.group(1) if m else None


def decode_target_value(text: str) -> float | None:
    """目标值形态：文本内数值 ÷ 100 → kV。如 '...目标值,22315.' → 223.15。

    无法识别返回 None（调用方应跳过该指令，草稿 §2.8 解码失败处理）。
    """
    code = _extract_code(text)
    if code is None:
        return None
    # 目标值通常 5 位（220kV 档）或 6 位（500kV 档），÷100 得 kV
    try:
        return int(code) / 100.0
    except ValueError:
        return None


def decode_increment(text: str, realtime_v_kv: float | None) -> float | None:
    """增量值形态：4 位编码 → V_target = realtime_v ± 幅值(kV)。

    编码：第1位方向(2=加/1=减) · 第2位循环码 · 第3-4位幅值(每单位 100V = 0.1kV)。
    需外部提供 realtime_v_kv（t₀ 时刻实时母线电压）；缺失返回 None（草稿 §2.8 缺实时电压跳过）。
    """
    code = _extract_code(text)
    if code is None or len(code) != 4:
        return None
    if realtime_v_kv is None:
        return None
    try:
        direction = int(code[0])
        magnitude_units = int(code[2:4])  # 第3-4位
    except ValueError:
        return None
    delta_kv = magnitude_units * 0.1  # 每单位 100V = 0.1 kV
    if direction == 2:       # 加
        return round(realtime_v_kv + delta_kv, 2)
    elif direction == 1:     # 减
        return round(realtime_v_kv - delta_kv, 2)
    return None


def decode_any(text: str, realtime_v_kv: float | None) -> float | None:
    """自动识别两种形态。目标值优先（文本含"目标值"），否则按增量解码。

    失败返回 None。第2位循环码语义待真实数据确认（草稿 §2.8），当前不依赖它。
    """
    if "目标值" in text:
        return decode_target_value(text)
    return decode_increment(text, realtime_v_kv)
