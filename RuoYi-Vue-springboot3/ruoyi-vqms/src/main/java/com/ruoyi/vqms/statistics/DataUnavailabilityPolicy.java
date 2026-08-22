package com.ruoyi.vqms.statistics;

/**
 * 数据不可用处置评估——参数化策略<b>纯函数</b>（v5.0 §8.7 / 草稿 v5_0 §2.9）。
 *
 * <p>甲/乙/丙/丁不是四套代码，是同一函数的四组 {@link PolicyConfig}；
 * 换策略 = 改配置表几行数据，本类零改动。判定器只出事实
 * （{@link RegulationOutcome}），「怎么记账」全部收敛在此。</p>
 *
 * <p>评估规则（优先级自上而下，恰好落一个 {@link Disposition} 桶）：</p>
 * <ol>
 *   <li>{@link RegulationOutcome.Undecodable} → undecodableMode；</li>
 *   <li>Judged 且 invalidTiers 非空（该档不可判族）→ invalidTierMode；</li>
 *   <li>Judged 且 completeness &lt; 1（部分缺）→ partialMissingMode
 *       （= EXCLUDE_REPORTED 时按阈值：completeness×100 ≥ 阈值则正常记账，否则剔除）；</li>
 *   <li>其余（数据齐全正常判）→ COUNT_NORMAL。</li>
 * </ol>
 *
 * <p>⚠ UNVERIFIED-口径（对抗验证吸收 2026-08-22）：阈值边界取「可用度 ≥ 阈值 = 正常记账」
 * （策略文档 §3.2 口径）——草稿 v5_0 §2.8 / v5.0 §8.6 的缺失率措辞「≥50% 整窗剔除」在边界点
 * 与之相反。选套未定，边界方向随政策拍板一并钉死，届时复核。</p>
 */
public final class DataUnavailabilityPolicy
{
    private DataUnavailabilityPolicy()
    {
    }

    /**
     * 纯函数：同一输入同一配置输出恒定；任一输入在任一配置下恰好落一个处置桶。
     */
    public static Disposition evaluate(RegulationOutcome outcome, PolicyConfig config)
    {
        if (outcome instanceof RegulationOutcome.Undecodable)
        {
            return config.undecodableMode();
        }
        RegulationOutcome.Judged judged = (RegulationOutcome.Judged) outcome;
        if (!judged.invalidTiers().isEmpty())
        {
            return config.invalidTierMode();
        }
        if (judged.completeness() < 1.0)
        {
            if (config.partialMissingMode() == Disposition.EXCLUDE_REPORTED)
            {
                double usablePct = judged.completeness() * 100.0;
                return usablePct >= config.partialMissingThresholdPct()
                        ? Disposition.COUNT_NORMAL
                        : Disposition.EXCLUDE_REPORTED;
            }
            return config.partialMissingMode();
        }
        return Disposition.COUNT_NORMAL;
    }
}
