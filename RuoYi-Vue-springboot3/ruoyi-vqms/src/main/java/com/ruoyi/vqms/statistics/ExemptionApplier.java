package com.ruoyi.vqms.statistics;

import java.util.Objects;
import java.util.Optional;

/**
 * 免考后置应用（正式 v1_0 §2.6 apply_exemption / §2.0 阶段三）——参数化策略之外的
 * 无争议部分，纯函数。
 *
 * <p>逐档规则（两档独立、结论不跨档）：{@code PENALIZED ∧ yx501=1 → EXEMPTED}；
 * {@code PENALIZED ∧ yx501=0 → PENALIZED}（计罚）；QUALIFIED / invalidTiers 档原样透传。</p>
 *
 * <p><b>类型堵（§2.1）</b>：签名只收 {@link RegulationOutcome.Judged}——
 * {@code Undecodable} 是 sealed 另一分支、编译期进不了本组件；其不经免考的分流
 * （占位合成 + 策略处置）归 {@code RegulationPipeline} 路由（v5.0 §8.8.2），
 * 由 Pipeline 集成断言（测试方案 §5.0），不在本组件测试范围。</p>
 */
public final class ExemptionApplier
{
    private ExemptionApplier()
    {
    }

    /**
     * @param yx501 免考旗在该指令时点的阶跃保持值（正式版 §2.6），合法域 {0, 1}
     */
    public static TierFinalDisposition apply(RegulationOutcome.Judged judged, int yx501)
    {
        Objects.requireNonNull(judged, "judged 不可为 null");
        if (yx501 != 0 && yx501 != 1)
        {
            throw new IllegalArgumentException("yx501 合法域 {0,1}: " + yx501);
        }
        return new TierFinalDisposition(map(judged.fast(), yx501), map(judged.econ(), yx501));
    }

    private static FinalTierState map(Optional<Verdict> verdict, int yx501)
    {
        if (verdict.isEmpty())
        {
            return FinalTierState.INVALID;
        }
        return verdict.get() == Verdict.QUALIFIED ? FinalTierState.QUALIFIED
                : yx501 == 1 ? FinalTierState.EXEMPTED : FinalTierState.PENALIZED;
    }
}
