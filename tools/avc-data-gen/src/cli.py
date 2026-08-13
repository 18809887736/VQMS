"""命令行入口。

用法：
    python -m src.cli schema  --out output/00-schema.sql
    python -m src.cli gen     --scenario S01 --out output/scenarios/S01.sql
    python -m src.cli gen     --group regulation --out output/all_regulation.sql --with-ddl
    python -m src.cli gen     --all --out output/ --split          # 每场景一文件
    python -m src.cli manifest --out output/manifest.json          # 期望结论清单（测试 oracle）
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

import yaml

from .scenarios.base import ScenarioConfig
from .scenarios.regulation import REGULATION_SCENARIOS
from .scenarios.uptime import UPTIME_SCENARIOS
from .sql_writer import write_schema_sql, write_bundle_sql, write_bundled_sql

_CONFIG_DIR = Path(__file__).resolve().parents[1] / "config"
ALL_SCENARIOS = REGULATION_SCENARIOS + UPTIME_SCENARIOS


def _load_config() -> ScenarioConfig:
    points = yaml.safe_load((_CONFIG_DIR / "points.yaml").read_text(encoding="utf-8"))
    thresholds = yaml.safe_load((_CONFIG_DIR / "thresholds.yaml").read_text(encoding="utf-8"))
    base_date = datetime.strptime(thresholds["base_date"], "%Y-%m-%d")
    return ScenarioConfig(points=points, thresholds=thresholds, base_date=base_date)


def _find_scenario(sid: str):
    for s in ALL_SCENARIOS:
        if s.id == sid:
            return s
    raise SystemExit(f"未知场景: {sid}（已注册 {[s.id for s in ALL_SCENARIOS]}）")


def _select(group: str | None, scenario: str | None):
    if scenario:
        return [_find_scenario(scenario)]
    if group == "regulation":
        return REGULATION_SCENARIOS
    if group == "uptime":
        return UPTIME_SCENARIOS
    if group in ("all", None):
        return ALL_SCENARIOS
    raise SystemExit(f"未知 group: {group}（regulation/uptime/all）")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="avc-data-gen", description="VQMS AVC 合成数据生成器")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_schema = sub.add_parser("schema", help="生成 00-schema.sql（三表 DDL）")
    p_schema.add_argument("--out", required=True)

    p_gen = sub.add_parser("gen", help="生成场景 .sql")
    g = p_gen.add_mutually_exclusive_group(required=True)
    g.add_argument("--scenario", help="单个场景 ID（如 S01）")
    g.add_argument("--group", choices=["regulation", "uptime", "all"], help="场景组")
    p_gen.add_argument("--out", required=True)
    p_gen.add_argument("--with-ddl", action="store_true", help="输出文件前置 schema（仅非 split 时）")
    p_gen.add_argument("--split", action="store_true", help="每场景单独一文件（out 为目录）")

    p_man = sub.add_parser("manifest", help="输出场景→期望结论 manifest.json")
    p_man.add_argument("--out", required=True)

    args = parser.parse_args(argv)
    cfg = _load_config()

    if args.cmd == "schema":
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        write_schema_sql(out)
        print(f"[OK] schema -> {out}")
        return 0

    if args.cmd == "manifest":
        items = []
        for s in ALL_SCENARIOS:
            b = s.build(cfg)
            items.append({"id": b.scenario_id, "description": b.description,
                          "expected": b.expected})
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"[OK] manifest -> {out}  ({len(items)} 场景)")
        return 0

    if args.cmd == "gen":
        selected = _select(args.group, args.scenario)
        bundles = [s.build(cfg) for s in selected]
        if args.split:
            out_dir = Path(args.out)
            out_dir.mkdir(parents=True, exist_ok=True)
            for b in bundles:
                p = out_dir / f"{b.scenario_id}.sql"
                write_bundle_sql(b, p)
            print(f"[OK] {len(bundles)} 场景 -> {out_dir}/ (每场景一文件)")
        else:
            out = Path(args.out)
            out.parent.mkdir(parents=True, exist_ok=True)
            if len(bundles) == 1:
                write_bundle_sql(bundles[0], out, include_header=not args.with_ddl)
            else:
                label = args.group or "all"
                write_bundled_sql(bundles, out, label=label)
                if args.with_ddl:
                    # 前置 schema：拼接到同目录 00-schema.sql（split 模式更清晰，这里只提示）
                    print("[note] --with-ddl 建议配合 --split 或单场景；多场景合并请单独跑 `schema`")
            print(f"[OK] {len(bundles)} 场景 -> {out}")
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
