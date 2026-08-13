"""投运率场景（U 系列，U01-U07）。

投运率 = 投运分钟 / (投运 + 非电网退出) × 100%（电网原因退出从分母扣除）。
逐分钟三态分流（草稿 §1.3-1.5）：
  并网(t)=是 且 avc_in=1 → 投运
  并网=是、avc_in=0、yc521/522=2 → 非电网退出（扣罚）
  并网=是、avc_in=0、yc521/522=1 → 电网退出（免责，扣分母）
  未并网（yc511<10 且 yc512<10）→ 不计

yc511/512 编码：值 = 带电(1/0)×10 + 机组数(0/1/2)；≥10 即并网。
AVC 投退点是阶跃保持量：两次变位间取最近≤t 的值，故生成器只在变位点写一条。
"""
from __future__ import annotations

from datetime import timedelta

from .base import YcPoint, ScenarioBundle, ScenarioConfig
from ..timeutil import at_minute


def _day_minutes(base_date):
    """生成当天 00:00..23:59 共 1440 个分钟时间点。"""
    return [at_minute(base_date, hour=m // 60, minute=m % 60) for m in range(1440)]


def _build_uptime_yc(cfg, base_date, *, grid_main_fn, grid_aux_fn,
                     onoff_changes, exit_main_changes, exit_aux_changes):
    """构造一天的 yc_history 点（投运率用）。

    grid_main_fn/aux_fn(m)->int：每分钟 yc511/512 值（并网信号）。
    onoff_changes/exit_main_changes/exit_aux_changes：list[(minute_offset, value)]，
        只在变位点写一条（阶跃保持）；生成器据此产生 yc 点。
    """
    p = cfg.points
    pts = []
    # 并网信号每分钟一条（不是阶跃保持，是周期性求值输出）
    for m, t in enumerate(_day_minutes(base_date)):
        pts.append(YcPoint(yc_num=p["grid_signal_main"], t=t, value=float(grid_main_fn(m))))
        pts.append(YcPoint(yc_num=p["grid_signal_aux"], t=t, value=float(grid_aux_fn(m))))
    # AVC 投退点（阶跃保持：只在变位点写）
    for (m, v) in onoff_changes:
        t = at_minute(base_date, hour=m // 60, minute=m % 60)
        pts.append(YcPoint(yc_num=p["avc_onoff"], t=t, value=float(v)))
    # 退出原因 yc521/522（变位点写）
    for (m, v) in exit_main_changes:
        t = at_minute(base_date, hour=m // 60, minute=m % 60)
        pts.append(YcPoint(yc_num=p["exit_reason_main"], t=t, value=float(v)))
    for (m, v) in exit_aux_changes:
        t = at_minute(base_date, hour=m // 60, minute=m % 60)
        pts.append(YcPoint(yc_num=p["exit_reason_aux"], t=t, value=float(v)))
    return pts


def _all_grid(m, val=11):
    return val


def _no_grid(m):
    return 0


# ────────────────────────── U01 全投100% ──────────────────────────

class U01AllUp100:
    """全天并网 yc511=11（1台带电）、AVC 全程投（00:00 写1）、yc521=0。期望投运率=100%。"""
    id = "U01"
    def build(self, cfg):
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 11, grid_aux_fn=lambda m: 0,
            onoff_changes=[(0, 1)],
            exit_main_changes=[(0, 0)], exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "全投100%（全天并网+AVC投）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": 1.0, "qualified": True, "up_minutes": 1440,
                      "non_grid_exit": 0, "grid_exit": 0})


# ────────────────────────── U02 非电网退出1分钟·扣罚 ──────────────────────────

class U02NonGridExit1Min:
    """1分钟非电网退出（yc521=2）。投运率=1439/1440=99.93% 合格。"""
    id = "U02"
    def build(self, cfg):
        # 12:00=退（0），12:01 恢复投（1）；yc521 12:00→2
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 11, grid_aux_fn=lambda m: 0,
            onoff_changes=[(0, 1), (720, 0), (721, 1)],   # 720=12:00
            exit_main_changes=[(0, 0), (720, 2), (721, 0)],
            exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "非电网退出1分钟·扣罚（yc521=2）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": round(1439 / 1440, 6), "qualified": True,
                      "up_minutes": 1439, "non_grid_exit": 1, "grid_exit": 0})


# ────────────────────────── U03 电网免责 ──────────────────────────

class U03GridExitExempt:
    """1分钟电网原因退出（yc521=1）。从分母扣除，投运率=100%。"""
    id = "U03"
    def build(self, cfg):
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 11, grid_aux_fn=lambda m: 0,
            onoff_changes=[(0, 1), (720, 0), (721, 1)],
            exit_main_changes=[(0, 0), (720, 1), (721, 0)],   # 1=电网免责
            exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "电网原因退出1分钟·免责（yc521=1）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": 1.0, "qualified": True,
                      "up_minutes": 1439, "non_grid_exit": 0, "grid_exit": 1})


# ────────────────────────── U04 未并网不计 ──────────────────────────

class U04NotGridNotCount:
    """夜间 00:00-05:59 未并网（yc511=yc512=0），06:00 起并网（yc511=11）。夜间 360 分钟不计。
    投运率分母 = 1080 分钟。AVC 全程投。期望投运率=100%（夜间不计）。"""
    id = "U04"
    def build(self, cfg):
        def grid(m):
            return 11 if m >= 360 else 0   # 360=06:00
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=grid, grid_aux_fn=lambda m: 0,
            onoff_changes=[(0, 1)],
            exit_main_changes=[(0, 0)], exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "未并网时段不计（06:00前不计）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": 1.0, "qualified": True, "up_minutes": 1080,
                      "non_grid_exit": 0, "grid_exit": 0, "not_grid_minutes": 360})


# ────────────────────────── U05 大量退出·<99%罚 ──────────────────────────

class U05MassExitBelow99:
    """30分钟非电网退出（yc521=2 持续）。投运率=1410/1440=97.92% <99% 罚。"""
    id = "U05"
    def build(self, cfg):
        # 12:00-12:29 共30分钟退出
        onoff = [(0, 1)] + [(720, 0), (750, 1)]   # 720=12:00, 750=12:30
        exit_main = [(0, 0), (720, 2), (750, 0)]
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 11, grid_aux_fn=lambda m: 0,
            onoff_changes=onoff, exit_main_changes=exit_main, exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "大量非电网退出·<99%罚（30分钟）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": round(1410 / 1440, 6), "qualified": False,
                      "up_minutes": 1410, "non_grid_exit": 30, "grid_exit": 0})


# ────────────────────────── U06 阶跃保持读法 ──────────────────────────

class U06StepHoldReading:
    """阶跃保持：AVC 投退点只在 00:00/12:00/13:00 写三点（1/0/1）。
    中间分钟靠"取最近≤t"保持。期望：12:00-12:59 退出60分钟（非电网），其余投运。
    yc521 在 12:00→2，13:00→0。投运率=1380/1440=95.83% <99% 罚。"""
    id = "U06"
    def build(self, cfg):
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 11, grid_aux_fn=lambda m: 0,
            onoff_changes=[(0, 1), (720, 0), (780, 1)],   # 720=12:00 退, 780=13:00 投
            exit_main_changes=[(0, 0), (720, 2), (780, 0)],
            exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "阶跃保持读法（三点 1/0/1，中间保持）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": round(1380 / 1440, 6), "qualified": False,
                      "up_minutes": 1380, "non_grid_exit": 60, "grid_exit": 0})


# ────────────────────────── U07 副母带电 ──────────────────────────

class U07AuxBusbarGrid:
    """副母线带电：全程 yc512=11（副母带电1台）、yc511=0（主母不带电）。
    任一母线带电即并网。AVC 全程投。期望投运率=100%（并网成立）。"""
    id = "U07"
    def build(self, cfg):
        yc = _build_uptime_yc(cfg, cfg.base_date,
            grid_main_fn=lambda m: 0, grid_aux_fn=lambda m: 11,   # 副母带电
            onoff_changes=[(0, 1)],
            exit_main_changes=[(0, 0)], exit_aux_changes=[(0, 0)])
        return ScenarioBundle(self.id, "副母带电（yc512=11，任一母线带电即并网）", cfg.base_date,
            commands=[], curve=[], yc_points=yc, yx501_timeline=[],
            expected={"uptime_rate": 1.0, "qualified": True, "up_minutes": 1440,
                      "non_grid_exit": 0, "grid_exit": 0})


UPTIME_SCENARIOS = [
    U01AllUp100(), U02NonGridExit1Min(), U03GridExitExempt(),
    U04NotGridNotCount(), U05MassExitBelow99(), U06StepHoldReading(), U07AuxBusbarGrid(),
]
