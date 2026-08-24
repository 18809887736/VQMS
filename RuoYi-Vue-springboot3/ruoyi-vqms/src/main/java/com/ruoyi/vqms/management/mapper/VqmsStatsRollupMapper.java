package com.ruoyi.vqms.management.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 统计三级 rollup mapper（S4 Slice3——SQL 从 S2S3StatsTablesIT 权威语句提升，单一来源）。
 *
 * <p>铁律：对计数求和、绝不平均率列；algorithm_id 单一=该 ID / 混合=MIXED（决策④）。
 * 全部幂等（ON DUPLICATE KEY UPDATE 覆盖），可按任意日期区间重复执行。</p>
 */
public interface VqmsStatsRollupMapper
{
    /** 调节明细 → 日汇总 */
    int rollupRegulationDaily(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 调节日汇总 → 月汇总（对计数求和） */
    int rollupRegulationMonthly(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 调节月汇总 → 年汇总 */
    int rollupRegulationYearly(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 投运率日记账 → 月记账（分钟计数求和；率快照由调用方按合计分钟重算，本语句置 NULL 待写回） */
    int rollupRuntimeMonthly(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 投运率月记账 → 年记账 */
    int rollupRuntimeYearly(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
