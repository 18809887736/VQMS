package com.ruoyi.vqms.statistics;

import java.util.Objects;

/**
 * AVC 投运率统计（正式 v1_0 §1.1/§1.5，附件6 §一）——纯函数。
 *
 * <p>投运率 = 投运分钟 / (投运分钟 + 非电网退出分钟) × 100%——电网原因退出从分母
 * 扣除（免责语义）；缺额 = max(0, 99% − 投运率)；考核量 = 缺额百分点 × 额定容量 ×
 * 0.02 分/万千瓦（与调节合格率同单价、线性非分档）。</p>
 */
public final class RuntimeStatistics
{
    /** 合格标准 ≥99%（附件6 §一，政策原文写死——无现场整定诉求） */
    private static final double PASS_THRESHOLD_PCT = 99.0;

    private static final double KW_PER_WAN_KW = 10_000.0;

    private static final double SCORE_PER_POINT_PER_WAN_KW = 0.02;

    /**
     * 单周期结果。ratePct 分母 = 投运 + 非电网退出（EXIT_GRID 免责扣除）；
     * OFFLINE 不进任何算术、仅随计数透传供报表核对。
     */
    public record RuntimeRateResult(RuntimeMinuteCounts counts,
            double ratePct, double shortfallPct, double penaltyScore)
    {
    }

    /**
     * @param counts          周期内逐分钟状态计数（调用方保证口径：分母 = 并网运行分钟）
     * @param ratedCapacityKw 额定容量（kW），≥ 0
     */
    public static RuntimeRateResult summarize(RuntimeMinuteCounts counts, double ratedCapacityKw)
    {
        Objects.requireNonNull(counts, "counts 不可为 null");
        if (Double.isNaN(ratedCapacityKw) || ratedCapacityKw < 0)
        {
            throw new IllegalArgumentException("额定容量须 ≥ 0 kW: " + ratedCapacityKw);
        }
        int denominator = counts.inService() + counts.exitNonGrid();
        if (denominator == 0)
        {
            // 约定：零并网分钟（全 OFFLINE / 空周期）无可考核基数 → 全 0 不产 NaN，
            // 报表侧（是否呈现 NULL）由调用方决定；不得经 max(0,99−0) 罚出 99 点
            return new RuntimeRateResult(counts, 0.0, 0.0, 0.0);
        }
        double ratePct = counts.inService() * 100.0 / denominator;
        double shortfallPct = Math.max(0.0, PASS_THRESHOLD_PCT - ratePct);
        double penaltyScore =
                shortfallPct * (ratedCapacityKw / KW_PER_WAN_KW) * SCORE_PER_POINT_PER_WAN_KW;
        return new RuntimeRateResult(counts, ratePct, shortfallPct, penaltyScore);
    }
}
