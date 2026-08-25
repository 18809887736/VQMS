package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 戊·自由组合 L0：求值器（首中即断 / 兜底 / 子类分发 / NOT-组语义）。
 */
class FreeformPolicyEvaluatorTest
{
    private static final RegulationOutcome UNDECODABLE_CYCLE =
            new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID);
    private static final RegulationOutcome UNDECODABLE_T0 =
            new RegulationOutcome.Undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE);
    private static final RegulationOutcome INVALID_PARTIAL = judged(0.4, Tier.FAST);
    private static final RegulationOutcome COMPLETE = judged(1.0);

    private static RegulationOutcome.Judged judged(double completeness, Tier... invalid)
    {
        Optional<Verdict> fast = Set.of(invalid).contains(Tier.FAST)
                ? Optional.empty() : Optional.of(Verdict.QUALIFIED);
        Optional<Verdict> econ = Set.of(invalid).contains(Tier.ECON)
                ? Optional.empty() : Optional.of(Verdict.PENALIZED);
        return new RegulationOutcome.Judged(fast, econ, completeness, Set.of(invalid));
    }

    private static FreeformRule rule(String line)
    {
        return FreeformPolicyParser.parseRule(line);
    }

    @Test
    void firstMatchWins_orderedSemantics()
    {
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(List.of(
                rule("A3 -> COUNT_UNQUALIFIED"),
                rule("A1 -> EXCLUDE_REPORTED")), 50);
        // 同一输入同时满足 A2×A3 跨档并存——表序决定落桶（首中即断）
        assertEquals(Disposition.COUNT_UNQUALIFIED,
                FreeformPolicyEvaluator.evaluate(INVALID_PARTIAL, cfg).disposition());
    }

    @Test
    void decodeSubtypeDispatch_viaRuleOrder()
    {
        // 子类分发（§4.2 归因口径）：循环码非法=编码方案问题挂起待人工；其余解码失败剔除+计数。
        // 注意 COUNT_NORMAL 不可用于解码轴——无判定结论可「正常记账」（校验器同口径拦截）
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(List.of(
                rule("A1B -> PEND_MARKED").withRuleId("R001"),
                rule("A1 -> EXCLUDE_REPORTED").withRuleId("R002")), 50);
        assertEquals(new FreeformPolicyEvaluator.Decision(Disposition.PEND_MARKED, "R001"),
                FreeformPolicyEvaluator.evaluate(UNDECODABLE_CYCLE, cfg));
        assertEquals(new FreeformPolicyEvaluator.Decision(Disposition.EXCLUDE_REPORTED, "R002"),
                FreeformPolicyEvaluator.evaluate(UNDECODABLE_T0, cfg));
    }

    @Test
    void fallbackCountNormal_withNullRuleId()
    {
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(List.of(rule("A1 -> PEND_MARKED")), 50);
        FreeformPolicyEvaluator.Decision d = FreeformPolicyEvaluator.evaluate(COMPLETE, cfg);
        assertEquals(Disposition.COUNT_NORMAL, d.disposition());
        assertNull(d.ruleId(), "兜底命中无规则 ID——留痕列消费口径");
    }

    @Test
    void notAndGroup_semantics()
    {
        // (A2 & !A3)：档无效但数据齐全才触发（跨档并存时放行给后续规则）
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(
                List.of(rule("(A2 & !A3) -> PEND_MARKED")), 50);
        assertEquals(Disposition.PEND_MARKED,
                FreeformPolicyEvaluator.evaluate(judged(1.0, Tier.ECON), cfg).disposition());
        assertEquals(Disposition.COUNT_NORMAL,
                FreeformPolicyEvaluator.evaluate(INVALID_PARTIAL, cfg).disposition(),
                "档无效且部分缺 → 组内 !A3 为假 → 不命中 → 兜底");
    }

    @Test
    void thresholdFlowsIntoA4()
    {
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(
                List.of(rule("A3 & A4 -> EXCLUDE_REPORTED")), 40);
        assertEquals(Disposition.EXCLUDE_REPORTED,
                FreeformPolicyEvaluator.evaluate(judged(0.39), cfg).disposition());
        assertEquals(Disposition.COUNT_NORMAL,
                FreeformPolicyEvaluator.evaluate(judged(0.40), cfg).disposition(),
                "可用度 =τ 边界不剔除（承现行乙档方向）");
    }
}
