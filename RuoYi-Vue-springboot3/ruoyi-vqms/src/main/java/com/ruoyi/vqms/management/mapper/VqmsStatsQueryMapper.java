package com.ruoyi.vqms.management.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 统计读侧 mapper（S4 读侧：rollup 表 → 查询接口；写侧见各自 upsert mapper）。
 *
 * <p>返回 {@code List<Map<String,Object>>}——读侧是聚合行直出，字段名与 SQL 别名即
 * 前端契约；率/罚款不在此算（由 Service 经 {@code RegulationStatistics.summarizeCounts}
 * 纯函数统一计算，单一来源）。</p>
 */
public interface VqmsStatsQueryMapper
{
    /** 调节月汇总行（statMonth 为空 = 全部月份，倒序） */
    List<Map<String, Object>> selectRegulationMonthly(@Param("statMonth") String statMonth);

    /** 投运率月记账行（statMonth 为空 = 全部月份，倒序） */
    List<Map<String, Object>> selectRuntimeMonthly(@Param("statMonth") String statMonth);
}
