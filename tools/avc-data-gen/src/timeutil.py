"""时间工具：就近取整到分钟 + varchar 亚秒时间戳格式化。

VQMS 时间原则（CLAUDE.md / 草稿 §2.2）：原始 save_time / warn_time / yc_time 带毫秒，
判定前须就近取整到分钟——秒 ≥ 30 进位，< 30 舍去。这是所有逐分钟判定与汇总的对齐基础。
外部源 save_time 是 varchar(255)（非 datetime），格式如 '2021-05-07 15:27:57.556'。
"""
from __future__ import annotations

from datetime import datetime, timedelta

FMT = "%Y-%m-%d %H:%M:%S"


def round_to_minute(dt: datetime) -> datetime:
    """就近取整到分钟：秒 ≥ 30 进位到下一分钟，< 30 截断到当前分钟。

    边界：精确 30.000 按进位处理（草稿 §5.5 "round half up"；精确 30 罕见）。
    """
    if dt.second >= 30:
        return (dt.replace(second=0, microsecond=0) + timedelta(minutes=1))
    return dt.replace(second=0, microsecond=0)


def format_sv_save_time(dt: datetime) -> str:
    """his_curve_sv.save_time 格式：'2026-03-15 10:00:57.556'（亚秒三位毫秒，仿真实 dump）。"""
    ms3 = dt.microsecond // 1000
    return f"{dt.strftime(FMT)}.{ms3:03d}"


def format_warn_time(dt: datetime) -> str:
    """warn_info.warn_time 格式：同 save_time，亚秒三位毫秒。"""
    ms3 = dt.microsecond // 1000
    return f"{dt.strftime(FMT)}.{ms3:03d}"


def format_millisecond(dt: datetime) -> str:
    """warn_info.millisecond 列：亚秒的毫秒部分（三位字符串，如 '447'）。"""
    return f"{dt.microsecond // 1000:03d}"


def format_yc_time(dt: datetime) -> str:
    """yc_history.yc_time 格式：同 save_time，亚秒三位毫秒。"""
    ms3 = dt.microsecond // 1000
    return f"{dt.strftime(FMT)}.{ms3:03d}"


def jitter_save_time(dt_minute: datetime, variant: int = 0) -> datetime:
    """给目标分钟加亚秒偏离，仿真实 his_curve_sv 的 '15:27:57.556' 模式。

    返回 = 目标分钟**前一分钟**的 57/58 秒 + 毫秒；就近取整（秒 ≥ 30 进位）后回到目标分钟。
    variant 让双写母线 0/1 与连续点的时间戳略不同（仿真实采集：同周期母线 0/1 差几十毫秒）。
    判定侧必须先就近取整，才能把该点归到正确分钟——这正是 jitter 的测试价值。
    """
    prev = dt_minute.replace(second=0, microsecond=0) - timedelta(minutes=1)
    sec = 57 + (variant % 2)               # 57 或 58 秒（≥30 → 取整进位到目标分钟）
    ms = (variant * 137) % 900 + 100        # 100~999 毫秒（确定性，不依赖随机）
    return prev + timedelta(seconds=sec, milliseconds=ms)


def at_minute(base: datetime, *, days: int = 0, hour: int, minute: int,
              second: int = 0, microsecond: int = 0) -> datetime:
    """构造一个相对 base 日的时间点（便于场景锚定 t₀ 与窗口）。

    先把 base 调到当天 00:00，加 days 天偏移，再设时分秒——避免 base.day+days 跨月越界。
    """
    day0 = base.replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(days=days)
    return day0.replace(hour=hour, minute=minute, second=second, microsecond=microsecond)
