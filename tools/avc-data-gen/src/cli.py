"""命令行入口（D1 最小版：跑通 S01 出 .sql；D4 补全 manifest/direct/all/group）。

用法（D1）：
    python -m src.cli gen --scenario S01 --out output/scenarios/S01.sql
    python -m src.cli schema --out output/00-schema.sql
"""
from __future__ import annotations

import argparse
import sys
from datetime import datetime
from pathlib import Path

import yaml

from .scenarios.base import ScenarioConfig
from .scenarios.regulation import REGULATION_SCENARIOS
from .sql_writer import write_schema_sql, write_bundle_sql

_CONFIG_DIR = Path(__file__).resolve().parents[1] / "config"


def _load_config() -> ScenarioConfig:
    points = yaml.safe_load((_CONFIG_DIR / "points.yaml").read_text(encoding="utf-8"))
    thresholds = yaml.safe_load((_CONFIG_DIR / "thresholds.yaml").read_text(encoding="utf-8"))
    base_date = datetime.strptime(thresholds["base_date"], "%Y-%m-%d")
    return ScenarioConfig(points=points, thresholds=thresholds, base_date=base_date)


def _find_scenario(sid: str):
    for s in REGULATION_SCENARIOS:
        if s.id == sid:
            return s
    raise SystemExit(f"未知场景: {sid}（当前仅注册了 {[s.id for s in REGULATION_SCENARIOS]}）")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="avc-data-gen", description="VQMS AVC 合成数据生成器")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_schema = sub.add_parser("schema", help="生成 00-schema.sql（三表 DDL）")
    p_schema.add_argument("--out", required=True)

    p_gen = sub.add_parser("gen", help="生成场景 .sql")
    p_gen.add_argument("--scenario", required=True, help="场景 ID（如 S01）")
    p_gen.add_argument("--out", required=True)

    args = parser.parse_args(argv)
    cfg = _load_config()

    if args.cmd == "schema":
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        write_schema_sql(out)
        print(f"[OK] schema -> {out}")
    elif args.cmd == "gen":
        sc = _find_scenario(args.scenario)
        bundle = sc.build(cfg)
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        write_bundle_sql(bundle, out)
        print(f"[OK] {args.scenario} -> {out}  (curve={len(bundle.curve)}, "
              f"warn={len(bundle.commands)}, yc={len(bundle.yc_points)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
