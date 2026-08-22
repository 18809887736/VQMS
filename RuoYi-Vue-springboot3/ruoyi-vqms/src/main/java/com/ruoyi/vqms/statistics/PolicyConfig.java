package com.ruoyi.vqms.statistics;

import java.util.Objects;

/**
 * 数据不可用策略配置（v5.0 §8.7 参数化：甲/乙/丙/丁 = 同一纯函数的四组配置，不是四个类）。
 *
 * <p>载体 = {@code vqms_policy_param} 表（param_value varchar），<b>选套值留空待政策拍板</b>
 * （Leo 2026-08-18 拍板：不预设处置值、不当默认值）；拍板后改表几行即换策略，代码不动。
 * 甲/乙/丙/丁四套候选向量只存在于<b>测试侧</b>（PolicyFunctionVectorTest，v5.0 §8.6
 * 「甲/乙/丙/丁不硬编码进任何代码」）——main 侧无任何预设配置。</p>
 *
 * @param undecodableMode             Undecodable 指令（解码失败）的处置
 * @param invalidTierMode             Judged 且 invalidTiers 非空（该档整档全缺 / L>H，与缺数据同族）的处置
 * @param partialMissingMode          Judged 且 completeness &lt; 1（部分缺分钟）的处置
 * @param partialMissingThresholdPct  部分缺可用度阈值（百分比整数，如乙档 50）；
 *                                    仅当 partialMissingMode = EXCLUDE_REPORTED 时必填
 */
public record PolicyConfig(Disposition undecodableMode,
                           Disposition invalidTierMode,
                           Disposition partialMissingMode,
                           Integer partialMissingThresholdPct)
{
    public PolicyConfig
    {
        Objects.requireNonNull(undecodableMode, "undecodableMode 不可为 null");
        Objects.requireNonNull(invalidTierMode, "invalidTierMode 不可为 null");
        Objects.requireNonNull(partialMissingMode, "partialMissingMode 不可为 null");
        if (partialMissingMode == Disposition.EXCLUDE_REPORTED)
        {
            Objects.requireNonNull(partialMissingThresholdPct,
                    "partialMissingMode=EXCLUDE_REPORTED 须提供阈值 partialMissingThresholdPct");
            if (partialMissingThresholdPct < 0 || partialMissingThresholdPct > 100)
            {
                throw new IllegalArgumentException("阈值须 ∈ [0,100]: " + partialMissingThresholdPct);
            }
        }
    }
}
