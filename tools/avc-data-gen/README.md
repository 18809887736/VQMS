# VQMS AVC 合成数据生成器

纯 Python 工具，为 VQMS AVC 考核算法（投运率 + 调节合格率两档平行）合成覆盖全部判定分支的测试数据。
与 Java 后端解耦——产出 `.sql` 文件供导入，或直连写入。

权威算法：`docs/AVC考核核心算法_v1_0.md`（§1 投运率 + §2 调节合格率）。

## 为什么需要

真实 dump（`docs/外部DB/qheatavchisdb.md`）有三处致命缺口，无法验证判定：
- `his_curve_sv` 退化稳态（`high=low=avg`、`plan_SV=10245` 全废值），跑不出越限/波动；
- `warn_info` **无 warn_type=5**（只有 2/7/8），调节合格率所需的"遥调指令"一条都没有；
- `yc_history` **0 条数据**（纯结构 dump）。

本工具合成覆盖 29 个判定分支的数据 + 一个 `manifest.json`（场景→期望结论，算法侧当测试 oracle）。

## 场景清单（26 个）

### 调节合格率（S01–S19，R 系列）
两档平行，每档三态 QUAL / PEN / EXEMPT / SKIP。

| 场景 | 覆盖分支 | 期望 {fast, econ} |
|---|---|---|
| S01 | 快合+经合（目标值，包络夹住） | {QUAL, QUAL} |
| S02 | 快不合+经合（调得慢最终调到） | {PEN, QUAL} |
| S03 | 快合+经不合（短期夹住长期漂走） | {QUAL, PEN} |
| S04 | 两档都不合·非免考 | {PEN, PEN} |
| S05 | 免考逐档·快免考+经不免考（yx501 阶跃） | {EXEMPT, PEN} |
| S06 | 免考逐档·快不免考+经免考 | {PEN, EXEMPT} |
| S07 | 全免考（yx501 全程1） | {EXEMPT, EXEMPT} |
| S08 | 偏低边界（L=V_target 测≤闭区间） | {QUAL, PEN} |
| S09 | 增量加（2202@234.25→234.45kV） | {QUAL, QUAL} |
| S10 | 增量减（1202@234.25→234.05kV） | {QUAL, QUAL} |
| S11 | 缺实时电压→指令跳过 | {SKIP, SKIP} |
| S12 | 解码失败→指令跳过 | {SKIP, SKIP} |
| S13 | 部分缺分钟（不影响聚合） | {QUAL, QUAL} |
| S14 | 整窗全缺→该档剔除 | {SKIP, QUAL} |
| S15 | plan_SV=10245 废值干扰（算法应不读） | {QUAL, QUAL} |
| S16 | 区间 L>H 异常→该窗无效 | {SKIP, QUAL} |
| S17 | 双指令分通道（obj0夹/obj1不夹） | per_command 各结论 |
| S18 | 亚秒:29舍（warn_time 10:00:29→t0=10:00） | {QUAL, QUAL} |
| S19 | 亚秒:30进（warn_time 10:00:30→t0=10:01） | {QUAL, QUAL} |

### 投运率（U01–U07，U 系列）
逐分钟三态分流：投运 / 非电网退出（扣罚）/ 电网退出（免责）/ 未并网（不计）。

| 场景 | 覆盖 | 投运率 | 合格 |
|---|---|---|---|
| U01 | 全投 100% | 1.0 | ✓ |
| U02 | 非电网退出1分钟（yc521=2） | 99.93% | ✓ |
| U03 | 电网免责1分钟（yc521=1，扣分母） | 100% | ✓ |
| U04 | 未并网时段不计（06:00 前） | 100% | ✓ |
| U05 | 大量退出30分钟·<99%罚 | 97.92% | ✗ |
| U06 | 阶跃保持读法（三点 1/0/1，中间保持） | 95.83% | ✗ |
| U07 | 副母带电（yc512=11） | 100% | ✓ |

## 用法

```bash
# 安装依赖
pip install -r requirements.txt

# 生成 schema（三表 DDL，复用 backup/*.sql）
python -m src.cli schema --out output/00-schema.sql

# 生成单场景
python -m src.cli gen --scenario S01 --out output/scenarios/S01.sql

# 生成整组（合并）
python -m src.cli gen --group regulation --out output/all_regulation.sql
python -m src.cli gen --group uptime --out output/all_uptime.sql

# 生成全部（每场景一文件）
python -m src.cli gen --group all --out output/scenarios/ --split

# 生成期望结论清单（算法侧测试 oracle）
python -m src.cli manifest --out output/manifest.json
```

## 导入到 10.0.0.9 独立库

独立库 `vqms_avc_test`（mysql57 容器内，与真实 `qheatavchisdb` / 主库 `ry_vqms` 隔离）。

```bash
# 1. 本机生成
python -m src.cli schema --out output/00-schema.sql
python -m src.cli gen --group regulation --out output/all_regulation.sql
python -m src.cli gen --group uptime --out output/all_uptime.sql

# 2. 传服务器（密码从环境变量 MYSQL57_ROOT_PW 读，勿明文传）
scp output/00-schema.sql output/all_*.sql syth@10.0.0.9:~/vqms-avc-test/

# 3. 导入（mysql57 容器，须加 --default-character-set=utf8mb4 否则中文 warn_info 乱码）
ssh syth@10.0.0.9 'docker exec -i mysql57 mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL57_ROOT_PW" \
    -e "CREATE DATABASE IF NOT EXISTS vqms_avc_test DEFAULT CHARSET utf8mb4;"'
ssh syth@10.0.0.9 'docker exec -i mysql57 mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL57_ROOT_PW" \
    vqms_avc_test < ~/vqms-avc-test/00-schema.sql'
ssh syth@10.0.0.9 'docker exec -i mysql57 mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL57_ROOT_PW" \
    vqms_avc_test < ~/vqms-avc-test/all_regulation.sql'

# 4. 验证行数
ssh syth@10.0.0.9 'docker exec -i mysql57 mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL57_ROOT_PW" vqms_avc_test \
    -e "SELECT (SELECT COUNT(*) FROM his_curve_sv) sv, (SELECT COUNT(*) FROM warn_info) warn, (SELECT COUNT(*) FROM yc_history) yc;"'

# 清理重跑
ssh syth@10.0.0.9 'docker exec -i mysql57 mysql -uroot -p"$MYSQL57_ROOT_PW" \
    -e "DROP DATABASE IF EXISTS vqms_avc_test;"'
```

## 真实库只读验证（路 A）

`verify/` 下脚本连真实 `qheatavchisdb`，只 SELECT，验证读层（varchar save_time 解析、双写去重、排序、亚秒取整）。

```bash
python -m verify.run_verify --host 10.0.0.9 --port 3306 --user root \
    --password-env VQMS_REALDB_PASSWORD --db qheatavchisdb
```

硬约束：`cursor.execute()` 白名单仅允许 SELECT/SHOW；密码从环境变量读，不写死。

## 配置

- `config/points.yaml`：yc_num 点号映射（占位号 4001-4004，真实点号"现场整定"未公开；现场补录后改此文件）。
- `config/thresholds.yaml`：T_fast=5、T_econ=30、额定容量=600000kW、基准日 2026-03-15。

## 安全

- mysql57 root 密码在 `docs/tmp.md` 的 git 历史里（明文）。导入需明文 `-p`，建议轮换；脚本从环境变量读。
- `output/` 生成产物已 gitignore，勿提交。
- 真实库只读脚本不写任何数据。

## 关键设计

- **`decode.py` 是契约级参考实现**：算法虽未定稿，但解码（目标值÷100、增量第1位方向/第3-4位×100V）稳定。生成器反向用它算 V_target，再据此排布 high/low 让数据稳定落到目标分支。
- **双写**：his_curve_sv 每分钟生成 busbar 0+1；主母线由 yc_history 指示点（默认0）决定，副母线作干扰。
- **yc_history UNIQUE(yc_num,yc_time)**：生成器保证唯一；阶跃保持场景只在变位点写。
- **manifest.json**：场景→期望结论，算法实现侧当测试 oracle，免手写 assert。
