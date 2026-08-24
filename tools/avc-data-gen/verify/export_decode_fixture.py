"""S1 解码差分 fixture 导出（测试方案 §5.0 VTargetDecoder 行：Python decode.py vs Java 差分）。

从 output/scenarios/S01..S19.sql 离线提取同一批 warn_info 原文（含 yc4002 实时电压，
阶跃保持语义取 ≤ 指令时刻最近一条），经 src/decode.py 参考解码，产出 Java 侧
回放比对 fixture：src/test/resources（VQMS repo）下 s1_decode_fixture.json。

用法：在 tools/avc-data-gen 目录下 `python verify/export_decode_fixture.py`。
"""
from __future__ import annotations

import json
import re
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))
from decode import decode_any  # noqa: E402

SCENARIOS_DIR = Path(__file__).resolve().parents[1] / "output" / "scenarios"
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("s1_decode_fixture.json")

WARN_RE = re.compile(r"\('([^']+)',\s*'[^']*',\s*5,\s*[^,]+,\s*'(收到远方遥调执行指令:[^']*)'\)")
YC4002_RE = re.compile(r"\(4002,\s*'([\d\- :.]+)',\s*([\d.]+)\)")


def parse_ts(text: str) -> datetime:
    return datetime.strptime(text.split(".")[0], "%Y-%m-%d %H:%M:%S")


def main() -> None:
    entries: list[dict] = []
    for path in sorted(SCENARIOS_DIR.glob("S*.sql")):
        if not re.fullmatch(r"S(?:0[1-9]|1[0-9])\.sql", path.name):
            continue
        content = path.read_text(encoding="utf-8")
        realtime_rows = [(parse_ts(t), float(v)) for t, v in YC4002_RE.findall(content)]
        for warn_time, text in WARN_RE.findall(content):
            at = parse_ts(warn_time)
            prior = sorted((ts, v) for ts, v in realtime_rows if ts <= at)
            realtime = prior[-1][1] if prior else None
            py_v = decode_any(text, realtime)
            entries.append({
                "scenario": path.stem,
                "warn_content": text,
                "realtime_kv": realtime,
                "py_v_target": py_v,
            })
    OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(entries)} commands -> {OUT}")


if __name__ == "__main__":
    main()
