package com.ruoyi.vqms.statistics;

import java.util.Optional;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1 L0：VTargetDecoder 单元 + 属性（测试方案 §5.0 组件矩阵）。
 *
 * <p>两形态全分支（正式 v1_0 §2.1 例）+ 循环码 {0..5} 值域/越界 = 非法 +
 * 缺 t₀；属性：目标值形态编码→解码<b>往返可逆</b>、增量形态同输入同 t₀ 输出恒定。</p>
 */
class VTargetDecoderTest
{
    private static final String TARGET_TEXT = "收到远方遥调执行指令:主省220KV目标值,22315.";
    private static final String INCREMENT_ADD_TEXT =
            "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.";
    private static final double T0 = 234.25;

    private static AvcCommand cmd(String text, Double t0Kv)
    {
        return new AvcCommand("2026-03-23 09:59:59", "100", 0L, text, t0Kv);
    }

    // ────────────────── 目标值形态 ──────────────────

    @Test
    void targetForm_dividesBy100()
    {
        assertEquals(Optional.empty(), VTargetDecoder.classify(cmd(TARGET_TEXT, null)));
        assertEquals(223.15, VTargetDecoder.decode(cmd(TARGET_TEXT, null)), 1e-12);
    }

    @Test
    void targetForm_t0Irrelevant_targetIgnoresRealtime()
    {
        // 目标值形态不依赖 t₀ 实时电压（正式版 §2.1）
        assertEquals(Optional.empty(), VTargetDecoder.classify(cmd(TARGET_TEXT, 999.0)));
        assertEquals(223.15, VTargetDecoder.decode(cmd(TARGET_TEXT, 999.0)), 1e-12);
    }

    @Test
    void targetForm_nonNumericTail_corrupted()
    {
        assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                VTargetDecoder.classify(cmd("收到远方遥调执行指令:主省220KV目标值,abc.", null)));
    }

    // ────────────────── 增量值形态 ──────────────────

    @Test
    void incrementAdd_plusDeltaOnT0()
    {
        // 正式版 §2.1 例：2202 = +200V；234.25 + 0.2 → 234.45 kV
        assertEquals(Optional.empty(), VTargetDecoder.classify(cmd(INCREMENT_ADD_TEXT, T0)));
        assertEquals(234.45, VTargetDecoder.decode(cmd(INCREMENT_ADD_TEXT, T0)), 1e-12);
    }

    @Test
    void incrementSubtract_minusDeltaOnT0()
    {
        // S10 口径：1202@234.25 → 234.05 kV
        double v = VTargetDecoder.decode(
                cmd(INCREMENT_ADD_TEXT.replace("2202", "1202"), T0));
        assertEquals(234.05, v, 1e-12);
    }

    /** ⚠ UNVERIFIED-ASSUMPTION：循环码轮转规律待生产数据实证（Leo 2026-08-19 仅拍板值域 {0..5}）。 */
    @Test
    void increment_cycleCode_allSixLegal_ignoredNumerically()
    {
        for (char c = '0'; c <= '5'; c++)
        {
            final char cycle = c;
            String text = INCREMENT_ADD_TEXT.replace("2202", "2" + cycle + "02");
            assertEquals(Optional.empty(), VTargetDecoder.classify(cmd(text, T0)),
                    () -> "循环码 " + cycle + " 合法且不参与数值");
            assertEquals(234.45, VTargetDecoder.decode(cmd(text, T0)), 1e-12,
                    () -> "循环码 " + cycle + " 不改变 V_target");
        }
    }

    /** ⚠ UNVERIFIED-ASSUMPTION：越界用例自造（6~9 / 非数字 → 循环码非法），生产数据验证后复核。 */
    @Test
    void increment_cycleCode_outOfRange_invalid()
    {
        for (String code : new String[] {"2602", "2902", "2702"})
        {
            assertEquals(Optional.of(DecodeFailureReason.CYCLE_CODE_INVALID),
                    VTargetDecoder.classify(cmd(INCREMENT_ADD_TEXT.replace("2202", code), T0)),
                    () -> code + " 第 2 位 ∉ {0..5}");
        }
        assertEquals(Optional.of(DecodeFailureReason.CYCLE_CODE_INVALID),
                VTargetDecoder.classify(cmd("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2a02.", T0)),
                "第 2 位非数字 → 循环码非法");
    }

    /** '9X02' 类双向坏按循环码非法归因（策略文档 §4.1 例 #4，校验顺序脏写→循环码→方向→t₀）。 */
    @Test
    void increment_doubleBad_cycleCodeWins()
    {
        assertEquals(Optional.of(DecodeFailureReason.CYCLE_CODE_INVALID),
                VTargetDecoder.classify(cmd("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,9X02.", T0)));
    }

    @Test
    void increment_directionIllegal_corrupted()
    {
        // 方向合法仅 1=减 / 2=加（'3202'：槽位数字全对、循环码合法、方向越界 → 脏写）
        assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                VTargetDecoder.classify(cmd("收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,3202.", T0)));
    }

    @Test
    void increment_lengthNot4_orNonDigitSlot_corrupted()
    {
        String base = "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,%s.";
        assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                VTargetDecoder.classify(cmd(String.format(base, "202"), T0)), "长度 3");
        assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                VTargetDecoder.classify(cmd(String.format(base, "12345"), T0)), "长度 5");
        assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                VTargetDecoder.classify(cmd(String.format(base, "ABCD"), T0)), "第 1/3/4 位非数字");
    }

    @Test
    void increment_missingT0_missingT0Voltage()
    {
        assertEquals(Optional.of(DecodeFailureReason.MISSING_T0_VOLTAGE),
                VTargetDecoder.classify(cmd(INCREMENT_ADD_TEXT, null)));
    }

    // ────────────────── 结构脏写通用 ──────────────────

    @Test
    void blankOrNullText_orNoTrailingCode_corrupted()
    {
        for (String text : new String[] {null, "", "   ", "收到远方遥调执行指令无尾码"})
        {
            assertEquals(Optional.of(DecodeFailureReason.CORRUPTED_ENCODING),
                    VTargetDecoder.classify(cmd(text, T0)),
                    () -> "文本[" + text + "]应归因编码脏写");
        }
    }

    // ────────────────── 属性（jqwik，§2.1 差分②前置基线） ──────────────────

    /** 目标值形态编码→解码往返可逆：任意 2 位小数 kV 值 ×100 入码、÷100 出码恒还原。 */
    @Property
    void targetForm_roundTrip_reversible(
            @ForAll @DoubleRange(min = 60.0, max = 1000.0) double rawKv)
    {
        double kv = Math.round(rawKv * 100.0) / 100.0; // 收口到 2 位小数（源数据精度）
        long encoded = Math.round(kv * 100);
        String text = "收到远方遥调执行指令:主省220KV目标值," + encoded + ".";
        assertEquals(kv, VTargetDecoder.decode(cmd(text, null)), 1e-9,
                () -> "往返可逆: " + encoded + " → " + kv);
    }

    /** 增量形态纯确定性：同输入同 t₀ 输出恒定（零门槛纯度校验）。 */
    @Property
    void increment_deterministic_sameInputSameT0(
            @ForAll @DoubleRange(min = 200.0, max = 240.0) double rawT0)
    {
        Double t0 = Math.round(rawT0 * 100.0) / 100.0;
        AvcCommand c = cmd(INCREMENT_ADD_TEXT, t0);
        assertEquals(VTargetDecoder.decode(c), VTargetDecoder.decode(c), 1e-12);
    }
}
