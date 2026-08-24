package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Optional;

/**
 * 窗口包络聚合（正式 v1_0 §2.3；v5.0 §8.8.2 S1 组件）。
 *
 * <p>综合区间 [L, H] = [min(low_SV), max(high_SV)]——窗口内电压摆动曾覆盖的最宽范围。
 * 只聚合不判定：空窗（empty）与聚合后 L&gt;H 均原样上报，由
 * {@code DefaultRegulationJudge} 映射为该档 invalidTiers（2026-08-19 拍板口径）。</p>
 */
public final class EnvelopeAggregator
{
    private EnvelopeAggregator()
    {
    }

    /** 聚合结果：L = 下包络（low 最小值），H = 上包络（high 最大值）。 */
    public record Envelope(int low, int high)
    {
    }

    /**
     * 聚合 [loInclusive, hiInclusive] 分钟偏移范围内的曲线行。
     * 窗口内无任何行 → {@link Optional#empty()}（整档全缺，S14）。
     */
    public static Optional<Envelope> aggregate(List<MinuteCurve> curves, int loInclusive, int hiInclusive)
    {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        boolean any = false;
        for (MinuteCurve c : curves)
        {
            if (c.minuteOffset() >= loInclusive && c.minuteOffset() <= hiInclusive)
            {
                any = true;
                low = Math.min(low, c.lowSv());
                high = Math.max(high, c.highSv());
            }
        }
        return any ? Optional.of(new Envelope(low, high)) : Optional.empty();
    }
}
