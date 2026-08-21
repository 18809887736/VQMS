package com.ruoyi.vqms.source.reader;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * D4 锚点用例（属性测试的补充，非替代；属性见 SaveTimeGatePropertyTest）。
 * 三步各自独立断言：① 放宽边界粗筛；② 正则校验；③ 解析后精确过滤。
 */
class SaveTimeGateTest
{
    private static final String START = "2026-08-21 10:00:00";

    private static final String END = "2026-08-21 10:05:00";

    private static List<String> filter(List<String> rawTimes)
    {
        return SaveTimeGate.filter(new ArrayList<>(rawTimes), s -> s, START, END);
    }

    // ---------- ① 放宽边界粗筛 ----------

    @Test
    void assert_expand_区间前后各扩1分钟()
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(START, END);
        Assertions.assertEquals("2026-08-21 09:59:00", bounds.start());
        Assertions.assertEquals("2026-08-21 10:06:00", bounds.end());
    }

    @Test
    void assert_expand_跨日跨年边界()
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds("2026-01-01 00:00:00", "2026-01-01 00:02:00");
        Assertions.assertEquals("2025-12-31 23:59:00", bounds.start());
        Assertions.assertEquals("2026-01-01 00:03:00", bounds.end());
    }

    @Test
    void assert_expand_非法边界快速失败()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> SaveTimeGate.expandBounds("garbage", END));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> SaveTimeGate.expandBounds(START, "2027-02-29 12:00:00"));
    }

    // ---------- ② 正则校验（坏行拦截不拖垮整批） ----------

    @Test
    void assert_坏行拦截_好行不受影响()
    {
        List<String> kept = filter(List.of(
                START + ".000",
                "not-a-time",
                "2027-02-29 12:00:00",
                "2026-08-21 12:00:00.",
                END + ".123"));
        Assertions.assertEquals(List.of(START + ".000", END + ".123"), kept);
    }

    @Test
    void assert_全坏行返回空且不抛()
    {
        Assertions.assertTrue(filter(java.util.Arrays.asList("x", null, "")).isEmpty());
    }

    @Test
    void assert_空列表平安通过()
    {
        Assertions.assertTrue(filter(List.of()).isEmpty());
    }

    // ---------- ③ 解析后精确过滤（边界分钟不丢行 + 多读剔除） ----------

    @Test
    void assert_边界分钟不丢行_秒30进位纳入()
    {
        // 09:59:30.123 取整到 10:00 ∈ 目标区间 → 必须保留（SQL 粗筛原 between 会把它算进来，但语义上属目标首分钟）
        Assertions.assertEquals(List.of("2026-08-21 09:59:30.123"),
                filter(List.of("2026-08-21 09:59:30.123")));
    }

    @Test
    void assert_边界分钟_秒29舍去剔除()
    {
        // 09:59:29.999 取整到 09:59 ∉ 目标区间 → 剔除（目标区间精确过滤，不因粗筛多读而混入）
        Assertions.assertTrue(filter(List.of("2026-08-21 09:59:29.999")).isEmpty());
    }

    @Test
    void assert_区间两端闭含()
    {
        List<String> kept = filter(List.of("2026-08-21 10:00:00.000", "2026-08-21 10:05:00.000"));
        Assertions.assertEquals(2, kept.size());
    }

    @Test
    void assert_区间尾部秒30进位越界剔除()
    {
        // 10:05:30 取整到 10:06 > END → 剔除（粗筛多读的行在此剔除）
        Assertions.assertTrue(filter(List.of("2026-08-21 10:05:30.000")).isEmpty());
    }

    @Test
    void assert_原始时间戳原样保留()
    {
        String raw = "2026-08-21 09:59:45.067";
        Assertions.assertEquals(List.of(raw), filter(List.of(raw)), "返回的须是原文，不得取整改写");
    }
}
