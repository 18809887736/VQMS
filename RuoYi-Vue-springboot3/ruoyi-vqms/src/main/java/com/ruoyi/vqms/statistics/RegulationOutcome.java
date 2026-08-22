package com.ruoyi.vqms.statistics;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * judge 输出契约（sealed，草稿 v5_0 §2.5 / v5.0 §8.2）。
 *
 * <p>两态 + 判不了信号：<b>Undecodable 不是第三判定态</b>——它是输入有效性信号
 * （解码失败如实上报），处置决策归参数化策略层（{@code DataUnavailabilityPolicy}）。</p>
 *
 * <p>judge 不做任何处置决策（不吞信息、不自作主张）；免考不在此（阶段三后置读 yx501）。</p>
 */
public sealed interface RegulationOutcome permits RegulationOutcome.Judged, RegulationOutcome.Undecodable
{

    /**
     * 判了：两档平行结论 + 窗口完整度 + 按档无效标记。
     *
     * <p><b>构造期不变式（2026-08-19 Leo 拍板，compact constructor 强制）</b>：
     * 每档「VERDICT 为空 ⟺ 该档 ∈ invalidTiers」——非空枚举装不下"不可判"，
     * 可空性让消费方在类型层面读不到无效档结论。违背即抛 IllegalArgumentException。</p>
     */
    record Judged(Optional<Verdict> fast, Optional<Verdict> econ,
                  double completeness, Set<Tier> invalidTiers) implements RegulationOutcome
    {
        public Judged
        {
            Objects.requireNonNull(fast, "fast 不可为 null（用 Optional.empty()）");
            Objects.requireNonNull(econ, "econ 不可为 null（用 Optional.empty()）");
            Objects.requireNonNull(invalidTiers, "invalidTiers 不可为 null（用 Set.of()）");
            invalidTiers = Set.copyOf(invalidTiers);
            if (completeness < 0.0 || completeness > 1.0 || Double.isNaN(completeness))
            {
                throw new IllegalArgumentException("completeness 须 ∈ [0,1]: " + completeness);
            }
            for (Tier tier : Tier.values())
            {
                Optional<Verdict> verdict = tier == Tier.FAST ? fast : econ;
                if (verdict.isEmpty() != invalidTiers.contains(tier))
                {
                    throw new IllegalArgumentException("不变式违背（VERDICT 为空 ⟺ 该档 ∈ invalidTiers）: "
                            + tier + " verdict=" + verdict + ", invalidTiers=" + invalidTiers);
                }
            }
        }
    }

    /**
     * 判不了：解码失败，带原因分类（循环码非法 / 缺t₀电压 / 编码脏写）。
     * 不携带 completeness——窗口数据存在与否与解码无关（缺 t₀ 但窗口齐全照样判不了），
     * 两类信息互斥由 sealed 结构性排除。
     */
    record Undecodable(DecodeFailureReason reason) implements RegulationOutcome
    {
        public Undecodable
        {
            Objects.requireNonNull(reason, "reason 不可为 null");
        }
    }
}
