package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.source.model.YcHistory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1 L0：YxSignalReader 阶跃保持选取语义（正式 v1_0 §1.2；测试方案 §5.0）。
 *
 * <p>纯函数 {@code selectHeld} 单测——取 ≤ 当分钟最近一条、未来行不取、
 * 坏时间原文跳过、点前无数据 → empty。合成库五类点 IT（联调）另行覆盖读取全链。</p>
 */
class YxSignalReaderHeldStepTest
{
    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 23, 10, 0);

    private static YcHistory row(String ycTime, double value)
    {
        YcHistory y = new YcHistory();
        y.setYcTime(ycTime);
        y.setYcData(value);
        return y;
    }

    @Test
    void noRows_empty()
    {
        assertEquals(Optional.empty(), YxSignalReaderImpl.selectHeld(List.of(), AT));
    }

    @Test
    void allRowsAfterAtMinute_empty()
    {
        // 10:00:30 就近取整进位到 10:01 > AT；10:05 更晚——两行都不取
        assertEquals(Optional.empty(), YxSignalReaderImpl.selectHeld(
                List.of(row("2026-03-23 10:00:30.000", 1), row("2026-03-23 10:05:00.000", 0)), AT));
    }

    @Test
    void picksLatestRowAtOrBefore_minuteRounding()
    {
        // :59.9 取整进位到 10:00 → 属于 ≤AT 的最新行（就近取整语义）
        Optional<Integer> held = YxSignalReaderImpl.selectHeld(List.of(
                row("2026-03-23 09:58:00.000", 0),
                row("2026-03-23 09:59:59.900", 1),
                row("2026-03-23 10:00:30.000", 0)), AT);
        assertEquals(Optional.of(1), held);
    }

    @Test
    void exactMinuteRow_inclusive()
    {
        assertEquals(Optional.of(1), YxSignalReaderImpl.selectHeld(
                List.of(row("2026-03-23 10:00:00.123", 1)), AT), "恰在当分钟的行计入（≤ 语义）");
    }

    @Test
    void unorderedRows_latestWins()
    {
        assertEquals(Optional.of(0), YxSignalReaderImpl.selectHeld(List.of(
                row("2026-03-23 09:55:00.000", 1),
                row("2026-03-23 09:57:00.000", 0)), AT));
    }

    @Test
    void dirtyTimestamp_skipped_notFatal()
    {
        assertEquals(Optional.of(1), YxSignalReaderImpl.selectHeld(List.of(
                row("not-a-time", 0),
                row("2026-03-23 09:58:00.000", 1)), AT), "坏行跳过不致命");
    }
}
