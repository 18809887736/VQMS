package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * yx/yc 阶跃保持量读取契约（v5.0 §8.8.2 S1 新增；正式 v1_0 §1.2 同族语义）。
 *
 * <p>五类信号点共用一个读取语义：投退（yx1001/合成 yc3009）、远方就地总（yx2003）、
 * 并网（yc511/512）、退出原因（yc521/522）、免考旗（yx501）——均为阶跃保持量，
 * 取该点 <b>≤ 当分钟最近一条</b>值。方言切换同 D1 模式（当前 yc_history 实现）。</p>
 */
public interface YxSignalReader
{
    /**
     * @return 该点在 atMinute 时点的阶跃保持值；该点此前无任何数据 → empty
     */
    Optional<Integer> heldValue(Long pointNum, LocalDateTime atMinute);

    /**
     * 同 {@link #heldValue} 的十进制原值版——模拟量点位（如 t₀ 实时电压 234.25 kV）用，
     * 开关量点位走 {@link #heldValue} 即可。
     */
    Optional<Double> heldDecimalValue(Long pointNum, LocalDateTime atMinute);
}
