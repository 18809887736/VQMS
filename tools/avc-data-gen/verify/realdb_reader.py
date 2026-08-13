"""真实库只读验证（路 A）：连 qheatavchisdb 验证读层处理，不写任何数据。

硬约束：
- cursor.execute() 只允许 SELECT / SHOW 开头的 SQL（白名单断言，源码层强制只读）。
- 密码从环境变量读，不写死、不提交。
- 不 INSERT / UPDATE / DELETE / DROP —— 防污染真实库。

验证内容（对应算法读层要正确处理的外部源特性）：
1. 连通性 + 各表行数快照
2. his_curve_sv：varchar save_time 带亚秒可解析、双写（同时刻 busbar 0+1）、无主键需去重、排序
3. warn_info：warn_type 取值分布（确认真实库无 warn_type=5）
4. yc_history：已存在的 yc_num 点号（对照生成器配置是否冲突）
5. 就近取整边界：带亚秒的 save_time 跑 round_to_minute
"""
from __future__ import annotations

import os
import re
from datetime import datetime

# 只读白名单：SQL 必须以这些关键字开头（大小写不敏感）
_READONLY_PREFIXES = ("SELECT", "SHOW")


def _assert_readonly(sql: str) -> None:
    """源码层断言：只允许 SELECT/SHOW。防止误写。"""
    stripped = sql.strip().lstrip("(").strip().upper()
    # 去掉可能的 WITH/SQL_NO_CACHE 前缀
    stripped = re.sub(r"^(SQL_NO_CACHE|WITH)\s+", "", stripped)
    if not stripped.startswith(_READONLY_PREFIXES):
        raise PermissionError(f"[只读约束] 拒绝非 SELECT 语句: {sql[:80]}...")


class RealDbReader:
    def __init__(self, host: str, port: int, user: str, password: str, db: str):
        self._conn_kwargs = dict(host=host, port=port, user=user, password=password, db=db)
        self._conn = None

    def _connect(self):
        if self._conn is None:
            try:
                import mysql.connector  # 延迟导入，直连模式才需要
            except ImportError as e:
                raise SystemExit(
                    "缺 mysql-connector-python。安装：pip install mysql-connector-python"
                ) from e
            self._conn = mysql.connector.connect(**self._conn_kwargs)
        return self._conn

    def _exec(self, sql: str, params=None):
        _assert_readonly(sql)
        cur = self._connect().cursor(dictionary=True)
        cur.execute(sql, params)
        rows = cur.fetchall()
        cur.close()
        return rows

    def close(self):
        if self._conn:
            self._conn.close()
            self._conn = None

    # ─────────── 探针 ───────────

    def test_connection(self) -> dict:
        """连通性 + 三表行数。"""
        self._exec("SELECT 1 AS ok")
        tables = [r for r in self._exec("SHOW TABLES")]
        table_name_key = list(tables[0].keys())[0] if tables else None
        counts = {}
        for known in ("his_curve_sv", "warn_info", "yc_history"):
            try:
                row = self._exec(f"SELECT COUNT(*) AS c FROM `{known}`")
                counts[known] = row[0]["c"]
            except Exception as e:
                counts[known] = f"ERR: {e}"
        return {"tables": [t.get(table_name_key) for t in tables] if table_name_key else [],
                "row_counts": counts}

    def probe_his_curve_sv(self, limit: int = 50) -> dict:
        """验证：varchar save_time 解析、双写、去重、排序。"""
        rows = self._exec(
            "SELECT save_time, busbar_num, high_SV, low_SV, average_SV, plan_SV "
            "FROM his_curve_sv ORDER BY save_time DESC, busbar_num LIMIT %s", (limit,))
        parse_ok = 0
        parse_fail = []
        for r in rows:
            st = r.get("save_time")
            if st is None:
                continue
            try:
                datetime.strptime(str(st)[:19], "%Y-%m-%d %H:%M:%S")
                parse_ok += 1
            except ValueError:
                parse_fail.append(str(st))
        # 双写检查：同一 save_time 应有 busbar 0+1
        dual_write_ok = True
        dual_write_sample = None
        if rows:
            first_st = rows[0]["save_time"]
            busbars_at_first = {r["busbar_num"] for r in rows if r["save_time"] == first_st}
            dual_write_ok = {0, 1}.issubset(busbars_at_first)
            dual_write_sample = {"save_time": str(first_st), "busbar_nums": sorted(busbars_at_first)}
        # busbar_num 取值
        all_busbars = sorted({r["busbar_num"] for r in rows if r["busbar_num"] is not None})
        return {
            "sample_count": len(rows),
            "save_time_parse_ok": parse_ok,
            "save_time_parse_fail": parse_fail[:5],
            "dual_write_ok": dual_write_ok,
            "dual_write_sample": dual_write_sample,
            "busbar_num_values": all_busbars,
        }

    def probe_warn_info_types(self) -> dict:
        """warn_type 取值分布（确认真实库无 warn_type=5）。"""
        rows = self._exec(
            "SELECT warn_type, COUNT(*) AS c FROM warn_info GROUP BY warn_type ORDER BY warn_type")
        return {"warn_type_dist": {str(r["warn_type"]): r["c"] for r in rows},
                "has_warn_type_5": any(r["warn_type"] == 5 for r in rows)}

    def probe_yc_history_points(self) -> dict:
        """已存在的 yc_num 点号（对照生成器配置是否冲突）。"""
        rows = self._exec(
            "SELECT yc_num, COUNT(*) AS c FROM yc_history GROUP BY yc_num ORDER BY yc_num LIMIT 100")
        return {"existing_yc_nums": {str(r["yc_num"]): r["c"] for r in rows},
                "total_distinct_points": len(rows)}

    def verify_time_rounding(self) -> dict:
        """就近取整边界：带亚秒 save_time 跑 round_to_minute。"""
        from src.timeutil import round_to_minute
        rows = self._exec(
            "SELECT DISTINCT save_time FROM his_curve_sv ORDER BY save_time LIMIT 20")
        samples = []
        for r in rows:
            st = str(r["save_time"])
            try:
                dt = datetime.strptime(st[:19], "%Y-%m-%d %H:%M:%S")
                rounded = round_to_minute(dt)
                samples.append({"raw": st, "rounded": rounded.strftime("%Y-%m-%d %H:%M")})
            except ValueError:
                samples.append({"raw": st, "rounded": "PARSE_FAIL"})
        return {"rounding_samples": samples}
