package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ruoyi.common.utils.DateUtils;
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
import com.ruoyi.vqms.statistics.FreeformPolicyEvaluator;
import com.ruoyi.vqms.statistics.FreeformPolicyConfig;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.reader.MinuteRounder;
import com.ruoyi.vqms.source.reader.SourceReader;
import com.ruoyi.vqms.source.reader.YxSignalReader;

/**
 * 调节合格率管线（三阶段装配体，v5.0 §8.8.2；S4 Slice1）——本身无算术，只有编排与路由：
 *
 * <pre>ledger 指令流 → GateFilter → RegulationJudge → ExemptionApplier（免考后置）
 *                                              ↘ Undecodable → INVALID 占位
 *              → 写 vqms_regulation_cmd（uk 幂等 upsert，重算即覆盖）</pre>
 *
 * <p><b>默认口径（待 Leo 确认，改路由各一行）</b>：① stub 护栏=拒写——{@code judge.isStub()}
 * 时整批不落正式统计表并 WARN（§8.8.5 两选项取彻底防假数字者）；② 门控拦截指令不进分母、
 * 仅计数披露；③ 免考旗缺失从严按 0（计罚）——对齐 RuntimeMinuteClassifier 矛盾态从严先例，
 * ⚠ UNVERIFIED-口径：yx501 读取失败处置属第三类失效分轨（随 #3/#5），拍板后复核；
 * ④ 策略处置 disposition 本切片恒 NULL（选套前「只记不判」，S5 选套后接
 * {@code DataUnavailabilityPolicy.evaluate}）。</p>
 */
@Component
public class RegulationPipeline
{
    private static final Logger log = LoggerFactory.getLogger(RegulationPipeline.class);

    private static final long EXEMPT_FLAG_YC = 501L;
    private static final DateTimeFormatter DAY_TEXT = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD);
    private static final DateTimeFormatter MINUTE_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VqmsCommandLedgerMapper ledgerMapper;
    private final SourceReader sourceReader;
    private final GateFilter gateFilter;
    private final RegulationJudge judge;
    private final YxSignalReader yxReader;
    private final VqmsBusbarGroupMapper groupMapper;
    private final VqmsBusbarMapper busbarMapper;
    private final VqmsRegulationCmdMapper cmdMapper;
    private final VqmsJudgeParamService judgeParamService;
    private final VqmsPolicyParamService policyParamService;

    public RegulationPipeline(VqmsCommandLedgerMapper ledgerMapper, SourceReader sourceReader,
            GateFilter gateFilter, RegulationJudge judge, YxSignalReader yxReader,
            VqmsBusbarGroupMapper groupMapper, VqmsBusbarMapper busbarMapper,
            VqmsRegulationCmdMapper cmdMapper, VqmsJudgeParamService judgeParamService,
            VqmsPolicyParamService policyParamService)
    {
        this.ledgerMapper = ledgerMapper;
        this.sourceReader = sourceReader;
        this.gateFilter = gateFilter;
        this.judge = judge;
        this.yxReader = yxReader;
        this.groupMapper = groupMapper;
        this.busbarMapper = busbarMapper;
        this.cmdMapper = cmdMapper;
        this.judgeParamService = judgeParamService;
        this.policyParamService = policyParamService;
    }

    /** 一次重算的计数披露（门控拦截/脏时间/非电压指令/stub 拦截照实上报，不静默）。 */
    public record PipelineResult(int total, int written, int gateSkipped,
            int dirtyTimeSkipped, int nonVoltageSkipped, int undecodableCount, int stubBlocked)
    {
    }

    /**
     * 按【指令日期】区间幂等重算判定明细（t₀ 归属日）。
     *
     * @param startDate 含；endDate 含
     */
    public PipelineResult recompute(LocalDate startDate, LocalDate endDate)
    {
        JudgeParams params = new JudgeParams(
                judgeParamService.getInt("t_fast"), judgeParamService.getInt("t_econ"));

        // stub 护栏（默认口径①）：占位实现拒绝产出正式考核数字
        if (judge.isStub())
        {
            log.warn("VQMS 调节管线：当前判定实现为 STUB 占位——按护栏拒写正式统计表"
                    + "（切换 vqms.judge.algorithm=V1_0 后重跑本区间）");
            return new PipelineResult(0, 0, 0, 0, 0, 0, -1);
        }

        String rangeStart = DAY_TEXT.format(startDate) + " 00:00:00";
        String rangeEndExclusive = DAY_TEXT.format(endDate.plusDays(1)) + " 00:00:00";
        List<VqmsCommandLedger> commands =
                ledgerMapper.selectByWarnTimeRange(rangeStart, rangeEndExclusive);
        if (commands.isEmpty())
        {
            return new PipelineResult(0, 0, 0, 0, 0, 0, 0);
        }

        // 配置预取：母线（实时电压点位）、组（主母线号指示点 + 兜底）
        Map<Long, Long> realtimeYcByBusbar = new HashMap<>();
        for (VqmsBusbar b : busbarMapper.selectList())
        {
            if (b.getRealtimeYcNum() != null)
            {
                realtimeYcByBusbar.put(b.getBusbarNum(), b.getRealtimeYcNum());
            }
        }
        Long indicatorYc = null;
        Long fallbackBusbar = null;
        for (VqmsBusbarGroup g : groupMapper.selectList())
        {
            if (g.getMainIndicatorYcNum() != null && indicatorYc == null)
            {
                indicatorYc = g.getMainIndicatorYcNum();
                fallbackBusbar = g.getDefaultMainBusbarNum();
            }
        }

        // 曲线批量预取：区间两端各放宽 t_econ 分钟（首末指令的完整窗口）
        LocalDateTime minT0 = null;
        LocalDateTime maxT0 = null;
        List<T0Command> valid = new ArrayList<>();
        int dirtyTimeSkipped = 0;
        int nonVoltageSkipped = 0;
        for (VqmsCommandLedger c : commands)
        {
            LocalDateTime t0 = MinuteRounder.parseAndRound(c.getWarnTime());
            if (t0 == null)
            {
                dirtyTimeSkipped++;
                continue;
            }
            // 非电压指令（Leo 2026-08-26 拍板：以指令文本为准）——排除出分母，仅披露；
            // 不进 valid → 不参与 minT0/maxT0，曲线预取窗口不为它放宽
            if (VTargetDecoder.isNonVoltage(c.getWarnContent()))
            {
                nonVoltageSkipped++;
                continue;
            }
            valid.add(new T0Command(c, t0));
            if (minT0 == null || t0.isBefore(minT0))
            {
                minT0 = t0;
            }
            if (maxT0 == null || t0.isAfter(maxT0))
            {
                maxT0 = t0;
            }
        }

        Map<Long, Map<LocalDateTime, HisCurveSv>> curveIndex = new HashMap<>();
        if (!valid.isEmpty())
        {
            String from = MINUTE_TEXT.format(minT0.minusMinutes(1));
            String to = MINUTE_TEXT.format(maxT0.plusMinutes(params.tEcon() + 1L));
            for (Long busbarNum : realtimeYcByBusbar.keySet())
            {
                Map<LocalDateTime, HisCurveSv> byMinute = new HashMap<>();
                for (HisCurveSv row : sourceReader.readCurve(from, to, busbarNum))
                {
                    LocalDateTime m = MinuteRounder.parseAndRound(row.getSaveTime());
                    if (m != null)
                    {
                        byMinute.putIfAbsent(m, row); // 同分钟多行取首行（D9 IT 口径）
                    }
                }
                curveIndex.put(busbarNum, byMinute);
            }
        }

        // 策略预取（单选生效 §3.3.4：戊·自由组合键族存在即优先，预设四键休眠；
        // 两者皆缺 → disposition NULL 只记不判）
        Optional<FreeformPolicyConfig> freeformConfig = policyParamService.loadFreeformConfig();
        Optional<PolicyConfig> presetConfig = freeformConfig.isPresent()
                ? Optional.empty()
                : policyParamService.loadConfig();

        List<VqmsRegulationCmd> rows = new ArrayList<>();
        int gateSkipped = 0;
        int undecodableCount = 0;
        for (T0Command tc : valid)
        {
            VqmsCommandLedger c = tc.command();
            LocalDateTime t0 = tc.t0();

            if (!gateFilter.shouldJudge(t0))
            {
                gateSkipped++; // 默认口径②：拦截不进分母，仅披露
                continue;
            }

            Long busbarNum = resolveMainBusbar(indicatorYc, fallbackBusbar,
                    realtimeYcByBusbar.keySet(), t0);
            List<MinuteCurve> curves = collectWindow(curveIndex, busbarNum, t0, params.tEcon());
            Double realtime = busbarNum == null ? null
                    : yxReader.heldDecimalValue(realtimeYcByBusbar.get(busbarNum), t0).orElse(null);

            AvcCommand cmd = new AvcCommand(c.getWarnTime(), c.getMillisecond(),
                    c.getObjNum(), c.getWarnContent(), realtime);
            RegulationOutcome outcome = judge.judge(cmd, curves, params);

            VqmsRegulationCmd row = new VqmsRegulationCmd();
            row.setStatDate(Date.from(t0.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            row.setWarnTime(c.getWarnTime());
            row.setMillisecond(c.getMillisecond());
            row.setObjNum(c.getObjNum());
            row.setAlgorithmId("V1_0");
            row.settFastSnapshot(params.tFast());
            // 逐档时点采样（⚠ UNVERIFIED-口径：快档@t₀+t_fast、经济档@窗口闭合 t₀+t_econ+1，
            // 由合成场景 S05~S07 中途跳变数据反推，待对端确认）；缺失从严按 0（默认口径③）
            Integer fastFlag = heldFlag(EXEMPT_FLAG_YC, t0.plusMinutes(params.tFast()));
            Integer econFlag = heldFlag(EXEMPT_FLAG_YC, t0.plusMinutes(params.tEcon() + 1L));
            row.setYx501Fast(fastFlag);
            row.setYx501Econ(econFlag);

            // 策略处置（S5 选套 / §3.3 戊·自由组合生效后写入；未选套 → NULL 只记不判）
            Disp policyDisp = dispositionOf(outcome, presetConfig, freeformConfig);
            if (outcome instanceof RegulationOutcome.Judged j)
            {
                TierFinalDisposition tierDisp = ExemptionApplier.apply(j,
                        fastFlag == null ? 0 : fastFlag, econFlag == null ? 0 : econFlag);
                row.setFastState(tierDisp.fast().name());
                row.setEconState(tierDisp.econ().name());
                row.setCompleteness(BigDecimal.valueOf(j.completeness()));
                row.setInvalidTiers(joinTiers(j.invalidTiers()));
                row.setDisposition(policyDisp.disposition());
                row.setHitRuleId(policyDisp.ruleId());
            }
            else
            {
                RegulationOutcome.Undecodable u = (RegulationOutcome.Undecodable) outcome;
                undecodableCount++;
                row.setFastState(FinalTierState.INVALID.name());
                row.setEconState(FinalTierState.INVALID.name()); // 整指令占位（allInvalid 语义）
                row.setCompleteness(BigDecimal.ZERO);
                row.setUndecodableReason(u.reason().name());
                row.setDisposition(policyDisp.disposition());
                row.setHitRuleId(policyDisp.ruleId());
            }
            rows.add(row);
        }

        if (!rows.isEmpty())
        {
            cmdMapper.upsertBatch(rows);
        }
        log.info("VQMS 调节管线重算 [{}, {}]: 总 {} 条 / 落账 {} / 门控拦 {} / 判不了 {} / 非电压 {} / 脏时间 {}",
                startDate, endDate, commands.size(), rows.size(), gateSkipped, undecodableCount,
                nonVoltageSkipped, dirtyTimeSkipped);
        return new PipelineResult(commands.size(), rows.size(), gateSkipped,
                dirtyTimeSkipped, nonVoltageSkipped, undecodableCount, 0);
    }

    /** 策略处置结果：桶名 + 命中规则 ID（戊模式专用；预设/未选套 ruleId=null）。 */
    private record Disp(String disposition, String ruleId)
    {
    }

    /** 策略评估：自由组合键族优先（首中即断求值，落命中规则 ID）；预设四键次之；皆缺 → NULL 只记不判。 */
    private static Disp dispositionOf(RegulationOutcome outcome,
            Optional<PolicyConfig> presetConfig, Optional<FreeformPolicyConfig> freeformConfig)
    {
        if (freeformConfig.isPresent())
        {
            FreeformPolicyEvaluator.Decision d =
                    FreeformPolicyEvaluator.evaluate(outcome, freeformConfig.get());
            return new Disp(d.disposition().name(), d.ruleId());
        }
        return presetConfig
                .map(cfg -> new Disp(DataUnavailabilityPolicy.evaluate(outcome, cfg).name(), null))
                .orElseGet(() -> new Disp(null, null));
    }

    private record T0Command(VqmsCommandLedger command, LocalDateTime t0)
    {
    }

    /** 主母线解析：指示点阶跃保持值 ∈ 已知母线则用之，否则组兜底（§6.2.2）；无从解析 → null（窗口空→双档 invalid）。 */
    private Long resolveMainBusbar(Long indicatorYc, Long fallbackBusbar,
            java.util.Set<Long> knownBusbars, LocalDateTime t0)
    {
        if (indicatorYc != null)
        {
            Optional<Integer> v = yxReader.heldValue(indicatorYc, t0);
            if (v.isPresent() && knownBusbars.contains(v.get().longValue()))
            {
                return v.get().longValue();
            }
        }
        return fallbackBusbar;
    }

    private List<MinuteCurve> collectWindow(Map<Long, Map<LocalDateTime, HisCurveSv>> index,
            Long busbarNum, LocalDateTime t0, int tEcon)
    {
        List<MinuteCurve> curves = new ArrayList<>();
        Map<LocalDateTime, HisCurveSv> byMinute = busbarNum == null ? null : index.get(busbarNum);
        if (byMinute == null)
        {
            return curves;
        }
        for (int offset = 1; offset <= tEcon; offset++)
        {
            HisCurveSv row = byMinute.get(t0.plusMinutes(offset));
            if (row != null && row.getHighSV() != null && row.getLowSV() != null)
            {
                curves.add(new MinuteCurve(offset,
                        row.getHighSV().intValue(), row.getLowSV().intValue()));
            }
        }
        return curves;
    }

    private Integer heldFlag(long ycNum, LocalDateTime at)
    {
        return yxReader.heldValue(ycNum, at).orElse(null);
    }

    private static String joinTiers(java.util.Set<Tier> tiers)
    {
        if (tiers.isEmpty())
        {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Tier t : Tier.values())
        {
            if (tiers.contains(t))
            {
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(t.name());
            }
        }
        return sb.toString();
    }
}
