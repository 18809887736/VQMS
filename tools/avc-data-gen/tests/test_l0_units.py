"""L0 纯工具单元测试。

测判定链的算术地基：取整、解码、包络聚合。
设计要点：这组输入输出同时是 Java 侧（ruoyi-vqms）decode/round/envelope 单测的参照——
Python 与 Java 实现对同一组样例必须给出一致结果（decode 奇偶校验）。

运行：cd tools/avc-data-gen && python -m pytest tests/test_l0_units.py -v
（无 pytest 则：python -m tests.test_l0_units）
"""
from __future__ import annotations

import sys
from datetime import datetime

from src.timeutil import round_to_minute, jitter_save_time
from src.decode import decode_target_value, decode_increment, decode_any


def _run_all():
    """简单断言运行器（无 pytest 依赖也能跑）。"""
    passed, failed = 0, 0
    def check(name, got, want):
        nonlocal passed, failed
        ok = got == want
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}: got={got!r} want={want!r}")
        if ok: passed += 1
        else: failed += 1

    print("== round_to_minute 边界 ==")
    check(":29.4 舍", round_to_minute(datetime(2026,3,15,10,0,29,400000)).strftime("%H:%M"), "10:00")
    check(":30.4 进", round_to_minute(datetime(2026,3,15,10,0,30,400000)).strftime("%H:%M"), "10:01")
    check(":00.0 整", round_to_minute(datetime(2026,3,15,10,0,0,0)).strftime("%H:%M"), "10:00")
    check("59:29 当分", round_to_minute(datetime(2026,3,15,10,59,29,0)).strftime("%H:%M"), "10:59")
    check("59:30 进位小时", round_to_minute(datetime(2026,3,15,10,59,30,0)).strftime("%H:%M"), "11:00")

    print("== decode 目标值 ==")
    check("22315→223.15", decode_target_value("收到远方遥调执行指令:主省220KV目标值,22315."), 223.15)
    check("22500→225.0", decode_target_value("收到远方遥调执行指令:主省220KV目标值,22500."), 225.0)
    check("abc 失败", decode_target_value("收到远方遥调执行指令:主省220KV目标值,abc."), None)

    print("== decode 增量 ==")
    check("2202@234.25→234.45", decode_increment("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 234.25), 234.45)
    check("1202@234.25→234.05", decode_increment("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,1202.", 234.25), 234.05)
    check("增量缺实时电压", decode_increment("...,2202.", None), None)

    print("== decode_any 自动识别 ==")
    check("any 目标值", decode_any("...目标值,22315.", None), 223.15)
    check("any 增量", decode_any("...增量指令...,2202.", 234.25), 234.45)

    print("== jitter 取整回归 ==")
    t10 = datetime(2026,3,15,10,0,0,0)
    raw = jitter_save_time(t10, variant=0)
    check("jitter raw 秒=57", raw.second, 57)
    check("jitter 取整回 10:00", round_to_minute(raw).strftime("%H:%M"), "10:00")

    print(f"\n{'='*40}\nL0: {passed} passed, {failed} failed")
    return failed == 0


# ---- pytest 兼容入口 ----
def test_round_boundaries():
    assert round_to_minute(datetime(2026,3,15,10,0,29,400000)).strftime("%H:%M") == "10:00"
    assert round_to_minute(datetime(2026,3,15,10,0,30,400000)).strftime("%H:%M") == "10:01"
    assert round_to_minute(datetime(2026,3,15,10,59,30,0)).strftime("%H:%M") == "11:00"

def test_decode_target_value():
    assert decode_target_value("...,22315.") == 223.15
    assert decode_target_value("...,22500.") == 225.0
    assert decode_target_value("...,abc.") is None

def test_decode_increment():
    assert decode_increment("...,2202.", 234.25) == 234.45
    assert decode_increment("...,1202.", 234.25) == 234.05
    assert decode_increment("...,2202.", None) is None

def test_jitter_round_trip():
    t = datetime(2026,3,15,10,0)
    raw = jitter_save_time(t, variant=0)
    assert raw.second >= 30
    assert round_to_minute(raw).strftime("%H:%M") == "10:00"


if __name__ == "__main__":
    sys.exit(0 if _run_all() else 1)
