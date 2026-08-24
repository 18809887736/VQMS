package com.ruoyi.vqms.statistics;

import java.util.Objects;

/**
 * 一条指令经免考应用后的两档最终记账（v5.0 §8.8.2 统计输入记录的档侧一半）。
 *
 * <p>两档平行、互不隶属——本类型只是逐档状态的容器，不含任何跨档语义。</p>
 */
public record TierFinalDisposition(FinalTierState fast, FinalTierState econ)
{
    public TierFinalDisposition
    {
        Objects.requireNonNull(fast, "fast 不可为 null");
        Objects.requireNonNull(econ, "econ 不可为 null");
    }

    /** Undecodable 整指令占位（管线合成，正式 v1_0 §8.8.2 输出路由）：两档均不可判 */
    public static TierFinalDisposition allInvalid()
    {
        return new TierFinalDisposition(FinalTierState.INVALID, FinalTierState.INVALID);
    }
}
