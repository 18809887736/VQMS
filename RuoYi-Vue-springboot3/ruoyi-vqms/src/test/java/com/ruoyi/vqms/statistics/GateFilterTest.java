package com.ruoyi.vqms.statistics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.management.domain.VqmsYcPointMap;
import com.ruoyi.vqms.management.mapper.VqmsYcPointMapMapper;
import com.ruoyi.vqms.source.reader.YxSignalReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * S1 L0：GateFilter 门控前置过滤（v5.0 §8.8.2；测试方案 §5.0）。
 *
 * <p>gate_enabled=0 直通断言（已拍板空转）；置 1 后 AND 拦截语义。
 * mapper/reader 全 mock，零 DB。门控端到端联调（测试库置 gate_enabled=1）
 * 本体定义在 §4.2 门控 bullet，不在本类。</p>
 */
class GateFilterTest
{
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 23, 10, 0);

    private final VqmsYcPointMapMapper pointMapMapper = mock(VqmsYcPointMapMapper.class);
    private final YxSignalReader yxReader = mock(YxSignalReader.class);
    private final GateFilter filter = new GateFilter(pointMapMapper, yxReader);

    private static VqmsYcPointMap point(long ycNum, String type, int gateEnabled)
    {
        VqmsYcPointMap p = new VqmsYcPointMap();
        p.setYcNum(ycNum);
        p.setPointType(type);
        p.setGateEnabled(gateEnabled);
        return p;
    }

    @Test
    void noEnabledPoints_passThrough_neverTouchesReader()
    {
        // 种子现状：3009/2003 均 gate_enabled=0 → 直通（拍板空转），不发起任何信号读取
        when(pointMapMapper.selectList()).thenReturn(List.of(
                point(3009L, "yx", 0), point(2003L, "yx", 0), point(4001L, "busbar_id", 0)));
        assertTrue(filter.shouldJudge(T0), "无启用门控点应直通");
        verifyNoInteractions(yxReader);
    }

    @Test
    void enabledPoints_allHeldOne_judgeable()
    {
        when(pointMapMapper.selectList()).thenReturn(List.of(
                point(3009L, "yx", 1), point(2003L, "yx", 1)));
        when(yxReader.heldValue(eq(3009L), any())).thenReturn(Optional.of(1));
        when(yxReader.heldValue(eq(2003L), any())).thenReturn(Optional.of(1));
        assertTrue(filter.shouldJudge(T0), "投退=投入 且 远方 → 可判");
    }

    @Test
    void enabledPointZero_orMissingData_blocks()
    {
        when(pointMapMapper.selectList()).thenReturn(List.of(point(3009L, "yx", 1)));
        when(yxReader.heldValue(eq(3009L), any())).thenReturn(Optional.of(0));
        assertFalse(filter.shouldJudge(T0), "AVC 退出 → 拦截");

        when(yxReader.heldValue(eq(3009L), any())).thenReturn(Optional.empty());
        assertFalse(filter.shouldJudge(T0), "点前无数据（缺失）→ 从严拦截");
    }

    @Test
    void mixedPoints_andSemantics()
    {
        when(pointMapMapper.selectList()).thenReturn(List.of(
                point(3009L, "yx", 1), point(2003L, "yx", 1)));
        when(yxReader.heldValue(eq(3009L), any())).thenReturn(Optional.of(1));
        when(yxReader.heldValue(eq(2003L), any())).thenReturn(Optional.of(0));
        assertFalse(filter.shouldJudge(T0), "任一启用点=0 即拦截（AND）");
    }
}
