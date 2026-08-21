package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * D3 单测：就近取整到分钟（秒≥30 进位 / &lt;30 舍去 / 跨分跨小时及日/月/年边界 / 幂等 / 原文解析）。
 * 纯单元测试，不依赖数据库。
 */
class MinuteRounderTest
{
    @Test
    void assert_秒30整_进位()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 35, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 12, 34, 30, 0)));
    }

    @Test
    void assert_秒59带毫秒_进位()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 35, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 12, 34, 59, 999_000_000)));
    }

    @Test
    void assert_秒29带大毫秒_舍去()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 34, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 12, 34, 29, 999_999_999)));
    }

    @Test
    void assert_秒0整_舍去不变()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 34, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 12, 34, 0, 0)));
    }

    @Test
    void assert_跨分边界_59分30秒进到整点()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 13, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 12, 59, 30, 123)));
    }

    @Test
    void assert_跨小时跨日边界()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 22, 0, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 8, 21, 23, 59, 30, 500)));
    }

    @Test
    void assert_跨月跨年边界()
    {
        Assertions.assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 12, 31, 23, 59, 30, 0)));
    }

    @Test
    void assert_闰年2月末边界()
    {
        Assertions.assertEquals(LocalDateTime.of(2028, 2, 29, 0, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2028, 2, 28, 23, 59, 59, 999)));
        Assertions.assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0, 0),
                MinuteRounder.round(LocalDateTime.of(2026, 2, 28, 23, 59, 30, 0)));
    }

    @Test
    void assert_幂等_双重取整不变()
    {
        LocalDateTime raw = LocalDateTime.of(2026, 8, 21, 12, 34, 45, 678);
        LocalDateTime once = MinuteRounder.round(raw);
        Assertions.assertEquals(once, MinuteRounder.round(once));
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 35, 0), once);
    }

    @Test
    void assert_parse_空格分隔带毫秒()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 34, 56, 789_000_000),
                MinuteRounder.parse("2026-08-21 12:34:56.789"));
    }

    @Test
    void assert_parse_T分隔无毫秒_首尾空白容忍()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 34, 56, 0),
                MinuteRounder.parse("  2026-08-21T12:34:56  "));
    }

    @Test
    void assert_parse_非法原文返回null()
    {
        Assertions.assertNull(MinuteRounder.parse(null));
        Assertions.assertNull(MinuteRounder.parse("  "));
        Assertions.assertNull(MinuteRounder.parse("2026-08-21"));
        Assertions.assertNull(MinuteRounder.parse("2026-08-2112:34:56"));
        Assertions.assertNull(MinuteRounder.parse("2026-13-01 12:00:00"));
        Assertions.assertNull(MinuteRounder.parse("2026-08-21 25:00:00"));
        Assertions.assertNull(MinuteRounder.parse("not-a-time 12:00:00"));
        Assertions.assertNull(MinuteRounder.parse("2026-08-21 12:00:00.1234567890"));
    }

    @Test
    void assert_parse_不可能日期与24点_拒绝且不钳位改值()
    {
        // SMART 解析器曾把这几个静默钳位成合法时间（2027-02-29→02-28、04-31→04-30、02-30→02-28、24:00→次日00:00）
        Assertions.assertNull(MinuteRounder.parse("2027-02-29 12:00:00"));
        Assertions.assertNull(MinuteRounder.parse("2026-04-31 12:00:00"));
        Assertions.assertNull(MinuteRounder.parse("2026-02-30 12:00:00"));
        Assertions.assertNull(MinuteRounder.parse("2026-08-21 24:00:00"));
        Assertions.assertNull(MinuteRounder.parseAndRound("2027-02-29 12:00:00"));
    }

    @Test
    void assert_parse_尾随裸小数点_与正则契约一致拒绝()
    {
        Assertions.assertNull(MinuteRounder.parse("2026-08-21 12:00:00."));
    }

    @Test
    void assert_parse_真闰日合法()
    {
        Assertions.assertEquals(LocalDateTime.of(2028, 2, 29, 12, 0, 0),
                MinuteRounder.parse("2028-02-29 12:00:00"));
    }

    @Test
    void assert_parseAndRound_一步()
    {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 34, 0),
                MinuteRounder.parseAndRound("2026-08-21 12:34:29.999"));
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 21, 12, 35, 0),
                MinuteRounder.parseAndRound("2026-08-21 12:34:30"));
        Assertions.assertNull(MinuteRounder.parseAndRound("garbage"));
    }
}
