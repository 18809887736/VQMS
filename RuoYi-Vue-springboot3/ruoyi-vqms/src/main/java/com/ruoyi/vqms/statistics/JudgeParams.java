package com.ruoyi.vqms.statistics;

/**
 * 判定输入：整定参数（来自 vqms_judge_param，D7）。
 *
 * <p>t_fast ∈ [1,5) 整数可整定（默认建议 4）；t_econ = 5 写死（指令 5 分钟间隔）。
 * 合成场景布局口径为 (5, 30)（生成器 _win_curve 布局，见 D9 场景契约 IT 注）——
 * 与生产种子 (4,5) 的对齐随真实数据回放确认（测试方案已登记前置）。</p>
 */
public record JudgeParams(int tFast, int tEcon)
{
    public JudgeParams
    {
        if (tFast < 1 || tEcon < tFast)
        {
            throw new IllegalArgumentException("须 1 ≤ tFast ≤ tEcon: tFast=" + tFast + ", tEcon=" + tEcon);
        }
    }
}
