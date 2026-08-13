# VQMS AVC 考核核心算法 —— 流程图（基于 AVC考核核心算法_草稿.md）

> 配套 [AVC考核核心算法_草稿.md](../AVC考核核心算法_草稿.md)。两个考核维度（附件6 §一 投运率、§二 调节合格率）各自独立成图。
>
> ⚠️ 与旧的「电压合格率四道关」流程图（[v3.4 通俗版](backup/核心算法流程图_v3_4_通俗版.md)）**不是同一模型**：旧图讲每分钟实际电压 vs 目标窄区间；本图讲 **AVC 装置投运率（时间记账）** 和 **AVC 指令调节合格率（分档 + 免考）**。判定字段统一为 `high_SV`/`low_SV`（`average_SV`/`plan_SV` 不参与）；目标电压来自 `warn_info`（不取 `plan_SV`）。
>
> 📌 图中所有标「**来源待定**」「**待定**」之处均照录草稿未决项，未作虚构。

---

## 图 A：AVC 投运率算法（时间记账）

> 投运率考核 AVC 装置在不在线。**不适用 §三 免考**（免考仅作用于调节合格率）；它自己的免责是「扣除电网原因退出时间」。

```mermaid
graph TD
    classDef s1 fill:#f3e8ff,stroke:#6b21a8,stroke-width:2px,color:#000
    classDef s2 fill:#dbeafe,stroke:#1d4ed8,stroke-width:2px,color:#000
    classDef gate fill:#fef9c3,stroke:#a16207,stroke-width:2px,color:#000
    classDef pass fill:#dcfce7,stroke:#15803d,stroke-width:2px,color:#000
    classDef fail fill:#fecaca,stroke:#b91c1c,stroke-width:2px,color:#000
    classDef skip fill:#f1f5f9,stroke:#64748b,stroke-width:1px,color:#000
    classDef out fill:#fce7f3,stroke:#be185d,stroke-width:2px,color:#000

    Start(["统计周期：月"]) --> A1

    subgraph 取数 ["① 取数与时间对齐"]
        A1["读 yc_history：AVC 投退 yx 点<br/>yc_data 1=投 / 0=退（阶跃保持量）"]:::s1
        A1 --> A2["yc_time 就近取整到分钟<br/>秒≥30 进位；保持值取最近一条 ≤ t"]:::s2
        A2 --> A3["读并网信号（机组并网运行）<br/>来源待定"]:::s1
    end

    A3 --> G1

    subgraph 逐分钟 ["② 逐分钟遍历 t（周期内每一分钟）"]
        G1{"并网(t)？"}:::gate
        G1 -->|"否（未并网）"| S1["不计入任何分钟"]:::skip
        G1 -->|"是"| G2{"avc_in(t)？<br/>AVC 投退保持值"}:::gate
        G2 -->|"投 (1)"| P1["投运分钟 +1"]:::pass
        G2 -->|"退 (0)"| G3{"退出原因<br/>来源待定：warn_info / yc<br/>或人工标注"}:::gate
        G3 -->|"电网原因"| S2["电网原因退出分钟<br/>免责（不计罚基数）"]:::skip
        G3 -->|"非电网原因"| F1["非电网退出分钟<br/>计罚"]:::fail
    end

    P1 --> D1
    S2 --> D1
    F1 --> D1

    subgraph 汇总 ["③ 投运率统计（时间扣减）"]
        D1["投运率 =<br/>投运分钟 ÷ (投运分钟 + 非电网退出分钟)<br/>电网原因退出从分母扣除"]:::out
        D1 --> D2{"投运率 ≥ 99%？"}:::gate
        D2 -->|"是"| E1["合格"]:::pass
        D2 -->|"否"| D3["缺额 = max(0, 99% − 投运率)<br/>考核量 = 缺额百分点 × 额定容量 × 0.02 分/万千瓦"]:::fail
    end

    E1 --> End(["结束：投运率落表"])
    D3 --> End
```

---

## 图 B：调节合格率算法（分档判定 + 状态机 + 免考）

> 考核 AVC 指令下达后是否在窗口内把电压调到目标。分两档（快速性 / 经济性），先到先归类；两窗都未夹住再判免考（剔除法）。

```mermaid
graph TD
    classDef s1 fill:#f3e8ff,stroke:#6b21a8,stroke-width:2px,color:#000
    classDef s2 fill:#dbeafe,stroke:#1d4ed8,stroke-width:2px,color:#000
    classDef gate fill:#fef9c3,stroke:#a16207,stroke-width:2px,color:#000
    classDef pass fill:#dcfce7,stroke:#15803d,stroke-width:2px,color:#000
    classDef fail fill:#fecaca,stroke:#b91c1c,stroke-width:2px,color:#000
    classDef skip fill:#f1f5f9,stroke:#64748b,stroke-width:1px,color:#000
    classDef out fill:#fce7f3,stroke:#be185d,stroke-width:2px,color:#000

    Start(["一条 AVC 指令<br/>warn_info · warn_type=5"]) --> A1

    subgraph 解码 ["① 解码目标电压 V_target（不取 plan_SV）"]
        A1{"指令形态"}:::gate
        A1 -->|"目标值"| A2["文本数值 ÷ 100 → kV<br/>22315 → 223.15 kV"]:::s1
        A1 -->|"增量值"| A3["4 位编码 → 增量×100V×方向<br/>+ 当前实时母线电压<br/>2202 = +0.2 kV @ 234.25 → 234.45 kV"]:::s1
        A2 --> A4["V_target (kV)"]:::s2
        A3 --> A4
        A1 -->|"解码失败"| S1["跳过 + 记日志<br/>不计发令次数"]:::skip
    end

    A4 --> B1

    subgraph 对齐 ["② 数据对齐（按分钟）"]
        B1["t₀ = warn_time 就近取整到分钟<br/>主母线 busbar_num 筛选<br/>取 his_curve_sv：每分钟 high_SV / low_SV"]:::s2
    end

    B1 --> C1

    subgraph 快速窗 ["③ 快速性窗 [1, T_fast] 分钟"]
        C1["综合区间 [L_fast, H_fast]<br/>= min(low_SV), max(high_SV)（包络并集）"]:::s2
        C1 --> C2{"V_target ∈ [L_fast, H_fast]？"}:::gate
        C2 -->|"是（短窗内够到目标）"| Q1["快速性合格点<br/>调得快 · 停"]:::pass
        C2 -->|"否"| C3["进入经济性窗"]:::skip
    end

    C3 --> D1

    subgraph 经济窗 ["④ 经济性窗 [T_fast+1, T_econ] 分钟"]
        D1["综合区间 [L_econ, H_econ]<br/>= min(low_SV), max(high_SV)"]:::s2
        D1 --> D2{"V_target ∈ [L_econ, H_econ]？"}:::gate
        D2 -->|"是（长窗才够到目标）"| Q2["经济性合格点<br/>调得慢 · 停"]:::pass
        D2 -->|"否（两窗都没夹住）"| E0["不合格 → 判免考"]:::fail
    end

    E0 --> F1

    subgraph 免考 ["⑤ 免考判定（剔除法）"]
        F1{"全部 AVC 无功设备<br/>正确方向顶满仍不达标？<br/>需设备级无功遥测"}:::gate
        F1 -->|"是（满足免考）"| Q3["免考点<br/>不计率、不计罚（剔除分母）"]:::skip
        F1 -->|"否 / 拿不到遥测 → 留人工"| Q4["不合格-非免考<br/>罚"]:::fail
    end

    Q1 --> R1
    Q2 --> R1
    Q3 --> R1
    Q4 --> R1

    subgraph 统计 ["⑥ 调节合格率统计（剔除法）"]
        R1["发令有效次数 = 发令总次数 − 免考点数"]:::out
        R1 --> R2["调节合格率 =<br/>(快速性合格 + 经济性合格) ÷ 发令有效次数 × 100%"]:::out
        R2 --> R3["扣罚量 ∝ 不合格-非免考点数"]:::fail
    end

    R3 --> End(["结束：调节合格率落表"])
```

---

## 颜色含义

紫 = 取数 / 解码 · 蓝 = 对齐 / 综合 · 黄 = 判定关 · 绿 = 合格 · 红 = 不合格 / 罚 · 灰 = 不计 / 剔除 / 跳过 · 粉 = 汇总输出。

## 一句话总结

- **投运率**：逐分钟看「并网了没」「AVC 投着没」，投着就记投运分钟，退了按原因分流——电网原因免责扣除、非电网计罚，最后 投运分钟 ÷ (投运 + 非电网退出) 是否够 99%。
- **调节合格率**：一条指令解码出目标电压 → 先看短窗（快速性）包络夹不夹得住目标 → 夹不住再看长窗（经济性）→ 都夹不住判免考，满足免考就剔除去分母，否则计罚。

## 图中保留的待定项（照录草稿，未决）

- **退出原因来源**（图 A ②）：`warn_info` / `yc_history` 是否有 AVC 退出原因点位，待确认；若无则人工标注。
- **并网信号来源**（图 A ①）：机组并网信号 / 合并方式待定。
- **`T_econ` 上限**（图 B ④）：「≥5 min」需定上限（如 30 min），否则长越限永远在经济性窗内算合格。
- **窗口内缺数据**（图 B ③④）：某分钟无 `his_curve_sv` 记录——草稿暂定跳过（不计 min/max）+ 日志，整窗全缺则该指令剔除。
- **增量解码位定义**（图 B ①）：4 位编码第 2 位「循环码」含义待真实数据验证。
- **免考遥测可得性**（图 B ⑤）：设备级无功遥测若拿不到，免考留人工。

> 详见草稿 §1.4 / §2.8。
