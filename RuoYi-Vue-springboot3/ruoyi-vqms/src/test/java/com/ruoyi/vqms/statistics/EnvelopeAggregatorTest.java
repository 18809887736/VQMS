package com.ruoyi.vqms.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1 L0：EnvelopeAggregator 单元 + 属性（测试方案 §5.0 组件矩阵）。
 *
 * <p>min/max 窗口、空窗、单分钟窗；属性：窗口极值保持——子窗聚合区间 ⊆ 全窗聚合区间。
 * {@code TierEmpty}/{@code L>H} → invalidTiers 的映射由 judge 契约测试断言（§4.2）。</p>
 */
class EnvelopeAggregatorTest
{
    private static MinuteCurve c(int offset, int high, int low)
    {
        return new MinuteCurve(offset, high, low);
    }

    @Test
    void emptyWindow_returnsEmpty()
    {
        assertEquals(Optional.empty(), EnvelopeAggregator.aggregate(List.of(), 1, 4));
    }

    @Test
    void windowWithoutRows_returnsEmpty()
    {
        // 行全在窗外（偏移 5+）→ [1,4] 窗视为整档缺
        assertEquals(Optional.empty(),
                EnvelopeAggregator.aggregate(List.of(c(5, 235, 233), c(6, 235, 233)), 1, 4));
    }

    @Test
    void singleMinute_minMaxFromThatRow()
    {
        Optional<EnvelopeAggregator.Envelope> env =
                EnvelopeAggregator.aggregate(List.of(c(3, 226, 222)), 1, 4);
        assertEquals(new EnvelopeAggregator.Envelope(222, 226), env.orElseThrow());
    }

    @Test
    void multiMinute_extremesAcrossRows()
    {
        // 正式版 §2.3：L = min(low)，H = max(high)——极值可来自不同行
        Optional<EnvelopeAggregator.Envelope> env = EnvelopeAggregator.aggregate(
                List.of(c(1, 224, 223), c(2, 225, 221), c(3, 223, 222), c(4, 225, 224)), 1, 4);
        assertEquals(new EnvelopeAggregator.Envelope(221, 225), env.orElseThrow());
    }

    @Test
    void windowSlicing_excludesOutsideOffsets()
    {
        // 两档无缝拼接：fast 窗 [1,4] 不吃 econ 行、econ 窗 [5,5] 不吃 fast 行
        List<MinuteCurve> curves = List.of(c(1, 230, 228), c(4, 231, 229), c(5, 240, 235));
        assertEquals(new EnvelopeAggregator.Envelope(228, 231),
                EnvelopeAggregator.aggregate(curves, 1, 4).orElseThrow());
        assertEquals(new EnvelopeAggregator.Envelope(235, 240),
                EnvelopeAggregator.aggregate(curves, 5, 5).orElseThrow());
    }

    @Test
    void lowGreaterThanHigh_preservedAsIs()
    {
        // S16 数据异常：聚合器只上报不判定，L>H 原样交 judge 映射 invalidTiers
        Optional<EnvelopeAggregator.Envelope> env =
                EnvelopeAggregator.aggregate(List.of(c(2, 10, 20)), 1, 4);
        assertEquals(Optional.of(new EnvelopeAggregator.Envelope(20, 10)), env);
    }

    /** 属性：窗口极值保持——任意子窗的聚合区间 ⊆ 全窗聚合区间（单调收缩性）。 */
    @Property
    void subsetWindow_envelopeContainedInFull(@ForAll List<
            @IntRange(min = 1, max = 8) Integer> offsets,
            @ForAll @IntRange(min = -50, max = 50) int base)
    {
        // 构造去重偏移的曲线行（每分钟一行，high=low+5）
        List<MinuteCurve> curves = new ArrayList<>();
        offsets.stream().distinct().forEach(o -> curves.add(c(o, base + o + 5, base + o)));
        if (curves.isEmpty())
        {
            return;
        }
        EnvelopeAggregator.Envelope full =
                EnvelopeAggregator.aggregate(curves, 1, 8).orElseThrow();
        Optional<EnvelopeAggregator.Envelope> sub = EnvelopeAggregator.aggregate(curves, 2, 5);
        assertTrue(sub.isEmpty()
                        || (sub.get().low() >= full.low() && sub.get().high() <= full.high()),
                () -> "子窗[2,5]=" + sub + " ⊆ 全窗=" + full);
    }
}
