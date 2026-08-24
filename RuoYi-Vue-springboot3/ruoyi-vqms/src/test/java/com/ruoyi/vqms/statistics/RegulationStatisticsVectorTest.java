package com.ruoyi.vqms.statistics;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * S2 L0：RegulationStatistics 手算向量与不变式（测试方案 §5.0，正式 v1_0 §2.7）。
 *
 * <p>口径：固定分母 = 发令总次数（两档同分母）；免考不进分子、不剔分母；
 * INVALID（invalidTiers / Undecodable 占位）计入分母、拖低披露合格率。</p>
 *
 * <p>⚠️ 两链结构属<b>推荐读法</b>（罚款缺额 = 非免考不合格 ÷ 全量分母，剔免考待 Leo
 * 确认——正式版 §2.7）：标 ⚠ 的断言若拍板否掉则改为「罚款缺额 = 100 − 披露合格率」
 * 单链，本类相应重写（测试方案 §5.0 预告）。</p>
 */
class RegulationStatisticsVectorTest
{
    private static final double EPS = 1e-9;

    private static TierFinalDisposition d(FinalTierState fast, FinalTierState econ)
    {
        return new TierFinalDisposition(fast, econ);
    }

    // ── 手算样例 1：10 指令两档混合（含免考、不可判），容量 30 万千瓦 ──
    // 快速性：Q6 P2 E1 I1 → 披露 60%、缺额 20%、罚款 20×30×0.02=12 分
    // 经济性：Q5 P3 E1 I1 → 披露 50%、缺额 30%、罚款 30×30×0.02=18 分；总罚款 30 分

    @Test
    void handVector_tenCommands_twoTiersParallel()
    {
        //        指令:  1    2    3    4    5    6    7    8    9    10
        // 快速性列:      Q    Q    Q    Q    Q    Q    P    P    E    I   → Q6 P2 E1 I1
        // 经济性列:      Q    Q    Q    Q    Q    P    P    E    P    I   → Q5 P3 E1 I1
        List<TierFinalDisposition> records = List.of(
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.PENALIZED),
                d(FinalTierState.PENALIZED, FinalTierState.PENALIZED),
                d(FinalTierState.PENALIZED, FinalTierState.EXEMPTED),
                d(FinalTierState.EXEMPTED, FinalTierState.PENALIZED),
                d(FinalTierState.INVALID, FinalTierState.INVALID));

        RegulationStatistics.Summary s = RegulationStatistics.summarize(records, 300_000.0);

        Assertions.assertEquals(10, s.fast().totalCommands(), "固定分母=发令总次数");
        Assertions.assertEquals(6, s.fast().qualifiedCount());
        Assertions.assertEquals(2, s.fast().penalizedCount());
        Assertions.assertEquals(1, s.fast().exemptCount());
        Assertions.assertEquals(1, s.fast().invalidCount());
        Assertions.assertEquals(60.0, s.fast().disclosedRatePct(), EPS);
        Assertions.assertEquals(20.0, s.fast().penaltyShortfallPct(), EPS);
        Assertions.assertEquals(12.0, s.fast().penaltyScore(), EPS);

        Assertions.assertEquals(10, s.econ().totalCommands(), "两档同分母");
        Assertions.assertEquals(5, s.econ().qualifiedCount());
        Assertions.assertEquals(3, s.econ().penalizedCount());
        Assertions.assertEquals(1, s.econ().exemptCount());
        Assertions.assertEquals(1, s.econ().invalidCount());
        Assertions.assertEquals(50.0, s.econ().disclosedRatePct(), EPS);
        Assertions.assertEquals(30.0, s.econ().penaltyShortfallPct(), EPS);
        Assertions.assertEquals(18.0, s.econ().penaltyScore(), EPS);

        Assertions.assertEquals(30.0, s.totalPenaltyScore(), EPS, "总罚款=两档相加");
    }

    @Test
    void exemptStaysInDenominator_dragsDisclosedRate()
    {
        // 免考不剔分母：4 免考 0 合格 → 披露 0%（非 100%）；⚠ 罚款仍为 0（免于考核落在罚金层）
        List<TierFinalDisposition> records = List.of(
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED),
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED),
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED),
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED));
        RegulationStatistics.Summary s = RegulationStatistics.summarize(records, 300_000.0);
        Assertions.assertEquals(4, s.fast().totalCommands());
        Assertions.assertEquals(0.0, s.fast().disclosedRatePct(), EPS, "全免考核算 0 分子/全量分母");
        Assertions.assertEquals(0.0, s.fast().penaltyScore(), EPS, "⚠ 推荐读法：免考点不产生罚金");
    }

    @Test
    void invalidDragsDownDisclosedRate_butNotPenaltyChain()
    {
        // INVALID 计入分母拖低披露合格率：1 合格 + 1 不可判、零罚款 → 披露仅 50%（非 100%）
        List<TierFinalDisposition> records = List.of(
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.INVALID, FinalTierState.INVALID));
        RegulationStatistics.Summary s = RegulationStatistics.summarize(records, 300_000.0);
        Assertions.assertEquals(50.0, s.econ().disclosedRatePct(), EPS,
                "1 合格 / 2 分母（INVALID 在分母）→ 50% 而非 100%");
        Assertions.assertEquals(0, s.econ().penalizedCount());
        Assertions.assertEquals(0.0, s.econ().penaltyShortfallPct(), EPS,
                "⚠ 罚款缺额链只数真罚（现行实现，丙类处置落地后再议）");
    }

    // ── ⚠ 两条独立链路不可互推（推荐读法）──

    @Test
    void twoChains_notDerivableFromEachOther()
    {
        // 同一披露合格率（1/3 ≈ 33.33%）对应不同罚款缺额 → 知其一推不出其二
        List<TierFinalDisposition> withExempt = List.of(
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.PENALIZED, FinalTierState.PENALIZED),
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED));
        List<TierFinalDisposition> withInvalid = List.of(
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.INVALID, FinalTierState.INVALID),
                d(FinalTierState.INVALID, FinalTierState.INVALID));

        RegulationStatistics.Summary a = RegulationStatistics.summarize(withExempt, 300_000.0);
        RegulationStatistics.Summary b = RegulationStatistics.summarize(withInvalid, 300_000.0);

        Assertions.assertEquals(a.fast().disclosedRatePct(), b.fast().disclosedRatePct(), EPS,
                "两数据集披露合格率相同");
        Assertions.assertNotEquals(a.fast().penaltyShortfallPct(), b.fast().penaltyShortfallPct(),
                "⚠ 缺额不同（33.33% vs 0%）→ 披露合格率推不出罚款缺额");
        Assertions.assertTrue(a.fast().disclosedRatePct() + a.fast().penaltyShortfallPct() < 100.0,
                "有免考时两链之和 < 100%，缺口 = 免考+不可判占比");
    }

    // ── 边界 ──

    @Test
    void emptyStream_allZeros()
    {
        // 约定：无指令日报表侧自行决定 NULL/0 呈现；纯函数返回 0 不产 NaN
        RegulationStatistics.Summary s = RegulationStatistics.summarize(List.of(), 300_000.0);
        Assertions.assertEquals(0, s.fast().totalCommands());
        Assertions.assertEquals(0.0, s.fast().disclosedRatePct(), EPS);
        Assertions.assertEquals(0.0, s.econ().penaltyScore(), EPS);
        Assertions.assertEquals(0.0, s.totalPenaltyScore(), EPS);
    }

    @Test
    void singleQualified_perfect()
    {
        RegulationStatistics.Summary s = RegulationStatistics.summarize(
                List.of(d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED)), 300_000.0);
        Assertions.assertEquals(100.0, s.fast().disclosedRatePct(), EPS);
        Assertions.assertEquals(0.0, s.fast().penaltyShortfallPct(), EPS);
    }

    @Test
    void zeroCapacity_zeroFine_ratesUnchanged()
    {
        List<TierFinalDisposition> records = List.of(d(FinalTierState.PENALIZED, FinalTierState.PENALIZED));
        RegulationStatistics.Summary s = RegulationStatistics.summarize(records, 0.0);
        Assertions.assertEquals(0.0, s.fast().penaltyScore(), EPS);
        Assertions.assertEquals(100.0, s.fast().penaltyShortfallPct(), EPS, "罚款为 0 不影响缺额率");
    }

    @Test
    void negativeOrNaNCapacity_throws()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RegulationStatistics.summarize(List.of(), -1.0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RegulationStatistics.summarize(List.of(), Double.NaN));
    }

    // ────────────────── S4 读侧：计数版入口（与逐条版同源公式） ──────────────────

    @Test
    void summarizeCounts_handVector_matchesRecordVersion()
    {
        // total=5, q=3, p=1, exempt=1：rate=60%，shortfall=penalized/total=20%（剔免考）
        // 罚款 = 20×30万千瓦×0.02 = 12 分
        RegulationStatistics.TierRateResult r = RegulationStatistics.summarizeCounts(
                5, 3, 1, 1, 0, 300_000.0);
        Assertions.assertEquals(60.0, r.disclosedRatePct(), EPS);
        Assertions.assertEquals(20.0, r.penaltyShortfallPct(), EPS);
        Assertions.assertEquals(12.0, r.penaltyScore(), EPS);

        // 与逐条版交叉验证：econ 档分布 Q2/P1/E1/I1 ↔ 计数版同参
        List<TierFinalDisposition> records = List.of(
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.QUALIFIED, FinalTierState.PENALIZED),
                d(FinalTierState.QUALIFIED, FinalTierState.QUALIFIED),
                d(FinalTierState.EXEMPTED, FinalTierState.EXEMPTED),
                d(FinalTierState.INVALID, FinalTierState.INVALID));
        RegulationStatistics.TierRateResult viaRecords = RegulationStatistics
                .summarize(records, 300_000.0).econ();
        RegulationStatistics.TierRateResult viaCounts = RegulationStatistics.summarizeCounts(
                5, 2, 1, 1, 1, 300_000.0);
        Assertions.assertEquals(viaRecords.penaltyScore(), viaCounts.penaltyScore(), EPS,
                "两入口同源");
        Assertions.assertEquals(viaRecords.penaltyShortfallPct(),
                viaCounts.penaltyShortfallPct(), EPS);
    }

    @Test
    void summarizeCounts_zeroTotal_zeroesNoNaN()
    {
        RegulationStatistics.TierRateResult r = RegulationStatistics.summarizeCounts(
                0, 0, 0, 0, 0, 300_000.0);
        Assertions.assertEquals(0.0, r.disclosedRatePct(), EPS);
        Assertions.assertEquals(0.0, r.penaltyShortfallPct(), EPS);
        Assertions.assertEquals(0.0, r.penaltyScore(), EPS);
    }
}
