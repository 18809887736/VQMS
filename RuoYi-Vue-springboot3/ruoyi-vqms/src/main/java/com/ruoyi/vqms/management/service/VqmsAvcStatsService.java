package com.ruoyi.vqms.management.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsStatsQueryMapper;
import com.ruoyi.vqms.statistics.RegulationStatistics;

/**
 * 统计读侧 Service（S4 读侧：rollup 表 → AVC 两页）——计数到率/罚款的换算
 * 全部经 {@code RegulationStatistics.summarizeCounts} 纯函数（与逐条版同源公式，
 * 单一来源）；容量 = busbar_group 各组和（决策⑤ 厂级口径）。
 */
@Service
public class VqmsAvcStatsService
{
    /** 厂级口径（附件6 考核对象是电厂），页面 gridSubject 列恒「全厂」 */
    private static final String GRID_SUBJECT = "全厂";

    @Autowired
    private VqmsStatsQueryMapper statsQueryMapper;

    @Autowired
    private VqmsBusbarGroupMapper groupMapper;

    /** 调节合格率月度行（两档平行；罚款缺额=penalized/total，剔免考推荐读法） */
    public List<Map<String, Object>> regulationMonthly(String statMonth)
    {
        double capacity = capacityKwOrZero();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : statsQueryMapper.selectRegulationMonthly(statMonth))
        {
            int total = intOf(row.get("totalCmds"));
            RegulationStatistics.TierRateResult fast = RegulationStatistics.summarizeCounts(
                    total, intOf(row.get("qualifiedFast")), intOf(row.get("penalizedFast")),
                    intOf(row.get("exemptedFast")), intOf(row.get("invalidFast")), capacity);
            RegulationStatistics.TierRateResult econ = RegulationStatistics.summarizeCounts(
                    total, intOf(row.get("qualifiedEcon")), intOf(row.get("penalizedEcon")),
                    intOf(row.get("exemptedEcon")), intOf(row.get("invalidEcon")), capacity);

            Map<String, Object> page = new HashMap<>();
            page.put("statMonth", row.get("statMonth"));
            page.put("gridSubject", GRID_SUBJECT);
            page.put("cmdCount", total);
            page.put("fastRate", round3(fast.disclosedRatePct()));
            page.put("econRate", round3(econ.disclosedRatePct()));
            page.put("fastPenalty", round2(fast.penaltyScore()));
            page.put("econPenalty", round2(econ.penaltyScore()));
            page.put("totalPenalty", round2(fast.penaltyScore() + econ.penaltyScore()));
            page.put("algorithmId", row.get("algorithmId"));
            page.put("undecodableCount", intOf(row.get("undecodableCount")));
            page.put("pendedCount", intOf(row.get("pendedCount")));
            out.add(page);
        }
        return out;
    }

    /** 投运率月度行（分钟数直出，率/缺额用 rollup 已算快照——SQL 层按合计分钟重算过） */
    public List<Map<String, Object>> runtimeMonthly(String statMonth)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : statsQueryMapper.selectRuntimeMonthly(statMonth))
        {
            Map<String, Object> page = new HashMap<>();
            page.put("statMonth", row.get("statMonth"));
            page.put("gridSubject", GRID_SUBJECT);
            page.put("gridMinutes", intOf(row.get("inServiceMin"))
                    + intOf(row.get("exitGridMin")) + intOf(row.get("exitNonGridMin")));
            page.put("runtimeMinutes", intOf(row.get("inServiceMin")));
            page.put("nonGridExitMinutes", intOf(row.get("exitNonGridMin")));
            page.put("gridExitMinutes", intOf(row.get("exitGridMin")));
            page.put("runtimeRate", row.get("ratePct"));
            page.put("deficit", row.get("shortfallPct"));
            page.put("penaltyScore", row.get("penaltyScore"));
            out.add(page);
        }
        return out;
    }

    private double capacityKwOrZero()
    {
        double total = 0.0;
        boolean any = false;
        for (com.ruoyi.vqms.management.domain.VqmsBusbarGroup g : groupMapper.selectList())
        {
            if (g.getRatedCapacityKw() != null)
            {
                total += g.getRatedCapacityKw().doubleValue();
                any = true;
            }
        }
        return any ? total : 0.0; // 容量未配置 → 罚款为 0（读侧展示口径；记账侧存 NULL）
    }

    private static int intOf(Object v)
    {
        return v == null ? 0 : ((Number) v).intValue();
    }

    private static Double round3(double v)
    {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static Double round2(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }
}
