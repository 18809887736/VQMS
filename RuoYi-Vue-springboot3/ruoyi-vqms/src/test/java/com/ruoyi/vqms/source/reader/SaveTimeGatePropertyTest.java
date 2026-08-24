package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.time.api.DateTimes;

/**
 * D4 属性断言（测试方案 §4.4）：
 * ① 粗筛窗口不变量——凡取整后落在目标区间的行，其原文必在放宽边界内（边界分钟不丢行的生成器证明）；
 * ② 合法格式恒通过；
 * ③ 非法格式恒拦截且不抛异常；
 * ④ 过滤恰好保留「取整后落在目标区间」的行（不多不少）。
 */
class SaveTimeGatePropertyTest
{
    private static final DateTimeFormatter MINUTE_TEXT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    @Provide
    Arbitrary<LocalDateTime> times()
    {
        return DateTimes.dateTimes()
                .between(LocalDateTime.of(1600, 1, 1, 0, 0), LocalDateTime.of(2999, 12, 31, 23, 59, 59, 999_999_999));
    }

    /** 任意时间 → 原文形态（毫秒位数 0~9 随机，模拟外部源变宽毫秒） */
    private static String toRaw(LocalDateTime t, int fractionDigits)
    {
        String base = t.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
        if (fractionDigits == 0)
        {
            return base;
        }
        long nanos = t.getNano();
        StringBuilder fraction = new StringBuilder(Long.toString(nanos));
        while (fraction.length() < 9)
        {
            fraction.insert(0, '0');
        }
        return base + "." + fraction.substring(0, fractionDigits);
    }

    // ---------- ① 粗筛窗口不变量 ----------

    /** row 取自窗口附近（偏移 [-2, window+2] 分钟），保证 in-window 分支真实命中 */
    @Property
    void prop_粗筛窗口包含所有目标行(@ForAll("times") LocalDateTime start, @ForAll @IntRange(min = 0, max = 1440) int windowMinutes,
            @ForAll @IntRange(min = -2, max = 1442) int rowOffsetMinutes)
    {
        LocalDateTime end = start.plusMinutes(windowMinutes);
        LocalDateTime row = start.plusMinutes(rowOffsetMinutes);
        String raw = toRaw(row, 3);
        if (!MinuteRounder.round(row).isBefore(start) && !MinuteRounder.round(row).isAfter(end))
        {
            SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(
                    start.format(MINUTE_TEXT), end.format(MINUTE_TEXT));
            Assertions.assertTrue(raw.compareTo(bounds.start()) >= 0 && raw.compareTo(bounds.end()) <= 0,
                    () -> "目标行被粗筛边界裁掉: raw=" + raw + " bounds=[" + bounds.start() + "," + bounds.end() + "]");
        }
    }

    // ---------- ② 合法格式恒通过 ----------

    @Property
    void prop_合法格式恒通过(@ForAll("times") LocalDateTime row, @ForAll @IntRange(min = 0, max = 9) int fractionDigits,
            @ForAll @IntRange(min = 0, max = 120) int beforeMinutes, @ForAll @IntRange(min = 0, max = 120) int afterMinutes)
    {
        LocalDateTime rounded = MinuteRounder.round(row);
        String start = rounded.minusMinutes(beforeMinutes).format(MINUTE_TEXT);
        String end = rounded.plusMinutes(afterMinutes).format(MINUTE_TEXT);
        String raw = toRaw(row, fractionDigits);
        Assertions.assertEquals(List.of(raw), SaveTimeGate.filter(List.of(raw), s -> s, start, end),
                () -> "合法行被误杀: raw=" + raw + " window=[" + start + "," + end + "]");
    }

    // ---------- ③ 非法格式恒拦截且不抛 ----------

    @Provide
    Arbitrary<String> corruptedTimes()
    {
        return times().flatMap(t -> {
            String base = toRaw(t, 3);
            int index = ThreadLocalRandom.current().nextInt(base.length());
            int mutation = ThreadLocalRandom.current().nextInt(4);
            String corrupted = switch (mutation)
            {
                case 0 -> base.substring(0, index) + "#" + base.substring(index + 1); // 换非法字符
                case 1 -> base.substring(0, index) + base.substring(index + 1); // 删字符
                case 2 -> base.substring(0, Math.max(index, 10)); // 截断成残串
                default -> base + "xyz"; // 尾部垃圾
            };
            return Arbitraries.of(corrupted);
        }).filter(s -> MinuteRounder.parse(s) == null); // 腐化可能碰巧仍是合法时间（如年份被改成闰年），只保留真非法串
    }

    @Property
    void prop_非法格式恒拦截且不抛(@ForAll("corruptedTimes") String corrupted, @ForAll("times") LocalDateTime start,
            @ForAll @IntRange(min = 0, max = 1440) int windowMinutes)
    {
        String end = start.plusMinutes(windowMinutes).format(MINUTE_TEXT);
        Assertions.assertTrue(SaveTimeGate.filter(List.of(corrupted), s -> s, start.format(MINUTE_TEXT), end).isEmpty(),
                () -> "非法串未被拦截: " + corrupted);
    }

    // ---------- ④ 恰好保留目标行 ----------

    @Property
    void prop_过滤恰好保留目标区间内的行(@ForAll("times") LocalDateTime start, @ForAll @IntRange(min = 0, max = 1440) int windowMinutes,
            @ForAll @IntRange(min = -2, max = 1442) int rowOffsetMinutes)
    {
        // 边界截到秒再喂两侧：闸门入参经 MINUTE_TEXT 文本天然丢亚秒，oracle 若用纳秒精度
        // 比较，边界行（如 start 带 1ns）会与实现判定错位（2026-08-24 S3 回归踩中，D4 潜伏修）
        LocalDateTime startSec = start.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        LocalDateTime end = startSec.plusMinutes(windowMinutes);
        LocalDateTime row = startSec.plusMinutes(rowOffsetMinutes);
        String raw = toRaw(row, 3);
        boolean inTarget = !MinuteRounder.round(row).isBefore(startSec) && !MinuteRounder.round(row).isAfter(end);
        List<String> kept = SaveTimeGate.filter(List.of(raw), s -> s, startSec.format(MINUTE_TEXT), end.format(MINUTE_TEXT));
        Assertions.assertEquals(inTarget ? 1 : 0, kept.size(),
                () -> "raw=" + raw + " rounded=" + MinuteRounder.round(row) + " window=[" + startSec + "," + end + "]");
    }
}
