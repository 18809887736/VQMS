package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * D9 L0 属性断言（jqwik，测试方案 §4.8 §2.1）：
 *
 * <ol>
 *   <li><b>纯确定性</b>：同输入同配置输出恒定；</li>
 *   <li><b>不重不漏</b>：任一输入在任一配置下恰好落一个处置桶——输入空间 =
 *       {@code RegulationOutcome} sealed 类型的不相交并
 *       （Judged{completeness∈[0,1], invalidTiers⊆{fast,econ}} ∪ Undecodable{三类归因}），
 *       不可达组合（如 Undecodable 带 completeness）由 sealed 结构性排除，无需生成器过滤。
 *       ⚠️ completeness=0 × 无归因是合法可达状态（乙档主战场），生成器不得排除。</li>
 * </ol>
 */
class PolicyFunctionPropertyTest
{
    /** Judged 的不变式满足态生成：每档 ∈ {有值∉, 空∈}（非法组合不可构造，由构造期强制） */
    @Provide
    private Arbitrary<RegulationOutcome> outcomes()
    {
        Arbitrary<Boolean> fastPresent = Arbitraries.of(true, false);
        Arbitrary<Boolean> econPresent = Arbitraries.of(true, false);
        Arbitrary<Double> completeness = Arbitraries.doubles().between(0.0, 1.0);
        return Combinators.combine(fastPresent, econPresent, completeness).as((fp, ep, comp) -> {
            Optional<Verdict> fast = fp ? Optional.of(Verdict.QUALIFIED) : Optional.empty();
            Optional<Verdict> econ = ep ? Optional.of(Verdict.PENALIZED) : Optional.empty();
            Set<Tier> invalid = new java.util.HashSet<>();
            if (!fp) { invalid.add(Tier.FAST); }
            if (!ep) { invalid.add(Tier.ECON); }
            return (RegulationOutcome) new RegulationOutcome.Judged(fast, econ, comp, invalid);
        }).edgeCases(config -> {
            // 边界显式注入：completeness 0 与 1、全缺/全齐
            config.add(new RegulationOutcome.Judged(Optional.empty(), Optional.empty(), 0.0,
                    Set.of(Tier.FAST, Tier.ECON)));
            config.add(new RegulationOutcome.Judged(Optional.of(Verdict.QUALIFIED),
                    Optional.of(Verdict.PENALIZED), 1.0, Set.of()));
        });
    }

    @Provide
    private Arbitrary<PolicyConfig> configs()
    {
        Arbitrary<Disposition> modes = Arbitraries.of(Disposition.values());
        // 阈值恒在场（1..100）——构造器只要求 EXCLUDE_REPORTED 时非 null，多给合法
        Arbitrary<Integer> threshold = Arbitraries.integers().between(1, 100);
        return Combinators.combine(modes, modes, modes, threshold)
                .as(PolicyConfig::new);
    }

    @Provide
    private Arbitrary<RegulationOutcome.Undecodable> undecodables()
    {
        return Arbitraries.of(DecodeFailureReason.values())
                .map(RegulationOutcome.Undecodable::new);
    }

    @Property(tries = 1000)
    void determinism_sameInputSameOutput(@ForAll("outcomes") RegulationOutcome outcome,
                                         @ForAll("configs") PolicyConfig config)
    {
        Assertions.assertEquals(DataUnavailabilityPolicy.evaluate(outcome, config),
                DataUnavailabilityPolicy.evaluate(outcome, config));
    }

    @Property(tries = 1000)
    void exactlyOneBucket_everyOutcomeEveryConfig(@ForAll("outcomes") RegulationOutcome outcome,
                                                  @ForAll("configs") PolicyConfig config)
    {
        Disposition d = DataUnavailabilityPolicy.evaluate(outcome, config);
        Assertions.assertNotNull(d, "必须落桶");
        Assertions.assertTrue(java.util.EnumSet.allOf(Disposition.class).contains(d), "桶 ∈ Disposition");
    }

    @Property(tries = 1000)
    void undecodable_branch_usesUndecodableMode(@ForAll("undecodables") RegulationOutcome.Undecodable u,
                                                @ForAll("configs") PolicyConfig config)
    {
        Assertions.assertEquals(config.undecodableMode(), DataUnavailabilityPolicy.evaluate(u, config));
    }

    @Property(tries = 1000)
    void judged_semantics_hold(@ForAll("outcomes") RegulationOutcome.Judged j,
                               @ForAll("configs") PolicyConfig config)
    {
        Disposition expected;
        if (!j.invalidTiers().isEmpty())
        {
            expected = config.invalidTierMode();
        }
        else if (j.completeness() < 1.0)
        {
            expected = config.partialMissingMode() == Disposition.EXCLUDE_REPORTED
                    ? (j.completeness() * 100 >= config.partialMissingThresholdPct()
                        ? Disposition.COUNT_NORMAL : Disposition.EXCLUDE_REPORTED)
                    : config.partialMissingMode();
        }
        else
        {
            expected = Disposition.COUNT_NORMAL;
        }
        Assertions.assertEquals(expected, DataUnavailabilityPolicy.evaluate(j, config));
    }
}
