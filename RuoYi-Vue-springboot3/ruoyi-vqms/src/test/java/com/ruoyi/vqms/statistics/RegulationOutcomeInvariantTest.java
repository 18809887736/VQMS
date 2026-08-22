package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * D9 L0：RegulationOutcome.Judged 构造期不变式「档 VERDICT 为空 ⟺ 该档 ∈ invalidTiers」。
 *
 * <p>每档 4 种状态（有值×∉ / 有值×∈ / 空×∈ / 空×∉）叉乘 = 16 种组合：
 * 恰 4 种合法（构造成功）、12 种抛 IllegalArgumentException。有限离散空间直接
 * {@code @ParameterizedTest} 枚举，无需属性生成（测试方案 §4.8）。</p>
 */
class RegulationOutcomeInvariantTest
{
    /** 单档状态：verdict 是否在场 / 是否标记无效；合法 ⟺ 在场与无效互斥 */
    private enum TierState
    {
        PRESENT_VALID(true, false),
        PRESENT_INVALID(true, true),
        EMPTY_INVALID(false, true),
        EMPTY_VALID(false, false);

        final boolean present;
        final boolean inInvalidTiers;

        TierState(boolean present, boolean inInvalidTiers)
        {
            this.present = present;
            this.inInvalidTiers = inInvalidTiers;
        }

        /** 合法 ⟺ VERDICT 为空 ⟺ 该档 ∈ invalidTiers */
        boolean legal()
        {
            return present != inInvalidTiers;
        }

        Optional<Verdict> verdict()
        {
            return present ? Optional.of(Verdict.QUALIFIED) : Optional.empty();
        }
    }

    private static RegulationOutcome.Judged build(TierState fast, TierState econ)
    {
        java.util.Set<Tier> invalid = java.util.EnumSet.noneOf(Tier.class);
        if (fast.inInvalidTiers) { invalid.add(Tier.FAST); }
        if (econ.inInvalidTiers) { invalid.add(Tier.ECON); }
        return new RegulationOutcome.Judged(fast.verdict(), econ.verdict(), 1.0, invalid);
    }

    @ParameterizedTest(name = "{index}: fast={0}")
    @EnumSource(TierState.class)
    void fastAxis_matrixHolds(TierState fast)
    {
        // 快速性档轴 × 合法经济性档：构造成功 ⟺ fast 满足不变式
        TierState legalEcon = TierState.PRESENT_VALID;
        if (fast.legal())
        {
            RegulationOutcome.Judged j = Assertions.assertDoesNotThrow(() -> build(fast, legalEcon));
            Assertions.assertEquals(fast.present ? Optional.of(Verdict.QUALIFIED) : Optional.empty(),
                    j.fast());
        }
        else
        {
            Assertions.assertThrows(IllegalArgumentException.class, () -> build(fast, legalEcon));
        }
    }

    @ParameterizedTest(name = "{index}: econ={0}")
    @EnumSource(TierState.class)
    void econAxis_matrixHolds(TierState econ)
    {
        // 经济性档轴 × 合法快速性档：构造成功 ⟺ econ 满足不变式
        TierState legalFast = TierState.EMPTY_INVALID;
        if (econ.legal())
        {
            RegulationOutcome.Judged j = Assertions.assertDoesNotThrow(() -> build(legalFast, econ));
            Assertions.assertEquals(econ.verdict(), j.econ());
        }
        else
        {
            Assertions.assertThrows(IllegalArgumentException.class, () -> build(legalFast, econ));
        }
    }

    @Test
    void fullCrossProduct_exactly4Legal12Illegal()
    {
        int legalCount = 0;
        for (TierState f : TierState.values())
        {
            for (TierState e : TierState.values())
            {
                boolean constructed;
                try
                {
                    build(f, e);
                    constructed = true;
                }
                catch (IllegalArgumentException expected)
                {
                    constructed = false;
                }
                Assertions.assertEquals(f.legal() && e.legal(), constructed,
                        "fast=" + f + ", econ=" + e + " 的可构性与不变式不符");
                legalCount += constructed ? 1 : 0;
            }
        }
        Assertions.assertEquals(4, legalCount, "恰 4 种合法（16 - 12 非法）");
    }

    @Test
    void anchor_s16Shape_constructs()
    {
        // 锚点：S16 场景形态——fast 无效（空+∈invalidTiers）+ econ 正常（有值+∉）
        RegulationOutcome.Judged j = new RegulationOutcome.Judged(
                Optional.empty(), Optional.of(Verdict.QUALIFIED), 1.0, Set.of(Tier.FAST));
        Assertions.assertTrue(j.fast().isEmpty());
        Assertions.assertEquals(Set.of(Tier.FAST), j.invalidTiers());
    }

    @Test
    void anchor_bothInvalid_zeroCompleteness_constructs()
    {
        // 整窗全缺双档：completeness=0 × 无归因（非 Undecodable）是乙档主战场合法输入（测试方案 §4.8）
        RegulationOutcome.Judged j = new RegulationOutcome.Judged(
                Optional.empty(), Optional.empty(), 0.0, Set.of(Tier.FAST, Tier.ECON));
        Assertions.assertEquals(0.0, j.completeness());
    }

    @Test
    void completenessBounds_enforced()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new RegulationOutcome.Judged(Optional.of(Verdict.QUALIFIED),
                        Optional.of(Verdict.QUALIFIED), -0.01, Set.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new RegulationOutcome.Judged(Optional.of(Verdict.QUALIFIED),
                        Optional.of(Verdict.QUALIFIED), 1.01, Set.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new RegulationOutcome.Judged(Optional.of(Verdict.QUALIFIED),
                        Optional.of(Verdict.QUALIFIED), Double.NaN, Set.of()));
    }

    @Test
    void undecodable_nullReason_throws()
    {
        Assertions.assertThrows(NullPointerException.class,
                () -> new RegulationOutcome.Undecodable(null));
    }
}
