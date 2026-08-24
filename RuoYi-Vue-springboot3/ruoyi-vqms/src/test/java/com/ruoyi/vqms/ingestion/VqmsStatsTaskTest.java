package com.ruoyi.vqms.ingestion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.management.mapper.VqmsStatsRollupMapper;
import com.ruoyi.vqms.statistics.RegulationPipeline;
import com.ruoyi.vqms.statistics.RuntimePipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S4 Slice3 L0：VqmsStatsTask 委托与区间解析（Quartz 入口，sys_job invokeTarget 按 bean 名）。
 */
class VqmsStatsTaskTest
{
    private final RegulationPipeline regulationPipeline = mock(RegulationPipeline.class);
    private final RuntimePipeline runtimePipeline = mock(RuntimePipeline.class);
    private final VqmsStatsRollupMapper rollupMapper = mock(VqmsStatsRollupMapper.class);

    private final VqmsStatsTask task =
            new VqmsStatsTask(regulationPipeline, runtimePipeline, rollupMapper);

    @Test
    void recomputeRange_cascadesInOrder()
    {
        task.recomputeRange("2026-03-20", "2026-03-21");

        verify(regulationPipeline).recompute(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 21));
        verify(runtimePipeline).recomputeDay(LocalDate.of(2026, 3, 20));
        verify(runtimePipeline).recomputeDay(LocalDate.of(2026, 3, 21));
        String s = "2026-03-20";
        String e = "2026-03-21";
        verify(rollupMapper).rollupRegulationDaily(s, e);
        verify(rollupMapper).rollupRegulationMonthly(s, e);
        verify(rollupMapper).rollupRegulationYearly(s, e);
        verify(rollupMapper).rollupRuntimeMonthly(s, e);
        verify(rollupMapper).rollupRuntimeYearly(s, e);
    }

    @Test
    void recomputeYesterday_delegatesYesterdayOnly()
    {
        String yesterday = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        task.recomputeYesterday();
        verify(regulationPipeline).recompute(LocalDate.parse(yesterday), LocalDate.parse(yesterday));
        verify(runtimePipeline).recomputeDay(LocalDate.parse(yesterday));
        verify(rollupMapper).rollupRegulationDaily(yesterday, yesterday);
    }

    @Test
    void invertedRange_throws()
    {
        assertThrows(IllegalArgumentException.class,
                () -> task.recomputeRange("2026-03-21", "2026-03-20"));
    }

    @Test
    void runtimeDayOnly_delegatesSingleDay()
    {
        task.recomputeRuntimeDay("2026-03-23");
        verify(runtimePipeline).recomputeDay(eq(LocalDate.of(2026, 3, 23)));
        verify(rollupMapper, never()).rollupRegulationDaily(any(), any());
    }
}
