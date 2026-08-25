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
import com.ruoyi.vqms.management.service.VqmsPolicyParamService;
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
    private final VqmsPolicyParamService policyParamService = mock(VqmsPolicyParamService.class);

    private RegulationPipeline pipeline;

    @BeforeEach
    void setUp()
    {
        pipeline = new RegulationPipeline(ledgerMapper, sourceReader, gateFilter, judge,
                yxReader, groupMapper, busbarMapper, cmdMapper, paramService, policyParamService);
        when(paramService.getInt("t_fast")).thenReturn(4);
        when(paramService.getInt("t_econ")).thenReturn(5);
        // 默认未选套：disposition NULL（选套前只记不判）
        when(policyParamService.loadConfig()).thenReturn(java.util.Optional.empty());

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
                cmdMapper, paramService, policyParamService);
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
    void policySelected_dispositionEvaluatedAndPersisted()
    {
        // S5 选套乙：partial_missing=EXCLUDE_REPORTED@50——completeness=0.4 的指令剔除+计数
        when(policyParamService.loadConfig()).thenReturn(java.util.Optional.of(
                PolicyPreset.YI.config()));
        // 窗口只余 2/5 分钟 → completeness 0.4（可用度 40%）< 阈值 50 → 触发剔除+计数
        List<HisCurveSv> partial = new java.util.ArrayList<>();
        for (int offset = 1; offset <= 2; offset++)
        {
            HisCurveSv r3 = new HisCurveSv();
            r3.setSaveTime(String.format("2026-03-23 10:%02d:00.000", offset));
            r3.setBusbarNum(0L);
            r3.setHighSV(new BigDecimal(225));
            r3.setLowSV(new BigDecimal(222));
            partial.add(r3);
        }
        when(sourceReader.readCurve(anyString(), anyString(), eq(0L))).thenReturn(partial);
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));

        pipeline.recompute(DAY, DAY);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        assertEquals("EXCLUDE_REPORTED", captor.getValue().get(0).getDisposition(),
                "选套乙：可用度 40% < 50% 阈值 → 剔除+计数");
    }

    @Test
    void allFourPresets_sameInput_differentDisposition_oneClickSwitch()
    {
        // 「一键切换」语义：同一条部分缺失指令（可用度 40%），仅换策略配置即得四种处置——
        // 甲=正常记账 / 乙=剔除计数 / 丙=计不合格 / 丁=挂起标记
        // 偏移取 {1,5}：两窗都有行（不触发 invalidTiers），缺 3 分钟 → completeness 0.4
        List<HisCurveSv> partial = new java.util.ArrayList<>();
        for (int offset : new int[] {1, 5})
        {
            HisCurveSv r4 = new HisCurveSv();
            r4.setSaveTime(String.format("2026-03-23 10:%02d:00.000", offset));
            r4.setBusbarNum(0L);
            r4.setHighSV(new BigDecimal(225));
            r4.setLowSV(new BigDecimal(222));
            partial.add(r4);
        }
        when(sourceReader.readCurve(anyString(), anyString(), eq(0L))).thenReturn(partial);

        String[][] matrix = {
                {"JIA", "COUNT_NORMAL"},
                {"YI", "EXCLUDE_REPORTED"},
                {"BING", "COUNT_UNQUALIFIED"},
                {"DING", "PEND_MARKED"},
        };
        for (String[] m : matrix)
        {
            when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                    .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));
            when(policyParamService.loadConfig())
                    .thenReturn(java.util.Optional.of(PolicyPreset.valueOf(m[0]).config()));
            org.mockito.Mockito.clearInvocations(cmdMapper);

            pipeline.recompute(DAY, DAY);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
            verify(cmdMapper).upsertBatch(captor.capture());
            assertEquals(m[1], captor.getValue().get(0).getDisposition(),
                    "预设 " + m[0] + " 下同输入应落 " + m[1]);
        }
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

    @Test
    void freeformRules_overrideDormantPreset()
    {
        // §3.3.4 单选生效：自由组合键族存在即优先，预设四键休眠——
        // 同一条部分缺失指令（可用度 40%）：丙在效会计不合格；戊规则表不命中 → 兜底正常记账
        when(policyParamService.loadFreeformConfig()).thenReturn(java.util.Optional.of(
                new FreeformPolicyConfig(List.of(
                        FreeformPolicyParser.parseRule("A1 -> PEND_MARKED")), 50)));
        when(policyParamService.loadConfig())
                .thenReturn(java.util.Optional.of(PolicyPreset.BING.config()));

        List<HisCurveSv> partial = new java.util.ArrayList<>();
        for (int offset : new int[] {1, 5})
        {
            HisCurveSv r5 = new HisCurveSv();
            r5.setSaveTime(String.format("2026-03-23 10:%02d:00.000", offset));
            r5.setBusbarNum(0L);
            r5.setHighSV(new BigDecimal(225));
            r5.setLowSV(new BigDecimal(222));
            partial.add(r5);
        }
        when(sourceReader.readCurve(anyString(), anyString(), eq(0L))).thenReturn(partial);
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT, TARGET_TEXT, 0L)));

        pipeline.recompute(DAY, DAY);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        assertEquals("COUNT_NORMAL", captor.getValue().get(0).getDisposition(),
                "戊规则表未命中部分缺事实 → 兜底正常记账（丙被休眠，不得出 COUNT_UNQUALIFIED）");
    }

    @Test
    void freeformDecodeSubtypeDispatch_endToEnd()
    {
        // 子类分发走全管线：缺 t₀（增量指令无实时电压）命中 R001 挂起待人工归因，
        // 其余解码失败才落到 R002 剔除——预设四键表达不了的形状。
        // （COUNT_NORMAL 不可用于解码轴——无判定结论可「正常记账」，校验器同口径拦截）
        when(policyParamService.loadFreeformConfig()).thenReturn(java.util.Optional.of(
                new FreeformPolicyConfig(List.of(
                        FreeformPolicyParser.parseRule("A1C -> PEND_MARKED"),
                        FreeformPolicyParser.parseRule("A1 -> EXCLUDE_REPORTED")), 50)));

        VqmsBusbar noRealtime = new VqmsBusbar();
        noRealtime.setBusbarNum(0L); // realtimeYcNum=null → 缺 t₀
        when(busbarMapper.selectList()).thenReturn(List.of(noRealtime));
        when(ledgerMapper.selectByWarnTimeRange(anyString(), anyString()))
                .thenReturn(List.of(ledger(T0_TEXT,
                        "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", 0L)));

        pipeline.recompute(DAY, DAY);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VqmsRegulationCmd>> captor = ArgumentCaptor.forClass(List.class);
        verify(cmdMapper).upsertBatch(captor.capture());
        VqmsRegulationCmd row = captor.getValue().get(0);
        assertEquals("MISSING_T0_VOLTAGE", row.getUndecodableReason());
        assertEquals("PEND_MARKED", row.getDisposition(),
                "A1C 规则命中：缺 t₀ 挂起标记（采集问题非电厂锅，留人工后审）");
    }
}
