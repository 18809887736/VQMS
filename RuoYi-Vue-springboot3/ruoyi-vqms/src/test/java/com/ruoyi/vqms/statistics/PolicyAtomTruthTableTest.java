package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 戊·自由组合 L0：原子真值表（策略文档 §3.3.1 清单逐条对照）。
 *
 * <p>事实构造覆盖三轴：Undecodable 三归因 / Judged 档无效 / Judged 部分缺 / 齐全正常。
 * A4 边界承现行乙档方向（可用度 =τ 不触发，⚠ UNVERIFIED 随政策钉死）。</p>
 */
class PolicyAtomTruthTableTest
{
    private static final int TAU = 50;

    private static RegulationOutcome.Undecodable undecodable(DecodeFailureReason reason)
    {
        return new RegulationOutcome.Undecodable(reason);
    }

    private static RegulationOutcome.Judged judged(double completeness, Tier... invalid)
    {
        Optional<Verdict> fast = Set.of(invalid).contains(Tier.FAST)
                ? Optional.empty() : Optional.of(Verdict.QUALIFIED);
        Optional<Verdict> econ = Set.of(invalid).contains(Tier.ECON)
                ? Optional.empty() : Optional.of(Verdict.PENALIZED);
        return new RegulationOutcome.Judged(fast, econ, completeness, Set.of(invalid));
    }

    @Test
    void a1_family_trueOnlyForMatchingDecodeReason()
    {
        RegulationOutcome dirty = undecodable(DecodeFailureReason.CORRUPTED_ENCODING);
        assertTrue(PolicyAtom.A1.eval(dirty, TAU));
        assertTrue(PolicyAtom.A1A.eval(dirty, TAU));
        assertFalse(PolicyAtom.A1B.eval(dirty, TAU));
        assertFalse(PolicyAtom.A1C.eval(dirty, TAU));

        RegulationOutcome cycle = undecodable(DecodeFailureReason.CYCLE_CODE_INVALID);
        assertTrue(PolicyAtom.A1.eval(cycle, TAU));
        assertTrue(PolicyAtom.A1B.eval(cycle, TAU));
        assertFalse(PolicyAtom.A1A.eval(cycle, TAU));

        RegulationOutcome noT0 = undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE);
        assertTrue(PolicyAtom.A1C.eval(noT0, TAU));
        assertFalse(PolicyAtom.A1A.eval(noT0, TAU));
        // MECE：三子类合取 ≡ A1——任一 Undecodable 下恰一子类为真
        for (RegulationOutcome o : new RegulationOutcome[] {dirty, cycle, noT0})
        {
            assertTrue(PolicyAtom.A1.eval(o, TAU), "子类为真则 A1 为真");
            assertEquals(1, (PolicyAtom.A1A.eval(o, TAU) ? 1 : 0)
                    + (PolicyAtom.A1B.eval(o, TAU) ? 1 : 0)
                    + (PolicyAtom.A1C.eval(o, TAU) ? 1 : 0), "恰一子类为真");
        }
    }

    @Test
    void a2_trueOnlyWhenInvalidTiersPresent()
    {
        assertTrue(PolicyAtom.A2.eval(judged(0.6, Tier.FAST), TAU),
                "快档全缺 → invalidTiers 含 FAST");
        assertFalse(PolicyAtom.A2.eval(judged(0.6), TAU), "无档无效 → A2 假");
    }

    @Test
    void a3_trueWheneverCompletenessBelowOne_evenWithInvalidTiers()
    {
        assertTrue(PolicyAtom.A3.eval(judged(0.4, Tier.FAST), TAU),
                "跨档并存：档无效且部分缺（§3.3.1 A2×A3）");
        assertFalse(PolicyAtom.A3.eval(judged(1.0), TAU), "齐全 → 假");
    }

    @Test
    void a4_thresholdBoundary_inclusiveUsableCountsAsNormal()
    {
        // completeness 0.5 × 100 = 50 = τ → 不低于阈值 → A4 假（≥阈值=正常记账）
        assertFalse(PolicyAtom.A4.eval(judged(0.5), TAU), "边界 =τ 不触发");
        assertTrue(PolicyAtom.A4.eval(judged(0.49), TAU));
        assertFalse(PolicyAtom.A4.eval(judged(1.0), TAU), "齐全时 A4 恒假");
    }

    @Test
    void decodeFailure_shortCircuitsDataAxes()
    {
        RegulationOutcome u = undecodable(DecodeFailureReason.CORRUPTED_ENCODING);
        assertFalse(PolicyAtom.A2.eval(u, TAU), "Undecodable 不携带窗口信息 → A2/A3/A4 假");
        assertFalse(PolicyAtom.A3.eval(u, TAU));
        assertFalse(PolicyAtom.A4.eval(u, TAU));
    }

    @Test
    void axisClassification_matchesPolicyConfigThreeKeys()
    {
        assertEquals(PolicyAtom.Axis.UNDECODABLE, PolicyAtom.A1.axis());
        assertEquals(PolicyAtom.Axis.UNDECODABLE, PolicyAtom.A1C.axis());
        assertEquals(PolicyAtom.Axis.INVALID_TIER, PolicyAtom.A2.axis());
        assertEquals(PolicyAtom.Axis.PARTIAL_MISSING, PolicyAtom.A3.axis());
        assertEquals(PolicyAtom.Axis.PARTIAL_MISSING, PolicyAtom.A4.axis());
    }
}
