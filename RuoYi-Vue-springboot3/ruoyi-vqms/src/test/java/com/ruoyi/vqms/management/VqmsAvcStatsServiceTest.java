package com.ruoyi.vqms.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsStatsQueryMapper;
import com.ruoyi.vqms.management.service.VqmsAvcStatsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * S4 读侧 L0：VqmsAvcStatsService——rollup 计数 → 页面行换算（率/罚款经
 * summarizeCounts 纯函数；容量=各组求和；gridSubject 恒全厂）。
 */
class VqmsAvcStatsServiceTest
{
    private final VqmsStatsQueryMapper queryMapper = mock(VqmsStatsQueryMapper.class);
    private final VqmsBusbarGroupMapper groupMapper = mock(VqmsBusbarGroupMapper.class);

    private VqmsAvcStatsService service;

    @BeforeEach
    void setUp()
    {
        service = new VqmsAvcStatsService();
        inject("statsQueryMapper", queryMapper);
        inject("groupMapper", groupMapper);

        VqmsBusbarGroup g = new VqmsBusbarGroup();
        g.setRatedCapacityKw(new java.math.BigDecimal("300000")); // 30 万千瓦
        when(groupMapper.selectList()).thenReturn(List.of(g));
    }

    private void inject(String field, Object value)
    {
        try
        {
            java.lang.reflect.Field f = VqmsAvcStatsService.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(service, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> regRow(int total, int qf, int pf, int ef, int inf,
            int qe, int pe, int ee, int ie)
    {
        Map<String, Object> row = new HashMap<>();
        row.put("statMonth", "2026-08");
        row.put("algorithmId", "V1_0");
        row.put("totalCmds", total);
        row.put("qualifiedFast", qf);
        row.put("penalizedFast", pf);
        row.put("exemptedFast", ef);
        row.put("invalidFast", inf);
        row.put("qualifiedEcon", qe);
        row.put("penalizedEcon", pe);
        row.put("exemptedEcon", ee);
        row.put("invalidEcon", ie);
        row.put("undecodableCount", 0);
        row.put("pendedCount", 0);
        row.put("excludedCount", 0);
        row.put("completenessSum", 0);
        return row;
    }

    @Test
    void regulationMonthly_mapsRatesAndPenalties_viaPureFunction()
    {
        // IT 同款布局：fast Q2/E1/I2、econ Q2/P1/E1/I1 → fastRate 40%、econRate 40%、
        // econ shortfall=20% → econPenalty 12 分；fast 无 penalized → 0 罚款
        when(queryMapper.selectRegulationMonthly(null)).thenReturn(
                List.of(regRow(5, 2, 0, 1, 2, 2, 1, 1, 1)));

        List<Map<String, Object>> rows = service.regulationMonthly(null);
        assertEquals(1, rows.size());
        Map<String, Object> page = rows.get(0);
        assertEquals("全厂", page.get("gridSubject"));
        assertEquals(5, page.get("cmdCount"));
        assertEquals(40.0, (Double) page.get("fastRate"), 1e-9);
        assertEquals(40.0, (Double) page.get("econRate"), 1e-9);
        assertEquals(0.0, (Double) page.get("fastPenalty"), 1e-9);
        assertEquals(12.0, (Double) page.get("econPenalty"), 0.001, "shortfall20%×30万kW×0.02");
        assertEquals(12.0, (Double) page.get("totalPenalty"), 0.001);
        assertEquals("V1_0", page.get("algorithmId"), "审计字段透出（MIXED 周期可见）");
    }

    @Test
    void runtimeMonthly_sumsOnGridMinutes_andPassesSnapshots()
    {
        Map<String, Object> row = new HashMap<>();
        row.put("statMonth", "2026-08");
        row.put("inServiceMin", 1100);
        row.put("exitGridMin", 100);
        row.put("exitNonGridMin", 60);
        row.put("offlineMin", 180);
        row.put("ratePct", new java.math.BigDecimal("94.828"));
        row.put("shortfallPct", new java.math.BigDecimal("4.172"));
        row.put("penaltyScore", new java.math.BigDecimal("2.503"));
        when(queryMapper.selectRuntimeMonthly(null)).thenReturn(List.of(row));

        List<Map<String, Object>> rows = service.runtimeMonthly(null);
        Map<String, Object> page = rows.get(0);
        assertEquals(1260, page.get("gridMinutes"), "并网运行=投运+两类退出（OFFLINE 不计）");
        assertEquals(1100, page.get("runtimeMinutes"));
        assertEquals("94.828", String.valueOf(page.get("runtimeRate")), "率快照直出不重算");
        assertEquals("4.172", String.valueOf(page.get("deficit")));
    }

    @Test
    void capacityMissing_penaltyZero_ratesUnchanged()
    {
        when(groupMapper.selectList()).thenReturn(List.of(new VqmsBusbarGroup()));
        when(queryMapper.selectRegulationMonthly(null)).thenReturn(
                List.of(regRow(4, 4, 0, 0, 0, 4, 0, 0, 0)));

        Map<String, Object> page = service.regulationMonthly(null).get(0);
        assertEquals(100.0, (Double) page.get("fastRate"), 1e-9);
        assertEquals(0.0, (Double) page.get("econPenalty"), 1e-9, "容量未配置罚款按 0 展示");
    }
}
