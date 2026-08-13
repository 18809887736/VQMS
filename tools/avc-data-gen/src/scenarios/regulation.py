"""调节合格率场景（R 系列，S01-S19）。

每个场景一条主指令（warn_type=5）+ ±35min his_curve_sv（每分钟双写 busbar 0/1）
+ 对应 yc_history 点 + yx501。用 decode 反向算 V_target，再据此排布 high/low 落到目标分支。

覆盖草稿 §2.4-2.8 两档平行全部分支。
"""
from __future__ import annotations

from datetime import timedelta

from .base import (
    Command, CurvePoint, YcPoint, QUAL, PEN, EXEMPT, SKIP,
    ScenarioBundle, ScenarioConfig,
)
from ..timeutil import at_minute


# ────────────────────────── 辅助 ──────────────────────────

def _dualwrite_curve(t0, minute_offsets, busbars, high_fn, low_fn, *,
                     average_fn=None, plan_sv=10245, missing_minutes=()):
    """生成窗口内逐分钟双写曲线。

    high_fn/low_fn: (minute_offset, busbar_num) -> int | None（None 表示该母线该分钟缺数据）。
    missing_minutes: 主母线这些 minute_offset 不生成（副母线照常，验证缺数据剔除）。
    """
    pts = []
    for m in minute_offsets:
        t = t0 + timedelta(minutes=m)
        for bn in busbars:
            if bn == 0 and m in missing_minutes:
                continue  # 主母线该分钟缺数据
            h = high_fn(m, bn)
            lo = low_fn(m, bn)
            if h is None or lo is None:
                continue
            avg = average_fn(m, bn) if average_fn else h
            pts.append(CurvePoint(t=t, busbar_num=bn, high_sv=h, low_sv=lo,
                                  average_sv=avg, plan_sv=plan_sv))
    return pts


def _realtime_and_meta(cfg: ScenarioConfig, t0, *, realtime_kv=234.25):
    """生成场景通用 yc 点：主母线号指示 + 双母线实时电压 + 总有功（@t₀）。返回 YcPoint 列表。"""
    p = cfg.points
    return [
        YcPoint(yc_num=p["main_busbar_num"], t=t0, value=0.0),           # 主母线号 = 0
        YcPoint(yc_num=p["realtime_v_busbar0"], t=t0, value=float(realtime_kv)),
        YcPoint(yc_num=p["realtime_v_busbar1"], t=t0, value=float(realtime_kv)),
        YcPoint(yc_num=p["active_power"], t=t0, value=114800.0),
    ]


# ────────────────────────── S01 快合+经合 ──────────────────────────

class S01FastQualEconQual:
    """S01：快速性档合格 + 经济性档合格（目标值指令，包络夹住 V_target）。

    目标值 22315 → V_target = 223.15 kV。
    快速窗 [1,5]：high{224,224,225,224,224} / low{222,223,222,223,222}
                 → 综合区间 [L=222, H=225] 夹 223.15 ✓
    经济窗 [6,30]：high∈{224,225} / low∈{222,223} → 夹 ✓
    期望 {fast: QUAL, econ: QUAL}。
    """
    id = "S01"

    def build(self, cfg: ScenarioConfig) -> ScenarioBundle:
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        v_target = 223.15
        cmd_text = "收到远方遥调执行指令:主省220KV目标值,22315."

        offsets = list(range(-2, 33))  # t0-2 .. t0+32，覆盖两窗 + 余量
        busbars = cfg.thresholds.get("default_busbar_pair", [0, 1])

        # 快速窗 1-5：夹住（min low=222, max high=225）
        # 经济窗 6-30：夹住
        # 窗外余量：稳态 224/222
        def high_fn(m, bn):
            if 1 <= m <= 5:
                return [224, 224, 225, 224, 224][m - 1]
            if 6 <= m <= 30:
                return 224 + (m % 2)  # 224/225 交替
            return 224
        def low_fn(m, bn):
            if 1 <= m <= 5:
                return [222, 223, 222, 223, 222][m - 1]
            if 6 <= m <= 30:
                return 222 + (m % 2)  # 222/223 交替
            return 222

        curve = _dualwrite_curve(t0, offsets, busbars, high_fn, low_fn,
                                 plan_sv=cfg.thresholds.get("plan_sv_discard", 10245))
        yc = _realtime_and_meta(cfg, t0)

        return ScenarioBundle(
            scenario_id=self.id,
            description="快合+经合（目标值 22315→223.15kV，包络夹住）",
            base_date=cfg.base_date,
            commands=[Command(t0=t0, obj_num=0, warn_info_text=cmd_text)],
            curve=curve,
            yc_points=yc,
            yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": v_target},
        )


# 场景注册表（D3 补齐其余 S02-S19）
REGULATION_SCENARIOS = [S01FastQualEconQual()]
