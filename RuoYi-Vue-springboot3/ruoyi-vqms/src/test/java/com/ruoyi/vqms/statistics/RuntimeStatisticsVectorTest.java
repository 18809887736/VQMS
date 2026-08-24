package com.ruoyi.vqms.statistics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * S3 L0：RuntimeStatistics 手算向量与不变式（测试方案 §5.0，正式 v1_0 §1.1/§1.5）。
 *
 * <p>口径：投运率 = 投运 / (投运 + 非电网退出)；电网退出从分母扣除（免责）；
 * OFFLINE 不进任何算术；缺额 max(0, 99−投运率) 封底；罚款 = 缺额×万千瓦×0.02 分。</p>
 */
class RuntimeStatisticsVectorTest
{
    private static final double EPS = 1e-9;

    private static final double CAP_KW = 300_000.0; // 30 万千瓦

    private static RuntimeMinuteCounts counts(int inService, int exitGrid, int exitNonGrid,
            int offline)
    {
        return new RuntimeMinuteCounts(inService, exitGrid, exitNonGrid, offline);
    }

    // ── 手算样例 1：达标月（缺额封底 0）──
    // 投运 28000、非电网退 200 → 分母 28200，率 99.2908% ≥ 99 → 缺额 0、罚款 0；
    // 电网退 800（免责）、OFFLINE 1000（不计）均不进分母

    @Test
    void handVector_aboveThreshold_zeroShortfall()
    {
        RuntimeStatistics.RuntimeRateResult r =
                RuntimeStatistics.summarize(counts(28_000, 800, 200, 1_000), CAP_KW);
        Assertions.assertEquals(29_000, r.counts().onGridMinutes(),
                "并网运行时间 = 投运 + 全部退出（含免责的电网退出；不含 OFFLINE）");
        Assertions.assertEquals(28000.0 * 100 / 28200, r.ratePct(), EPS,
                "分母 = 投运+非电网退出（电网退出/OFFLINE 均扣除）");
        Assertions.assertTrue(r.ratePct() > 99.0);
        Assertions.assertEquals(0.0, r.shortfallPct(), EPS, "max(0,…) 封底");
        Assertions.assertEquals(0.0, r.penaltyScore(), EPS);
    }

    // ── 手算样例 2：不达标——4900/5000 = 98%，缺额 1 点，罚 1×30×0.02=0.6 分 ──

    @Test
    void handVector_belowThreshold_penalty()
    {
        RuntimeStatistics.RuntimeRateResult r =
                RuntimeStatistics.summarize(counts(4_900, 0, 100, 0), CAP_KW);
        Assertions.assertEquals(98.0, r.ratePct(), EPS);
        Assertions.assertEquals(1.0, r.shortfallPct(), EPS);
        Assertions.assertEquals(0.6, r.penaltyScore(), EPS,
                "1 缺额点 × (300000/10000 万千瓦) × 0.02 分");
    }

    @Test
    void boundary_exactly99_noShortfall()
    {
        RuntimeStatistics.RuntimeRateResult r =
                RuntimeStatistics.summarize(counts(4_950, 0, 50, 0), CAP_KW);
        Assertions.assertEquals(99.0, r.ratePct(), EPS, "合格标准 ≥99%（含边界）");
        Assertions.assertEquals(0.0, r.shortfallPct(), EPS);
        Assertions.assertEquals(0.0, r.penaltyScore(), EPS);
    }

    // ── 免责语义：电网退出从分母扣除，非电网不扣 ──

    @Test
    void gridExit_exemptedFromDenominator_nonGrid_not()
    {
        double rateAllGrid = RuntimeStatistics
                .summarize(counts(4_900, 100, 0, 0), CAP_KW).ratePct();
        double rateAllNonGrid = RuntimeStatistics
                .summarize(counts(4_900, 0, 100, 0), CAP_KW).ratePct();
        Assertions.assertEquals(100.0, rateAllGrid, EPS,
                "全为电网退出 → 全部从分母扣除 → 率 100%");
        Assertions.assertEquals(98.0, rateAllNonGrid, EPS,
                "同为 100 分钟退出，原因不同率不同——免责只给电网原因");
    }

    // ── OFFLINE 不计任何分钟 ──

    @Test
    void offline_neverEntersMath()
    {
        RuntimeStatistics.RuntimeRateResult base =
                RuntimeStatistics.summarize(counts(4_900, 0, 100, 0), CAP_KW);
        RuntimeStatistics.RuntimeRateResult withOffline =
                RuntimeStatistics.summarize(counts(4_900, 0, 100, 9_999), CAP_KW);
        Assertions.assertEquals(base.ratePct(), withOffline.ratePct(), EPS);
        Assertions.assertEquals(base.penaltyScore(), withOffline.penaltyScore(), EPS);
        Assertions.assertEquals(5_000, withOffline.counts().onGridMinutes(),
                "onGridMinutes 不含 OFFLINE");
    }

    // ── 零并网分钟：无可考核基数 → 全 0（不得罚出 99 点）──

    @Test
    void zeroOnGrid_allZeros()
    {
        RuntimeStatistics.RuntimeRateResult r =
                RuntimeStatistics.summarize(counts(0, 0, 0, 600), CAP_KW);
        Assertions.assertEquals(0.0, r.ratePct(), EPS);
        Assertions.assertEquals(0.0, r.shortfallPct(), EPS,
                "空周期/全停机不经 max(0,99−0) 产生 99 点缺额");
        Assertions.assertEquals(0.0, r.penaltyScore(), EPS);
    }

    // ── 输入校验 ──

    @Test
    void negativeCounts_throws()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RuntimeMinuteCounts(-1, 0, 0, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RuntimeMinuteCounts(0, 0, -5, 0));
    }

    @Test
    void negativeOrNaNCapacity_throws()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeStatistics.summarize(counts(1, 0, 0, 0), -1.0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeStatistics.summarize(counts(1, 0, 0, 0), Double.NaN));
    }
}
