package com.ruoyi.vqms.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1 L0：DefaultRegulationJudge 判定逻辑（正式 v1_0 §2.4~2.5；测试方案 §5.0）。
 *
 * <p>两档平行真值表、invalidTiers 两种成因映射、completeness 如实、Undecodable 透传、
 * 确定性。19 场景 oracle 全量准入见 S1ScenarioContractIT（§4.2 manifest 映射）。</p>
 */
class DefaultRegulationJudgeTest
{
    private static final JudgeParams PARAMS = new JudgeParams(4, 5);

    private static final AvcCommand TARGET_CMD = new AvcCommand(
            "2026-03-23 09:59:59", "100", 0L,
            "收到远方遥调执行指令:主省220KV目标值,22315.", null); // V_target = 223.15

    private final DefaultRegulationJudge judge = new DefaultRegulationJudge();

    /** [1..tEcon] 连续完整窗，每分钟一行 (high, low)。 */
    private static List<MinuteCurve> window(double... highLowPairs)
    {
        if (highLowPairs.length % 2 != 0)
        {
            throw new IllegalArgumentException("须成对");
        }
        List<MinuteCurve> curves = new ArrayList<>();
        for (int i = 0; i < highLowPairs.length; i += 2)
        {
            curves.add(new MinuteCurve(i / 2 + 1, (int) highLowPairs[i], (int) highLowPairs[i + 1]));
        }
        return curves;
    }

    // ────────────────── 两档平行真值表（fast 行偏移 1..、econ 行偏移 5，t_fast=4 布局） ──────────────────

    private static MinuteCurve row(int offset, int high, int low)
    {
        return new MinuteCurve(offset, high, low);
    }

    @Test
    void bothTiersBracketed_qualifiedBoth()
    {
        RegulationOutcome.Judged j = judged(List.of(
                row(1, 225, 222), row(2, 224, 223), row(3, 224, 223), row(4, 225, 223),
                row(5, 225, 223)));                   // 全窗 [1..5] 齐整；fast/econ 均夹住
        assertEquals(Optional.of(Verdict.QUALIFIED), j.fast());
        assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
        assertTrue(j.invalidTiers().isEmpty());
        assertEquals(1.0, j.completeness(), 1e-9);
    }

    @Test
    void fastMiss_econHit_penalizedFast()
    {
        RegulationOutcome.Judged j = judged(List.of(
                row(1, 222, 220),                     // fast 包络 [220,222] 在目标下方，不夹
                row(5, 225, 223)));                   // econ 包络 [223,225] 夹住
        assertEquals(Optional.of(Verdict.PENALIZED), j.fast());
        assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
    }

    @Test
    void fastHit_econMiss_penalizedEcon()
    {
        RegulationOutcome.Judged j = judged(List.of(
                row(1, 224, 223),                     // fast 包络 [223,224] 夹住
                row(5, 228, 227)));                   // econ 包络 [227,228] 漂走不夹
        assertEquals(Optional.of(Verdict.QUALIFIED), j.fast());
        assertEquals(Optional.of(Verdict.PENALIZED), j.econ());
    }

    @Test
    void bothTiersMiss_penalizedBoth()
    {
        RegulationOutcome.Judged j = judged(List.of(row(1, 222, 220), row(5, 222, 220)));
        assertEquals(Optional.of(Verdict.PENALIZED), j.fast());
        assertEquals(Optional.of(Verdict.PENALIZED), j.econ());
    }

    @Test
    void boundaryInclusive_touchingEnvelope_qualified()
    {
        // V_target 落在包络下界：目标 223.00（尾码 22300），包络 [223,225] 含 223 → 合格（闭区间）
        AvcCommand v223 = new AvcCommand(TARGET_CMD.warnTime(), "100", 0L,
                TARGET_CMD.warnContent().replace("22315", "22300"), null);
        RegulationOutcome.Judged j = (RegulationOutcome.Judged)
                judge.judge(v223, List.of(row(1, 225, 223), row(5, 225, 223)), PARAMS);
        assertEquals(Optional.of(Verdict.QUALIFIED), j.fast());
    }

    @Test
    void doubleArithmeticTail_doesNotFlipIntegerBoundary()
    {
        // 回归哨兵：增量 2202@234.8 → 数学上恰 235.0；无 2 位小数收口时
        // double 的 234.8+0.2=235.00000000000003 会把 ≤235 边界档误判为越界
        AvcCommand cmd = new AvcCommand("t", "0", 0L,
                "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 234.8);
        RegulationOutcome.Judged j = (RegulationOutcome.Judged) judge.judge(cmd, window(235, 233), PARAMS);
        assertEquals(Optional.of(Verdict.QUALIFIED), j.fast(), "235.0 ∈ [233,235] 应合格");
    }

    // ────────────────── invalidTiers 两种成因 ──────────────────

    @Test
    void wholeTierMissing_invalidTier_verdictEmpty()
    {
        // S14 口径：fast 窗整档缺（只给 econ 行）→ fast 进 invalidTiers、VERDICT 空
        List<MinuteCurve> econOnly = List.of(new MinuteCurve(5, 225, 222));
        RegulationOutcome.Judged j = judged(econOnly);
        assertEquals(Set.of(Tier.FAST), j.invalidTiers());
        assertTrue(j.fast().isEmpty(), "无效档 VERDICT 必空（不变式）");
        assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
        assertEquals(0.2, j.completeness(), 1e-9);
    }

    @Test
    void lowGreaterThanHigh_invalidTier_econNormal()
    {
        // S16 口径：fast 窗 L>H 数据异常 → 该档不可判；econ 窗正常照判
        List<MinuteCurve> curves = List.of(
                new MinuteCurve(1, 10, 20),   // fast 窗：high=10 < low=20
                new MinuteCurve(5, 225, 222));
        RegulationOutcome.Judged j = judged(curves);
        assertEquals(Set.of(Tier.FAST), j.invalidTiers());
        assertTrue(j.fast().isEmpty(), "无效档 VERDICT 必空（构造期不变式）");
        assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
    }

    @Test
    void bothTiersInvalid_bothVerdictsEmpty()
    {
        // 整窗全缺 → completeness=0 且两档全 invalid（合法可达状态，非矛盾组合）
        RegulationOutcome.Judged j = judged(List.of());
        assertEquals(Set.of(Tier.FAST, Tier.ECON), j.invalidTiers());
        assertTrue(j.fast().isEmpty() && j.econ().isEmpty());
        assertEquals(0.0, j.completeness(), 1e-9);
    }

    // ────────────────── Undecodable 透传 + 其他契约 ──────────────────

    @Test
    void undecodable_passthrough_withReason()
    {
        AvcCommand badCycle = new AvcCommand("t", "0", 0L,
                "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2602.", 234.25);
        assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID),
                judge.judge(badCycle, window(225, 222), PARAMS),
                "解码失败与窗口数据无关——窗口齐全照样判不了");

        AvcCommand missingT0 = new AvcCommand("t", "0", 0L,
                "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", null);
        assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE),
                judge.judge(missingT0, window(225, 222), PARAMS));
    }

    @Test
    void partialMissing_completenessFraction_noInvalid()
    {
        // 缺第 3 分钟：4/5 如实上报，部分缺不走 invalidTiers
        List<MinuteCurve> withGap = new ArrayList<>();
        for (int o = 1; o <= 5; o++)
        {
            if (o != 3)
            {
                withGap.add(new MinuteCurve(o, 225, 222));
            }
        }
        RegulationOutcome.Judged j = judged(withGap);
        assertTrue(j.invalidTiers().isEmpty());
        assertEquals(0.8, j.completeness(), 1e-9);
    }

    @Test
    void tFastVariation_windowsFollowParam()
    {
        // t_fast 可整定 [1,5)：取 2 时 fast=[1,2]、econ=[3,5]，无缝拼接无重叠
        JudgeParams p = new JudgeParams(2, 5);
        List<MinuteCurve> curves = new ArrayList<>();
        curves.add(new MinuteCurve(1, 222, 220));   // fast 窗全在目标下方
        curves.add(new MinuteCurve(2, 222, 220));   // fast 包络 [220,222] 不夹 223.15 → PEN
        curves.add(new MinuteCurve(3, 225, 223));   // econ 包络 [222,225] 夹住 → QUALIFIED
        curves.add(new MinuteCurve(4, 224, 222));
        curves.add(new MinuteCurve(5, 224, 223));
        RegulationOutcome.Judged j = (RegulationOutcome.Judged) judge.judge(TARGET_CMD, curves, p);
        assertEquals(Optional.of(Verdict.PENALIZED), j.fast());
        assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
    }

    @Test
    void determinism_sameInputSameOutput()
    {
        List<MinuteCurve> curves = window(226, 224, 226, 223);
        assertEquals(judge.judge(TARGET_CMD, curves, PARAMS),
                judge.judge(TARGET_CMD, curves, PARAMS), "禁随机：同输入两次判定相等");
    }

    @Test
    void isStub_false()
    {
        assertFalse(judge.isStub());
    }

    private RegulationOutcome.Judged judged(List<MinuteCurve> curves)
    {
        return (RegulationOutcome.Judged) judge.judge(TARGET_CMD, curves, PARAMS);
    }
}
