package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.time.api.DateTimes;

/**
 * D3 属性测试（测试方案 §2.1/§4.3：jqwik 断言不变量，示例锚点见 MinuteRounderTest）。
 *
 * <p>生成域：1600-01-01 ~ 2999-12-31 任意时间（保证 4 位年份域内往返）；每属性默认 1000 样本。</p>
 */
class MinuteRounderPropertyTest
{
    private static final DateTimeFormatter TEXT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSSSS");

    @Provide
    Arbitrary<LocalDateTime> times()
    {
        return DateTimes.dateTimes()
                .between(LocalDateTime.of(1600, 1, 1, 0, 0), LocalDateTime.of(2999, 12, 31, 23, 59, 59, 999_999_999));
    }

    /** §4.3 属性①：幂等 round(round(t)) = round(t)（v5.0 §5 双重取整无害的依据） */
    @Property
    void prop_幂等(@ForAll("times") LocalDateTime t)
    {
        LocalDateTime once = MinuteRounder.round(t);
        Assertions.assertEquals(once, MinuteRounder.round(once));
    }

    /** §4.3 属性②：单调 t1 ≤ t2 ⇒ round(t1) ≤ round(t2) */
    @Property
    void prop_单调(@ForAll("times") LocalDateTime t1, @ForAll @LongRange(min = 0, max = 86399) long plusSeconds)
    {
        LocalDateTime t2 = t1.plusSeconds(plusSeconds);
        Assertions.assertFalse(MinuteRounder.round(t2).isBefore(MinuteRounder.round(t1)),
                () -> "违反单调: t1=" + t1 + " t2=" + t2);
    }

    /** §4.3 属性③：取整后秒/纳秒归零 */
    @Property
    void prop_秒纳秒归零(@ForAll("times") LocalDateTime t)
    {
        LocalDateTime rounded = MinuteRounder.round(t);
        Assertions.assertEquals(0, rounded.getSecond());
        Assertions.assertEquals(0, rounded.getNano());
    }

    /** 0~59 秒全空间 × 任意纳秒：当且仅当秒≥30 进位（Leo 2026-08-21 评审要求） */
    @Property
    void prop_全秒空间_当且仅当秒大于等于30进位(
            @ForAll @IntRange(min = 0, max = 59) int second,
            @ForAll @IntRange(min = 0, max = 999_999_999) int nano)
    {
        LocalDateTime t = LocalDateTime.of(2026, 8, 21, 12, 34, second, nano);
        LocalDateTime floor = t.truncatedTo(ChronoUnit.MINUTES);
        Assertions.assertEquals(second >= 30 ? floor.plusMinutes(1) : floor, MinuteRounder.round(t),
                () -> "second=" + second + " nano=" + nano);
    }

    /** 往返：任意时间 → 原文格式 → parseAndRound == round（STRICT 解析器全空间不丢不改） */
    @Property
    void prop_原文往返_解析取整等价(@ForAll("times") LocalDateTime t)
    {
        Assertions.assertEquals(MinuteRounder.round(t), MinuteRounder.parseAndRound(t.format(TEXT)),
                () -> "往返失败: " + t);
    }
}
