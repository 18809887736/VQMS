package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.domain.VqmsRuntimeDaily;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsRuntimeDailyMapper;
import com.ruoyi.vqms.source.model.YcHistory;
import com.ruoyi.vqms.source.reader.SourceReader;
import com.ruoyi.vqms.source.reader.YxSignalReaderImpl;

/**
 * 投运率日记账管线（S4 Slice2；正式 v1_0 §1.3~§1.5）——编排，无算术：
 *
 * <pre>整日信号预取（内存阶跃保持）→ 逐分钟 RuntimeMinuteClassifier → RuntimeMinuteCounts
 *      → RuntimeStatistics.summarize → 写 vqms_runtime_daily（同日覆盖 upsert）</pre>
 *
 * <p><b>信号组合默认口径（待 Leo 确认/随真实点号核对复核）</b>：</p>
 * <ul>
 *   <li>并网 = max(yc511, yc512) ≥ 10（任一母线并网即厂级并网，points.yaml 值语义）；</li>
 *   <li>退出原因 = max(yc521, yc522)——任一侧非电网证据(2)即从严计罚，
 *       两侧均电网(1)才免责；缺失按 0（矛盾态从严由分类器承接）；</li>
 *   <li>AVC投退缺失从严按 0（退）；</li>
 *   <li>容量 = busbar_group 各组和（决策⑤ 厂级口径）；全组未配置 → 率照算、罚款 NULL。</li>
 * </ul>
 */
@Component
public class RuntimePipeline
{
    private static final Logger log = LoggerFactory.getLogger(RuntimePipeline.class);

    /** ⚠ 合成库点号体系——真实现场核对后必须换号（3009 撞号警示 v5.0 §14-7），常量集中此处一处改 */
    private static final long AVC_ONOFF_YC = 3009L;
    private static final long GRID_MAIN_YC = 511L;
    private static final long GRID_AUX_YC = 512L;
    private static final long EXIT_MAIN_YC = 521L;
    private static final long EXIT_AUX_YC = 522L;

    /** 并网点值语义：带电×10 + 机组数，≥10 即并网（正式版 §8.3 注） */
    private static final int GRID_THRESHOLD = 10;

    private static final DateTimeFormatter MINUTE_TEXT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SourceReader sourceReader;
    private final VqmsBusbarGroupMapper groupMapper;
    private final VqmsRuntimeDailyMapper runtimeMapper;

    public RuntimePipeline(SourceReader sourceReader, VqmsBusbarGroupMapper groupMapper,
            VqmsRuntimeDailyMapper runtimeMapper)
    {
        this.sourceReader = sourceReader;
        this.groupMapper = groupMapper;
        this.runtimeMapper = runtimeMapper;
    }

    /** 单日重算结果披露。 */
    public record DayResult(LocalDate day, int inServiceMin, int exitGridMin,
            int exitNonGridMin, int offlineMin, BigDecimal ratePct, boolean capacityMissing)
    {
    }

    /**
     * 重算指定日的投运率记账（同日覆盖）。
     */
    public DayResult recomputeDay(LocalDate day)
    {
        String from = MINUTE_TEXT.format(day.atStartOfDay().minusDays(1)); // 回看前一日：跨零点保持
        String to = MINUTE_TEXT.format(day.plusDays(1).atStartOfDay());

        HeldSignal avc = prefetch(AVC_ONOFF_YC, from, to);
        HeldSignal gridMain = prefetch(GRID_MAIN_YC, from, to);
        HeldSignal gridAux = prefetch(GRID_AUX_YC, from, to);
        HeldSignal exitMain = prefetch(EXIT_MAIN_YC, from, to);
        HeldSignal exitAux = prefetch(EXIT_AUX_YC, from, to);

        int inService = 0;
        int exitGrid = 0;
        int exitNonGrid = 0;
        int offline = 0;
        LocalDateTime minute = day.atStartOfDay();
        for (int i = 0; i < 24 * 60; i++, minute = minute.plusMinutes(1))
        {
            boolean onGrid = Math.max(gridMain.held(minute), gridAux.held(minute)) >= GRID_THRESHOLD;
            int avcIn = avc.heldOr(minute, 0); // 缺失从严按退
            int exitReason = Math.max(exitMain.heldOr(minute, 0), exitAux.heldOr(minute, 0));
            switch (RuntimeMinuteClassifier.classify(onGrid, avcIn, exitReason))
            {
                case IN_SERVICE -> inService++;
                case EXIT_GRID -> exitGrid++;
                case EXIT_NON_GRID -> exitNonGrid++;
                case OFFLINE -> offline++;
            }
        }

        BigDecimal capacity = sumCapacity();
        RuntimeMinuteCounts counts = new RuntimeMinuteCounts(inService, exitGrid, exitNonGrid, offline);
        RuntimeStatistics.RuntimeRateResult result =
                RuntimeStatistics.summarize(counts, capacity == null ? 0.0 : capacity.doubleValue());
        boolean zeroBase = counts.inService() + counts.exitNonGrid() == 0;

        VqmsRuntimeDaily row = new VqmsRuntimeDaily();
        row.setStatDate(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        row.setInServiceMin(inService);
        row.setExitGridMin(exitGrid);
        row.setExitNonGridMin(exitNonGrid);
        row.setOfflineMin(offline);
        row.setRatedCapacityKw(capacity);
        row.setRatePct(zeroBase ? null : BigDecimal.valueOf(result.ratePct()));
        row.setShortfallPct(zeroBase ? null : BigDecimal.valueOf(result.shortfallPct()));
        row.setPenaltyScore(capacity == null || zeroBase ? null : BigDecimal.valueOf(result.penaltyScore()));
        runtimeMapper.upsert(row);

        log.info("VQMS 投运率日记账重算 [{}]: 投运 {} / 电网退出 {} / 非电网退出 {} / 未并网 {}",
                day, inService, exitGrid, exitNonGrid, offline);
        return new DayResult(day, inService, exitGrid, exitNonGrid, offline,
                row.getRatePct(), capacity == null);
    }

    /** 整日信号预取 + 内存阶跃保持（回看窗=前一日，覆盖零点静态延续）。 */
    private HeldSignal prefetch(long ycNum, String from, String to)
    {
        List<YcHistory> rows = sourceReader.readYc(from, to, ycNum);
        return new HeldSignal(rows);
    }

    /** 容量 = 各组和（决策⑤ 厂级口径）；全部未配置 → null。 */
    private BigDecimal sumCapacity()
    {
        BigDecimal total = null;
        for (VqmsBusbarGroup g : groupMapper.selectList())
        {
            if (g.getRatedCapacityKw() != null)
            {
                total = total == null ? g.getRatedCapacityKw() : total.add(g.getRatedCapacityKw());
            }
        }
        return total;
    }

    /** 内存阶跃保持：≤ 当分钟最近一条 yc_data（YxSignalReaderImpl.pickLatest 同款语义）。 */
    static final class HeldSignal
    {
        private final List<YcHistory> rows;

        HeldSignal(List<YcHistory> rows)
        {
            this.rows = rows;
        }

        Integer held(LocalDateTime at)
        {
            Optional<YcHistory> r = YxSignalReaderImpl.pickLatest(rows, at);
            return r.map(y -> y.getYcData().intValue()).orElse(null);
        }

        int heldOr(LocalDateTime at, int fallback)
        {
            Integer v = held(at);
            return v == null ? fallback : v;
        }
    }
}
