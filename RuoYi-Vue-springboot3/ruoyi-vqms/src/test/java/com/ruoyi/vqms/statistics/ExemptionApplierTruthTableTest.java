package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * S2 L0：ExemptionApplier 真值表（测试方案 §5.0，正式 v1_0 §2.6 apply_exemption）。
 *
 * <p>逐档规则：PENALIZED ∧ yx501=1 → EXEMPTED；PENALIZED ∧ yx501=0 → PENALIZED；
 * QUALIFIED / invalidTiers 档透传；两档独立、结论不跨档。</p>
 *
 * <p>类型堵（编译期保证，无运行时用例）：签名只收 {@code RegulationOutcome.Judged}，
 * {@code Undecodable} 无法传入本组件——其分流归 Pipeline 集成断言（§5.0）。</p>
 */
class ExemptionApplierTruthTableTest
{
    private static RegulationOutcome.Judged judged(Optional<Verdict> fast, Optional<Verdict> econ)
    {
        java.util.Set<Tier> invalid = new java.util.HashSet<>();
        if (fast.isEmpty())
        {
            invalid.add(Tier.FAST);
        }
        if (econ.isEmpty())
        {
            invalid.add(Tier.ECON);
        }
        return new RegulationOutcome.Judged(fast, econ, 1.0, invalid);
    }

    private static FinalTierState applyFast(Verdict v, int yx501)
    {
        Optional<Verdict> f = v == null ? Optional.empty() : Optional.of(v);
        Optional<Verdict> e = Optional.of(Verdict.QUALIFIED);
        return ExemptionApplier.apply(judged(f, e), yx501).fast();
    }

    // ── 单档真值表（每档 3 状态 × 旗 {0,1}，两档对称各验一遍）──

    @ParameterizedTest
    @CsvSource({
            "QUALIFIED, 0, QUALIFIED",
            "QUALIFIED, 1, QUALIFIED",
            "PENALIZED, 0, PENALIZED",
            "PENALIZED, 1, EXEMPTED",
    })
    void truthTable_judgedTier(Verdict v, int yx501, FinalTierState expected)
    {
        Assertions.assertEquals(expected, applyFast(v, yx501), "快速档");
        Assertions.assertEquals(expected, applyEcon(v, yx501), "经济性档");
    }

    private static FinalTierState applyEcon(Verdict v, int yx501)
    {
        Optional<Verdict> f = Optional.of(Verdict.QUALIFIED);
        Optional<Verdict> e = v == null ? Optional.empty() : Optional.of(v);
        return ExemptionApplier.apply(judged(f, e), yx501).econ();
    }

    @ParameterizedTest
    @CsvSource({ "0", "1" })
    void invalidTier_passthrough_regardlessOfFlag(int yx501)
    {
        // invalidTiers 档透传：VERDICT 为空 → INVALID，免考旗不触及
        Assertions.assertEquals(FinalTierState.INVALID, applyFast(null, yx501));
        Assertions.assertEquals(FinalTierState.INVALID, applyEcon(null, yx501));
    }

    // ── 两档独立：全组合矩阵（3 状态 × 3 状态 × 旗 {0,1} = 18 行），结论不跨档 ──

    @ParameterizedTest
    @CsvSource({
            "QUALIFIED, QUALIFIED, 0, QUALIFIED, QUALIFIED",
            "QUALIFIED, QUALIFIED, 1, QUALIFIED, QUALIFIED",
            "QUALIFIED, PENALIZED, 0, QUALIFIED, PENALIZED",
            "QUALIFIED, PENALIZED, 1, QUALIFIED, EXEMPTED",
            "PENALIZED, QUALIFIED, 0, PENALIZED, QUALIFIED",
            "PENALIZED, QUALIFIED, 1, EXEMPTED, QUALIFIED",
            "PENALIZED, PENALIZED, 0, PENALIZED, PENALIZED",
            "PENALIZED, PENALIZED, 1, EXEMPTED, EXEMPTED",
    })
    void bothTiersJudged_independent(Verdict f, Verdict e, int yx501,
            FinalTierState expFast, FinalTierState expEcon)
    {
        TierFinalDisposition out = ExemptionApplier.apply(judged(Optional.of(f), Optional.of(e)), yx501);
        Assertions.assertEquals(expFast, out.fast());
        Assertions.assertEquals(expEcon, out.econ());
    }

    @ParameterizedTest
    @CsvSource({
            "QUALIFIED, 0", "QUALIFIED, 1", "PENALIZED, 0", "PENALIZED, 1",
    })
    void oneInvalidOneJudged_independent(Verdict judgedSide, int yx501)
    {
        // 一档无效 + 另档正常判：无效档 INVALID、有效档按自身结论，互不影响
        Optional<Verdict> present = Optional.of(judgedSide);
        Optional<Verdict> absent = Optional.empty();

        TierFinalDisposition out = ExemptionApplier.apply(
                new RegulationOutcome.Judged(absent, present, 1.0, Set.of(Tier.FAST)), yx501);
        Assertions.assertEquals(FinalTierState.INVALID, out.fast());
        Assertions.assertEquals(judgedSide == Verdict.PENALIZED && yx501 == 1
                ? FinalTierState.EXEMPTED : FinalTierState.valueOf(judgedSide.name()), out.econ());

        TierFinalDisposition mirrored = ExemptionApplier.apply(
                new RegulationOutcome.Judged(present, absent, 1.0, Set.of(Tier.ECON)), yx501);
        Assertions.assertEquals(FinalTierState.INVALID, mirrored.econ());
        Assertions.assertEquals(judgedSide == Verdict.PENALIZED && yx501 == 1
                ? FinalTierState.EXEMPTED : FinalTierState.valueOf(judgedSide.name()), mirrored.fast());
    }

    // ── 输入校验 ──

    @Test
    void yx501_outOfDomain_throws()
    {
        RegulationOutcome.Judged j = judged(Optional.of(Verdict.PENALIZED), Optional.of(Verdict.PENALIZED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ExemptionApplier.apply(j, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ExemptionApplier.apply(j, -1));
    }

    @Test
    void nullJudged_throws()
    {
        Assertions.assertThrows(NullPointerException.class, () -> ExemptionApplier.apply(null, 1));
    }
}
