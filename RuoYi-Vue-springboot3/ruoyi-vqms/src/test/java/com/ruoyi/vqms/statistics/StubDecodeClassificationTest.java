package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * D9 L0：StubRegulationJudge 契约级行为——解码分类（三类归因）+ 窗口结构统计 + 确定性。
 *
 * <p>判定结论断言只锁「stub 占位值 = QUALIFIED」这一实现事实（S1 替换后本类相应调整），
 * <b>不锁包络判定正确性</b>——那是 S1 的 oracle（manifest）。</p>
 *
 * <p>⚠️ UNVERIFIED-ASSUMPTION 用例（2026-08-19 评审吸收约定）：循环码非法分支的合成场景
 * 零覆盖（合成样本第 2 位全=2），以下标 ⚠ 的用例为<b>测试代码自造输入</b>，验证的是
 * 「对该错误分支行为的假设」——{0..5} 值域为 Leo 2026-08-19 拍板、轮转规律待生产数据实证；
 * 生产数据验证语义后须统一复核全部带标用例。</p>
 */
class StubDecodeClassificationTest
{
    private final StubRegulationJudge judge = new StubRegulationJudge();

    private static final JudgeParams PARAMS = new JudgeParams(4, 5);

    private static List<MinuteCurve> fullWindow()
    {
        return List.of(new MinuteCurve(1, 225, 222), new MinuteCurve(2, 225, 222),
                new MinuteCurve(3, 225, 222), new MinuteCurve(4, 225, 222),
                new MinuteCurve(5, 225, 222));
    }

    private AvcCommand cmd(String text, Double realtimeKv)
    {
        return new AvcCommand("2026-03-15 10:00:00.000", "000", 0L, text, realtimeKv);
    }

    private RegulationOutcome judgeOf(String text, Double realtimeKv)
    {
        return judge.judge(cmd(text, realtimeKv), fullWindow(), PARAMS);
    }

    // ── 可解码形态 ──

    @Test
    void targetForm_decodable()
    {
        // S01 口径：'...目标值,22315.' → 结构可解（数值计算随 S1）
        RegulationOutcome o = judgeOf("收到远方遥调执行指令:主省220KV目标值,22315.", null);
        Assertions.assertInstanceOf(RegulationOutcome.Judged.class, o,
                "目标值形态不需要 t₀ 电压");
    }

    @Test
    void incrementForm_decodable()
    {
        RegulationOutcome o = judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 234.25);
        Assertions.assertInstanceOf(RegulationOutcome.Judged.class, o);
    }

    // ── 编码脏写 ──

    @Test
    void s12_anchor_garbageText_corruptedEncoding()
    {
        // S12 锚点（合成场景）：',abc.' 目标值形态尾码非数字
        RegulationOutcome o = judgeOf("收到远方遥调执行指令:主省220KV目标值,abc.", null);
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING), o);
    }

    @Test
    void noTrailingCode_corruptedEncoding()
    {
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                judgeOf("收到远方遥调执行指令:无尾码文本", 234.0));
    }

    @Test
    void allLetters_corruptedEncoding()
    {
        // 策略文档 §4.1 例 #10：'ABCD' = 脏写（非循环码——数字槽位全坏）
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,ABCD.", 234.0));
    }

    @Test
    void wrongLength_corruptedEncoding()
    {
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,12345.", 234.0));
    }

    @Test
    void illegalDirection_corruptedEncoding()
    {
        // ⚠ UNVERIFIED-ASSUMPTION：方向槽 '9' 非法（合法 {1,2}）——自造输入
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,9102.", 234.0));
    }

    @Test
    void nullText_corruptedEncoding()
    {
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                judgeOf(null, 234.0));
    }

    // ── 循环码非法（⚠ 全部自造输入，UNVERIFIED-ASSUMPTION）──

    @Test
    void cycleCode_X_illegal()
    {
        // ⚠ UNVERIFIED-ASSUMPTION + 策略文档 §4.1 例 #4/#8 口径：'9X02'/'1X02' 循环码位坏 →
        // 归因循环码非法（双向坏时循环码优先于方向）
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,1X02.", 233.5));
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,9X02.", 234.0));
    }

    @Test
    void cycleCode_six_illegal()
    {
        // ⚠ UNVERIFIED-ASSUMPTION：越界 6（合法值域 {0..5}，Leo 2026-08-19；轮转待实证）——自造输入
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2602.", 234.0));
    }

    @Test
    void cycleCode_boundaryValues_decodable()
    {
        // ⚠ UNVERIFIED-ASSUMPTION：边界 0/5 合法（自造输入；合成样本第 2 位全=2）
        for (char c : new char[] {'0', '5'})
        {
            String text = "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2" + c + "02.";
            Assertions.assertInstanceOf(RegulationOutcome.Judged.class,
                    judgeOf(text, 234.25), "循环码 " + c + " 应合法");
        }
    }

    // ── 缺 t₀ 电压 ──

    @Test
    void s11_anchor_incrementNoRealtime_missingT0Voltage()
    {
        // S11 锚点（合成场景）：增量形态 + realtime=null
        Assertions.assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE),
                judgeOf("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", null));
    }

    // ── 窗口结构统计 ──

    @Test
    void s16_anchor_invertedInterval_tierInvalid()
    {
        // S16 锚点：快速窗 low>high → FAST ∈ invalidTiers、fast VERDICT 空；econ 正常判（stub=QUALIFIED）
        List<MinuteCurve> inverted = List.of(
                new MinuteCurve(1, 224, 225), new MinuteCurve(2, 224, 225),
                new MinuteCurve(3, 224, 225), new MinuteCurve(4, 224, 225),
                new MinuteCurve(5, 225, 222));
        RegulationOutcome o = judge.judge(cmd("收到远方遥调执行指令:主省220KV目标值,22315.", null),
                inverted, PARAMS);
        RegulationOutcome.Judged j = Assertions.assertInstanceOf(RegulationOutcome.Judged.class, o);
        Assertions.assertEquals(java.util.Set.of(Tier.FAST), j.invalidTiers());
        Assertions.assertTrue(j.fast().isEmpty(), "无效档 VERDICT 必空（不变式）");
        Assertions.assertTrue(j.econ().isPresent());
    }

    @Test
    void s14_anchor_wholeFastWindowMissing_tierInvalid_completenessReported()
    {
        // S14 锚点：快速窗整档全缺（分钟 1-4 无数据）→ FAST 无效；completeness 如实 = 1/5
        List<MinuteCurve> econOnly = List.of(new MinuteCurve(5, 225, 222));
        RegulationOutcome.Judged j = Assertions.assertInstanceOf(RegulationOutcome.Judged.class,
                judge.judge(cmd("收到远方遥调执行指令:主省220KV目标值,22315.", null), econOnly, PARAMS));
        Assertions.assertEquals(java.util.Set.of(Tier.FAST), j.invalidTiers());
        Assertions.assertEquals(0.2, j.completeness(), 1e-9);
    }

    @Test
    void partialMissing_completenessFraction()
    {
        // S13 口径：部分缺分钟只进 completeness（4/5），不影响判定
        List<MinuteCurve> partial = List.of(new MinuteCurve(1, 225, 222), new MinuteCurve(2, 225, 222),
                new MinuteCurve(4, 225, 222), new MinuteCurve(5, 225, 222));
        RegulationOutcome.Judged j = Assertions.assertInstanceOf(RegulationOutcome.Judged.class,
                judge.judge(cmd("收到远方遥调执行指令:主省220KV目标值,22315.", null), partial, PARAMS));
        Assertions.assertEquals(0.8, j.completeness(), 1e-9);
        Assertions.assertTrue(j.invalidTiers().isEmpty());
    }

    @Test
    void stubMarker_and_stubVerdicts()
    {
        Assertions.assertTrue(judge.isStub());
        RegulationOutcome.Judged j = Assertions.assertInstanceOf(RegulationOutcome.Judged.class,
                judgeOf("收到远方遥调执行指令:主省220KV目标值,22315.", null));
        // stub 占位结论：有效档一律 QUALIFIED（确定性；不保证正确，S1 替换）
        Assertions.assertEquals(Optional.of(Verdict.QUALIFIED), j.fast());
        Assertions.assertEquals(Optional.of(Verdict.QUALIFIED), j.econ());
    }

    @Test
    void deterministic_sameInputSameOutcome()
    {
        AvcCommand c = cmd("收到远方遥调执行指令:主省220KV目标值,22315.", null);
        Assertions.assertEquals(judge.judge(c, fullWindow(), PARAMS), judge.judge(c, fullWindow(), PARAMS));
        AvcCommand inc = cmd("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 234.25);
        Assertions.assertEquals(judge.judge(inc, fullWindow(), PARAMS), judge.judge(inc, fullWindow(), PARAMS));
    }

    @Test
    void judgeParams_validation()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JudgeParams(0, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JudgeParams(5, 4));
    }

}
