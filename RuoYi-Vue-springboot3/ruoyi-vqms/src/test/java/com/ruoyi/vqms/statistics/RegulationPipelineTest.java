package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.domain.VqmsCommandLedger;
import com.ruoyi.vqms.management.domain.VqmsRegulationCmd;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.management.mapper.VqmsRegulationCmdMapper;
import com.ruoyi.vqms.management.service.VqmsJudgeParamService;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.reader.SourceReader;
import com.ruoyi.vqms.source.reader.YxSignalReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S4 Slice1 L0：RegulationPipeline 编排与路由（§8.8.2）——mock 全协作方、真 judge。
 *
 * <p>覆盖：stub 护栏拒写、正常判定落账（免考应用）、Undecodable 占位、门控拦截披露、
 * 脏时间跳过。策略处置（disposition）本切片恒 NULL（选套前只记不判，S5 接线）。</p>
 */
class RegulationPipelineTest
{
    private static final LocalDate DAY = LocalDate.of(2026, 3, 23);
    private static final String T0_TEXT = "2026-03-23 09:59:59";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 23, 10, 0);
    private static final String TARGET_TEXT =
            "收到远方遥调执行指令:主省220KV目标值,22315."; // V_target 223.15

    private final VqmsCommandLedgerMapper ledgerMapper = mock(VqmsCommandLedgerMapper.class);
    private final SourceReader sourceReader = mock(SourceReader.class);
    private final GateFilter gateFilter = mock(GateFilter.class);
    private final RegulationJudge judge = new DefaultRegulationJudge(); // 真实现
    private final YxSignalReader yxReader = mock(YxSignalReader.class);
    private final VqmsBusbarGroupMapper groupMapper = mock(VqmsBusbarGroupMapper.class);
    private final VqmsBusbarMapper busbarMapper = mock(VqmsBusbarMapper.class);
    private final VqmsRegulationCmdMapper cmdMapper = mock(VqmsRegulationCmdMapper.class);
    private final VqmsJudgeParamService paramService = mock(VqmsJudgeParamService.class);

    private RegulationPipeline pipeline;

    @BeforeEach
    void setUp()
    {
        pipeline = new RegulationPipeline(ledgerMapper, sourceReader, gateFilter, judge,
                yxReader, groupMapper, busbarMapper, cmdMapper, paramService);
        when(paramService.getInt("t_fast")).thenReturn(4);
        when(paramService.getInt("t_econ")).thenReturn(5);

        VqmsBusbar busbar = new VqmsBusbar();
        busbar.setBusbarNum(0L);
        busbar.setRealtimeYcNum(4002L);
        when(busbarMapper.selectList()).thenReturn(List.of(busbar));

        VqmsBusbarGroup group = new VqmsBusbarGroup();
        group.setMainIndicatorYcNum(4001L);
        group.setDefaultMainBusbarNum(0L);
        when(groupMapper.selectList()).thenReturn(List.of(group));

        // 指示点保持值=0 → 主母线 bn0；实时电压 234.25；免考旗缺省无数据
        when(yxReader.heldValue(eq(4001L), any())).thenReturn(Optional.of(0));
        when(yxReader.heldDecimalValue(eq(4002L), any())).thenReturn(Optional.of(234.25));
        when(yxReader.heldValue(eq(501L), any())).thenReturn(Optional.empty());
        when(gateFilter.shouldJudge(any())).thenReturn(true);

        // 窗口 [1..5] 全齐，包络 [222,225] 夹住 223.15
        List<HisCurveSv> curves = new java.util.ArrayList<>();
        for (int offset = 1; offset <= 5; offset++)
        {
            HisCurveSv r = new HisCurveSv();
            r.setSaveTime(String.format("2026-03-23 10:%02d:00.000", offset));
            r.setBusbarNum(0L);
            r.setHighSV(new BigDecimal(225));
            r.setLowSV(new BigDecimal(222));
            curves.add(r);
        }
        when(sourceReader.readCurve(anyString(), anyString(), eq(0L))).thenReturn(curves);
    }

    private VqmsCommandLedger ledger(String warnTime, String content, Long objNum)
    {
        VqmsCommandLedger c = new VqmsCommandLedger();
        c.setWarnTime(warnTime);
        c.setMillisecond("100");
        c.setObjNum(objNum);
        c.setWarnContent(content);
        return c;
    }

    @Test
    void stubGuard_refusesToWriteOfficialStats()
    {
        RegulationPipeline stubPipeline = new RegulationPipeline(ledgerMapper, sourceReader,
                gateFilter, new StubRegulationJudge(), yxReader, groupMapper, busbarMapper,
                cmdMapper, paramService);
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));

        RegulationPipeline.PipelineResult r = stubPipeline.recompute(DAY, DAY);
        assertEquals(-1, r.stubBlocked());
        assertEquals(0, r.written());
        verify(cmdMapper, never()).upsertBatch(any());
    }

    @Test
    void happyPath_judgedAndExemptionApplied()
    {
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));
        when(yxReader.heldValue(eq(501L), any())).thenReturn(Optional.of(0)); // 未免考

        RegulationPipeline.PipelineResult r = pipeline.recompute(DAY, DAY);
        assertEquals(1, r.total());
        assertEquals(1, r.written());

        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        VqmsRegulationCmd row = captor.getValue().get(0);
        assertEquals("QUALIFIED", row.getFastState());
        assertEquals("QUALIFIED", row.getEconState());
        assertEquals("V1_0", row.getAlgorithmId());
        assertEquals(4, row.gettFastSnapshot());
        assertEquals(0, new BigDecimal("1.0").compareTo(row.getCompleteness()));
        assertNull(row.getInvalidTiers());
        assertNull(row.getUndecodableReason());
        assertEquals(0, row.getYx501Fast());
        assertEquals(0, row.getYx501Econ());
        assertNull(row.getDisposition(), "选套前策略处置恒 NULL【默认口径④】");
    }

    @Test
    void penalizedWithExemptFlag_becomesExempted()
    {
        // 包络不夹：窗口行整体在目标上方 → 两档 PENALIZED；yx501=1 → EXEMPTED
        List<HisCurveSv> highCurves = new java.util.ArrayList<>();
        for (int offset = 1; offset <= 5; offset++)
        {
            HisCurveSv r2 = new HisCurveSv();
            r2.setSaveTime(String.format("2026-03-23 10:%02d:00.000", offset));
            r2.setBusbarNum(0L);
            r2.setHighSV(new BigDecimal(230));
            r2.setLowSV(new BigDecimal(228));
            highCurves.add(r2);
        }
        when(sourceReader.readCurve(anyString(), anyString(), eq(0L))).thenReturn(highCurves);
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));
        when(yxReader.heldValue(eq(501L), any())).thenReturn(Optional.of(1));

        pipeline.recompute(DAY, DAY);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        assertEquals("EXEMPTED", captor.getValue().get(0).getFastState());
        assertEquals("EXEMPTED", captor.getValue().get(0).getEconState());
    }

    @Test
    void undecodable_invalidPlaceholder_withReason()
    {
        // 增量形态 + 无实时电压（母线未配 realtime）→ 缺t₀电压
        VqmsBusbar noRealtime = new VqmsBusbar();
        noRealtime.setBusbarNum(0L); // realtimeYcNum=null
        when(busbarMapper.selectList()).thenReturn(List.of(noRealtime));
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT,
                        "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 0L)));

        RegulationPipeline.PipelineResult r = pipeline.recompute(DAY, DAY);
        assertEquals(1, r.written());
        assertEquals(1, r.undecodableCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        VqmsRegulationCmd row = captor.getValue().get(0);
        assertEquals("INVALID", row.getFastState());
        assertEquals("INVALID", row.getEconState());
        assertEquals("MISSING_T0_VOLTAGE", row.getUndecodableReason());
        assertEquals(0, BigDecimal.ZERO.compareTo(row.getCompleteness()));
    }

    @Test
    void gateBlocked_skippedAndDisclosed_notWritten()
    {
        when(gateFilter.shouldJudge(any())).thenReturn(false);
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));

        RegulationPipeline.PipelineResult r = pipeline.recompute(DAY, DAY);
        assertEquals(1, r.gateSkipped());
        assertEquals(0, r.written());
        verify(cmdMapper, never()).upsertBatch(any()); // 默认口径②：拦截不进分母
    }

    @Test
    void dirtyWarnTime_skipped()
    {
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger("not-a-time", TARGET_TEXT, 0L)));

        RegulationPipeline.PipelineResult r = pipeline.recompute(DAY, DAY);
        assertEquals(1, r.dirtyTimeSkipped());
        assertEquals(0, r.written());
        verify(cmdMapper, never()).upsertBatch(any());
    }
}
