"""真实库只读验证入口：跑全部探针，输出 Markdown 报告。

用法：
    python -m verify.run_verify --host 10.0.0.9 --port 3306 --user root \
        --password-env VQMS_REALDB_PASSWORD --db qheatavchisdb

密码从环境变量读（--password-env 指定变量名），不写死、不进命令行历史。
所有探针只 SELECT（realdb_reader._assert_readonly 源码层强制）。
"""
from __future__ import annotations

import argparse
import os
import sys

from verify.realdb_reader import RealDbReader


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="realdb-verify", description="真实库只读验证（路 A）")
    parser.add_argument("--host", required=True)
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password-env", required=True, help="存密码的环境变量名（如 VQMS_REALDB_PASSWORD）")
    parser.add_argument("--db", default="qheatavchisdb")
    args = parser.parse_args(argv)

    pw = os.environ.get(args.password_env)
    if not pw:
        print(f"[FAIL] 环境变量 {args.password_env} 未设置", file=sys.stderr)
        return 2

    reader = RealDbReader(args.host, args.port, args.user, pw, args.db)
    print(f"# 真实库只读验证报告\n\n目标：{args.host}:{args.port}/{args.db}\n")
    try:
        # 1. 连通性
        print("## 1. 连通性与行数\n")
        try:
            conn = reader.test_connection()
            print(f"- 表清单：{conn['tables']}")
            print(f"- 各表行数：{conn['row_counts']}\n")
        except Exception as e:
            print(f"- [FAIL] 连接失败：{e}\n")
            return 1

        # 2. his_curve_sv
        print("## 2. his_curve_sv 读层\n")
        try:
            p = reader.probe_his_curve_sv()
            print(f"- 样本数：{p['sample_count']}")
            print(f"- save_time varchar 解析成功：{p['save_time_parse_ok']}，失败：{p['save_time_parse_fail'] or '无'}")
            print(f"- 双写（同时刻 busbar 0+1）：{'[OK]' if p['dual_write_ok'] else '[FAIL]'} {p['dual_write_sample']}")
            print(f"- busbar_num 取值：{p['busbar_num_values']}\n")
        except Exception as e:
            print(f"- [FAIL] {e}\n")

        # 3. warn_info
        print("## 3. warn_info warn_type 分布\n")
        try:
            w = reader.probe_warn_info_types()
            print(f"- warn_type 分布：{w['warn_type_dist']}")
            print(f"- 存在 warn_type=5（遥调指令）：{'是 [确认须合成]' if w['has_warn_type_5'] else '否 [确认须合成]'}\n")
        except Exception as e:
            print(f"- [FAIL] {e}\n")

        # 4. yc_history 点号
        print("## 4. yc_history 已有点号\n")
        try:
            y = reader.probe_yc_history_points()
            print(f"- distinct 点号数：{y['total_distinct_points']}")
            print(f"- 点号清单：{y['existing_yc_nums']}\n")
        except Exception as e:
            print(f"- [FAIL] (yc_history 真实 dump 为空属正常): {e}\n")

        # 5. 就近取整
        print("## 5. 就近取整到分钟\n")
        try:
            t = reader.verify_time_rounding()
            for s in t["rounding_samples"][:10]:
                print(f"  {s['raw']} → {s['rounded']}")
            print()
        except Exception as e:
            print(f"- [FAIL] {e}\n")

    finally:
        reader.close()
    print("（所有探针均为只读 SELECT，未写入任何数据）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
