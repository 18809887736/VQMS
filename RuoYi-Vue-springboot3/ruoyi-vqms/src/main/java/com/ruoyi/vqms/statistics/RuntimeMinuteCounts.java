package com.ruoyi.vqms.statistics;

/**
 * 一个统计周期的逐分钟状态计数（{@link RuntimeMinuteClassifier} 输出的记账汇总）。
 *
 * <p>rollup 友好：日/月/年聚合对四个计数求和、绝不平均率列（CLAUDE.md Rollup 加权）。</p>
 */
public record RuntimeMinuteCounts(int inService, int exitGrid, int exitNonGrid, int offline)
{
    public RuntimeMinuteCounts
    {
        if (inService < 0 || exitGrid < 0 || exitNonGrid < 0 || offline < 0)
        {
            throw new IllegalArgumentException("分钟计数不可为负: inService=" + inService
                    + ", exitGrid=" + exitGrid + ", exitNonGrid=" + exitNonGrid
                    + ", offline=" + offline);
        }
    }

    /** 并网运行时间 = 投运 + 全部退出（正式 v1_0 §1.1；OFFLINE 不计入任何分钟） */
    public int onGridMinutes()
    {
        return inService + exitGrid + exitNonGrid;
    }
}
