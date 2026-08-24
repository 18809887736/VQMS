package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.domain.VqmsRuntimeDaily;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsRuntimeDailyMapper;
import com.ruoyi.vqms.source.model.YcHistory;
import com.ruoyi.vqms.source.reader.SourceReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S4 Slice2 L0：RuntimePipeline 编排（正式 v1_0 §1.3~§1.5）——mock 信号读取，真分类器/真统计。
 *
 * <p>覆盖：全投运日 100%、混合退出手算向量（罚款恰 5 分）、零并网日率列 NULL、
 * 容量缺失罚款 NULL、两母线原因组合从严（max）、并网 ≥10 边界。</p>
 */
class RuntimePipelineTest
{
    private static final LocalDate DAY = LocalDate.of(2026, 3, 23);
    /** 回看窗内的昨日正午——静态信号一行即可覆盖整日（阶跃保持） */
    private static final String HELD_TIME = "2026-03-22 12:00:00";

    private final SourceReader sourceReader = mock(SourceReader.class);
    private final VqmsBusbarGroupMapper groupMapper = mock(VqmsBusbarGroupMapper.class);
    private final VqmsRuntimeDailyMapper runtimeMapper = mock(VqmsRuntimeDailyMapper.class);

    private final RuntimePipeline pipeline =
            new RuntimePipeline(sourceReader, groupMapper, runtimeMapper);

    @BeforeEach
    void setUp()
    {
        VqmsBusbarGroup g = new VqmsBusbarGroup();
        g.setRatedCapacityKw(new BigDecimal("300000")); // 30 万千瓦
        when(groupMapper.selectList()).thenReturn(List.of(g));
    }

    private void signals(int grid, int avc, int exitMain, int exitAux)
    {
        when(sourceReader.readYc(anyString(), anyString(), eq(3009L)))
                .thenReturn(rows(avc));
        when(sourceReader.readYc(anyString(), anyString(), eq(511L)))
                .thenReturn(rows(grid));
        when(sourceReader.readYc(anyString(), anyString(), eq(512L)))
                .thenReturn(rows(grid));
        when(sourceReader.readYc(anyString(), anyString(), eq(521L)))
                .thenReturn(rows(exitMain));
        when(sourceReader.readYc(anyString(), anyString(), eq(522L)))
                .thenReturn(rows(exitAux));
    }

    private List<YcHistory> rows(double value)
    {
        YcHistory y = new YcHistory();
        y.setYcTime(HELD_TIME);
        y.setYcData(value);
        return List.of(y);
    }

    @Test
    void fullDayInService_rate100_noPenalty()
    {
        signals(12, 1, 0, 0);
        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertEquals(1440, r.inServiceMin());
        assertEquals(100.0, r.ratePct().doubleValue(), 1e-9);

        ArgumentCaptor<VqmsRuntimeDaily> captor = ArgumentCaptor.forClass(VqmsRuntimeDaily.class);
        verify(runtimeMapper).upsert(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getShortfallPct()));
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getPenaltyScore()));
    }

    @Test
    void mixedExit_handVector_penaltyExactly5()
    {
        // 手算向量：非电网退出 120 分钟（avc=0 且原因=2，00:00~01:59），02:00 起投入
        // rate = 1320/1440×100 = 91.667%；shortfall = 99−91.667 = 7.333 点；
        // 罚款 = 7.333×30万千瓦×0.02 = 恰 4.4 分
        when(sourceReader.readYc(anyString(), anyString(), eq(3009L))).thenReturn(
                List.of(row(HELD_TIME, 1), row("2026-03-23 00:00:00", 0),
                        row("2026-03-23 02:00:00", 1)));
        when(sourceReader.readYc(anyString(), anyString(), eq(521L))).thenReturn(
                List.of(row(HELD_TIME, 0), row("2026-03-23 00:00:00", 2)));
        when(sourceReader.readYc(anyString(), anyString(), eq(522L))).thenReturn(rows(0));
        when(sourceReader.readYc(anyString(), anyString(), eq(511L))).thenReturn(rows(12));
        when(sourceReader.readYc(anyString(), anyString(), eq(512L))).thenReturn(rows(12));

        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertEquals(1320, r.inServiceMin());
        assertEquals(120, r.exitNonGridMin());

        ArgumentCaptor<VqmsRuntimeDaily> captor = ArgumentCaptor.forClass(VqmsRuntimeDaily.class);
        verify(runtimeMapper).upsert(captor.capture());
        VqmsRuntimeDaily rowRow = captor.getValue();
        assertEquals(91.667, rowRow.getRatePct().doubleValue(), 0.001);
        assertEquals(7.333, rowRow.getShortfallPct().doubleValue(), 0.001);
        assertEquals(4.4, rowRow.getPenaltyScore().doubleValue(), 0.001);
    }

    @Test
    void zeroBase_ratesNull_notZero()
    {
        signals(0, 0, 0, 0); // 未并网整天
        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertEquals(1440, r.offlineMin());

        ArgumentCaptor<VqmsRuntimeDaily> captor = ArgumentCaptor.forClass(VqmsRuntimeDaily.class);
        verify(runtimeMapper).upsert(captor.capture());
        assertNull(captor.getValue().getRatePct(), "零并网基数=无可考核，率存 NULL 区别于真 0%");
        assertNull(captor.getValue().getPenaltyScore());
    }

    @Test
    void capacityMissing_penaltyNull_rateStillComputed()
    {
        when(groupMapper.selectList()).thenReturn(List.of(new VqmsBusbarGroup())); // 无容量配置
        signals(12, 1, 0, 0);
        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertTrue(r.capacityMissing());

        ArgumentCaptor<VqmsRuntimeDaily> captor = ArgumentCaptor.forClass(VqmsRuntimeDaily.class);
        verify(runtimeMapper).upsert(captor.capture());
        assertNotNull(captor.getValue().getRatePct(), "率不依赖容量，照算");
        assertNull(captor.getValue().getPenaltyScore(), "容量缺失不产罚款数");
    }

    @Test
    void dualBusbarReasonCombination_stricterWins()
    {
        // 正母报电网原因(1)、副母报非电网(2)：任一侧非电网证据即从严 → 计罚
        when(sourceReader.readYc(anyString(), anyString(), eq(3009L))).thenReturn(
                List.of(row(HELD_TIME, 1), row("2026-03-23 00:00:00", 0)));
        when(sourceReader.readYc(anyString(), anyString(), eq(521L))).thenReturn(
                List.of(row(HELD_TIME, 0), row("2026-03-23 00:00:00", 1)));
        when(sourceReader.readYc(anyString(), anyString(), eq(522L))).thenReturn(
                List.of(row(HELD_TIME, 0), row("2026-03-23 00:00:00", 2)));
        when(sourceReader.readYc(anyString(), anyString(), eq(511L))).thenReturn(rows(12));
        when(sourceReader.readYc(anyString(), anyString(), eq(512L))).thenReturn(rows(12));

        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertEquals(0, r.exitGridMin(), "两侧均电网才免责");
        assertEquals(1440, r.exitNonGridMin());
    }

    @Test
    void gridBoundary_atLeastTen_onGrid()
    {
        // 值语义 带电×10+机组数：10=带电无机组（并网）；9=不可能合法值但按 <10 未并网处理
        when(sourceReader.readYc(anyString(), anyString(), eq(3009L))).thenReturn(rows(1));
        when(sourceReader.readYc(anyString(), anyString(), eq(521L))).thenReturn(rows(0));
        when(sourceReader.readYc(anyString(), anyString(), eq(522L))).thenReturn(rows(0));
        when(sourceReader.readYc(anyString(), anyString(), eq(511L))).thenReturn(rows(10));
        when(sourceReader.readYc(anyString(), anyString(), eq(512L))).thenReturn(rows(9));

        RuntimePipeline.DayResult r = pipeline.recomputeDay(DAY);
        assertEquals(1440, r.inServiceMin(), "恰 10 即并网（≥10 阈值）");
    }

    private YcHistory row(String time, double value)
    {
        YcHistory y = new YcHistory();
        y.setYcTime(time);
        y.setYcData(value);
        return y;
    }
}
