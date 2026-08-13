"""场景基类：ScenarioBundle 统一容器 + Scenario 协议 + 期望结论枚举。

每个场景 = 一个独立的"日"，产出一组（指令 + 窗口内逐分钟曲线 + yc 点 + 免考标志），
且带 expected 期望结论（供 manifest.json 当测试 oracle）。
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Protocol


# 判定三态（草稿 §2.5）
QUAL = "QUAL"        # 合格
PEN = "PEN"          # 不合格·非免考（罚）
EXEMPT = "EXEMPT"    # 不合格·免考（剔除，不计分母不计罚）
SKIP = "SKIP"        # 指令/档位剔除（不计发令次数）


@dataclass
class CurvePoint:
    """his_curve_sv 一行（主母线由 yc_history 指示点定，副母线作干扰）。"""
    t: datetime                      # 原始时间戳（带亚秒，写库前格式化）
    busbar_num: int                  # 0/1/2
    high_sv: int                     # 窗口观测最大值（判定用，整数 kV）
    low_sv: int                      # 窗口观测最小值（判定用，整数 kV）
    average_sv: int | None = None    # 窗口均值（不参与判定，默认填与 high 一致或自定义）
    plan_sv: int | None = 10245      # 废值干扰（默认 10245，验证算法不读它）


@dataclass
class Command:
    """warn_info 一条指令（warn_type=5）。"""
    t0: datetime                     # 指令下达时刻（就近取整到分钟后的 t₀）
    obj_num: int                     # 对象编号（分通道用）
    warn_info_text: str              # 完整文本（目标值 / 增量形态）
    realtime_v_kv: float | None = None  # 增量形态拼接用的 t₀ 实时电压；目标值形态不用


@dataclass
class YcPoint:
    """yc_history 一行。yc_data 是 double（带小数）。"""
    yc_num: int
    t: datetime                      # 原始时间戳（带亚秒）
    value: float


@dataclass
class ScenarioBundle:
    """一个场景的全部产出 + 期望结论。"""
    scenario_id: str
    description: str
    base_date: datetime
    commands: list[Command] = field(default_factory=list)
    curve: list[CurvePoint] = field(default_factory=list)   # 已含双写
    yc_points: list[YcPoint] = field(default_factory=list)
    yx501_timeline: list[tuple[datetime, int]] = field(default_factory=list)  # (时刻, 0/1)
    # 期望：调节 {fast, econ} ∈ {QUAL,PEN,EXEMPT,SKIP}；投运率 {rate: float, qualified: bool}
    expected: dict = field(default_factory=dict)


@dataclass
class ScenarioConfig:
    """场景构建所需的配置（points + thresholds 合并）。"""
    points: dict
    thresholds: dict
    base_date: datetime


class Scenario(Protocol):
    """每个场景实现此协议：id 属性 + build(cfg) -> ScenarioBundle。"""
    id: str

    def build(self, cfg: ScenarioConfig) -> ScenarioBundle: ...
