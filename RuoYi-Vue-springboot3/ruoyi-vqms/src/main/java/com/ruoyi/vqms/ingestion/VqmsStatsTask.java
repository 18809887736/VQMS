package com.ruoyi.vqms.ingestion;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.ruoyi.vqms.management.mapper.VqmsStatsRollupMapper;
import com.ruoyi.vqms.statistics.RegulationPipeline;
import com.ruoyi.vqms.statistics.RuntimePipeline;

/**
 * VQMS 统计重算任务（S4 Slice3，RuoYi Quartz 入口——sys_job invokeTarget 按 bean 名调用）。
 *
 * <p>纯委托：区间解析 → 两条 Pipeline → 三级 rollup 级联。无算术、无状态。</p>
 *
 * <p>入口：</p>
 * <ul>
 *   <li>{@code vqmsStatsTask.recomputeYesterday()} —— 每日调度无参入口（昨日全链）；</li>
 *   <li>{@code vqmsStatsTask.recomputeRange('yyyy-MM-dd','yyyy-MM-dd')} —— 手动/回补；</li>
 *   <li>{@code vqmsStatsTask.recomputeRuntimeDay('yyyy-MM-dd')} —— 仅投运率单日。</li>
 * </ul>
 */
@Component("vqmsStatsTask")
public class VqmsStatsTask
{
    private final RegulationPipeline regulationPipeline;
    private final RuntimePipeline runtimePipeline;
    private final VqmsStatsRollupMapper rollupMapper;

    public VqmsStatsTask(RegulationPipeline regulationPipeline, RuntimePipeline runtimePipeline,
            VqmsStatsRollupMapper rollupMapper)
    {
        this.regulationPipeline = regulationPipeline;
        this.runtimePipeline = runtimePipeline;
        this.rollupMapper = rollupMapper;
    }

    /** 每日无参入口：昨日全链重算（调节明细 + 投运率日账 + 五级 rollup）。 */
    public void recomputeYesterday()
    {
        recomputeRange(LocalDate.now().minusDays(1).toString(),
                LocalDate.now().minusDays(1).toString());
    }

    /**
     * 按日期区间全链幂等重算（含两端）。
     *
     * @param startDateStr yyyy-MM-dd（含）
     * @param endDateStr   yyyy-MM-dd（含）
     */
    public void recomputeRange(String startDateStr, String endDateStr)
    {
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = LocalDate.parse(endDateStr);
        if (end.isBefore(start))
        {
            throw new IllegalArgumentException("日期区间倒置: " + startDateStr + " ~ " + endDateStr);
        }
        regulationPipeline.recompute(start, end);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
        {
            runtimePipeline.recomputeDay(d);
        }
        cascadeRollup(start, end);
    }

    /** 仅投运率单日重算（调节侧明细依赖指令流水抓取节奏，通常按区间跑）。 */
    public void recomputeRuntimeDay(String dayStr)
    {
        runtimePipeline.recomputeDay(LocalDate.parse(dayStr));
    }

    private void cascadeRollup(LocalDate start, LocalDate end)
    {
        String s = start.toString();
        String e = end.toString();
        rollupMapper.rollupRegulationDaily(s, e);
        rollupMapper.rollupRegulationMonthly(s, e);
        rollupMapper.rollupRegulationYearly(s, e);
        rollupMapper.rollupRuntimeMonthly(s, e);
        rollupMapper.rollupRuntimeYearly(s, e);
    }
}
