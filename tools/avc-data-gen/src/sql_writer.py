"""SQL 写出器：行字典 -> INSERT 语句（批量多值）+ DDL 头部。

DDL 直接复用 backup/ 下三表的 CREATE TABLE（Navicat 导出的纯结构，无 INSERT），
拼成独立库首启用的 00-schema.sql。
"""
from __future__ import annotations

from pathlib import Path

from .scenarios.base import ScenarioBundle
from .emitters.his_curve_sv import emit_his_curve_sv_rows
from .emitters.warn_info import emit_warn_info_rows
from .emitters.yc_history import emit_yc_history_rows

# backup/ 下三表 DDL 模板的相对路径（仓库根）
_SCHEMA_TEMPLATES = {
    "his_curve_sv": "backup/his_curve_sv.sql",
    "warn_info": "backup/warn_info.sql",
    "yc_history": "backup/yc_history.sql",
}

# 各表列顺序（与 DDL 一致）
_COLUMNS = {
    "his_curve_sv": ["save_time", "busbar_num", "high_SV", "low_SV", "average_SV", "plan_SV"],
    "warn_info": ["warn_time", "millisecond", "warn_type", "obj_num", "warn_info"],
    "yc_history": ["yc_num", "yc_time", "yc_data"],
}

_REPO_ROOT = Path(__file__).resolve().parents[3]  # tools/avc-data-gen/src -> 仓库根


def _quote(v) -> str:
    """SQL 字面量：字符串转义加引号，数字原样。"""
    if isinstance(v, (int, float)):
        return repr(v) if isinstance(v, float) else str(v)
    s = str(v).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"


def _emit_insert(table: str, rows: list[dict]) -> str:
    """行字典 -> 批量 INSERT 语句（多值，每 500 行一批）。"""
    if not rows:
        return f"-- {table}: (no rows)\n"
    cols = _COLUMNS[table]
    out = [f"-- {table}: {len(rows)} rows"]
    batch_size = 500
    for i in range(0, len(rows), batch_size):
        chunk = rows[i:i + batch_size]
        values_list = []
        for r in chunk:
            vals = ", ".join(_quote(r[c]) for c in cols)
            values_list.append(f"  ({vals})")
        out.append(f"INSERT INTO `{table}` VALUES\n" + ",\n".join(values_list) + ";")
    return "\n".join(out) + "\n"


def load_ddl_template(table: str) -> str:
    """从 backup/<table>.sql 提取 CREATE TABLE 段（DROP+CREATE），剥掉尾部 SET FOREIGN_KEY_CHECKS。"""
    path = _REPO_ROOT / _SCHEMA_TEMPLATES[table]
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    start = None
    for i, ln in enumerate(lines):
        if ln.startswith("DROP TABLE IF EXISTS"):
            start = i
            break
    if start is None:
        return text  # 兜底：原样返回
    body = "\n".join(lines[start:])
    # 剥掉模板自带的尾部 SET FOREIGN_KEY_CHECKS = 1（schema 文件末尾统一加一次）
    body = "\n".join(
        ln for ln in body.splitlines()
        if not ln.strip().upper().startswith("SET FOREIGN_KEY_CHECKS")
    )
    return body.strip() + "\n"


def write_schema_sql(path: Path) -> None:
    """写出 00-schema.sql：SET 头 + 三表 DDL。"""
    parts = [
        "-- VQMS AVC 合成数据测试库 schema（从 backup/*.sql 复用三表 DDL）",
        "-- 独立库 vqms_avc_test，与真实 qheatavchisdb / 主库 ry_vqms 隔离",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]
    for table in ("his_curve_sv", "warn_info", "yc_history"):
        parts.append(load_ddl_template(table))
        parts.append("")
    parts.append("SET FOREIGN_KEY_CHECKS = 1;")
    path.write_text("\n".join(parts), encoding="utf-8")


def write_bundle_sql(bundle: ScenarioBundle, path: Path, *, include_header: bool = True) -> None:
    """单个场景 -> .sql 文件（三表 INSERT）。"""
    parts = []
    if include_header:
        parts.append(f"-- 场景 {bundle.scenario_id}: {bundle.description}")
        parts.append(f"-- 期望结论: {bundle.expected}")
        parts.append("SET NAMES utf8mb4;")
        parts.append("")
    parts.append(_emit_insert("his_curve_sv", emit_his_curve_sv_rows(bundle)))
    parts.append(_emit_insert("warn_info", emit_warn_info_rows(bundle)))
    parts.append(_emit_insert("yc_history", emit_yc_history_rows(bundle)))
    path.write_text("\n".join(parts), encoding="utf-8")


def write_bundled_sql(bundles: list[ScenarioBundle], path: Path, *,
                      label: str = "all") -> None:
    """多场景合并 -> 单 .sql 文件。"""
    parts = [f"-- {label}（{len(bundles)} 场景合并）", "SET NAMES utf8mb4;", ""]
    for b in bundles:
        parts.append(f"-- ===== 场景 {b.scenario_id}: {b.description} =====")
        parts.append(_emit_insert("his_curve_sv", emit_his_curve_sv_rows(b)))
        parts.append(_emit_insert("warn_info", emit_warn_info_rows(b)))
        parts.append(_emit_insert("yc_history", emit_yc_history_rows(b)))
        parts.append("")
    path.write_text("\n".join(parts), encoding="utf-8")
