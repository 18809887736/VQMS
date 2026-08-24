package com.ruoyi.vqms.statistics;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 正式包络判定实现（注册 ID {@code V1_0}，正式 v1_0 §2.4~2.5；v5.0 §8.8.2 S1）。
 *
 * <p>三阶段管线的阶段二（纯判定）：解码（{@link VTargetDecoder}）→ 两档平行包络判定
 * （{@link EnvelopeAggregator} + 区间夹住比较）。门控前置过滤、免考后置应用、
 * 数据不可用处置都不在这里——judge 不做任何处置决策，缺多少、为什么判不了如实上报。</p>
 *
 * <p>纯函数：零 IO、零 Spring 依赖。与 {@code STUB} 双注册经
 * {@code vqms.judge.algorithm} 选择（v5.0 §8.8.5），装配见
 * {@link RegulationJudgeConfig}。</p>
 */
public class DefaultRegulationJudge implements RegulationJudge
{
    @Override
    public boolean isStub()
    {
        return false;
    }

    @Override
    public RegulationOutcome judge(AvcCommand cmd, List<MinuteCurve> curves, JudgeParams params)
    {
        Optional<DecodeFailureReason> reason = VTargetDecoder.classify(cmd);
        if (reason.isPresent())
        {
            return new RegulationOutcome.Undecodable(reason.get());
        }
        double vTarget = VTargetDecoder.decode(cmd);

        Set<Integer> presentOffsets = new HashSet<>();
        for (MinuteCurve c : curves)
        {
            if (c.minuteOffset() >= 1 && c.minuteOffset() <= params.tEcon())
            {
                presentOffsets.add(c.minuteOffset());
            }
        }
        double completeness = (double) presentOffsets.size() / params.tEcon();

        Set<Tier> invalidTiers = new HashSet<>();
        Map<Tier, Verdict> verdicts = new EnumMap<>(Tier.class);
        for (Tier tier : Tier.values())
        {
            int lo = tier == Tier.FAST ? 1 : params.tFast() + 1;
            int hi = tier == Tier.FAST ? params.tFast() : params.tEcon();
            Optional<EnvelopeAggregator.Envelope> envelope = EnvelopeAggregator.aggregate(curves, lo, hi);
            if (envelope.isEmpty() || envelope.get().low() > envelope.get().high())
            {
                // 整档全缺（S14）/ L>H 数据异常（S16）：该档不可判，VERDICT 必空（构造期不变式）
                invalidTiers.add(tier);
                continue;
            }
            verdicts.put(tier,
                    contains(envelope.get(), vTarget) ? Verdict.QUALIFIED : Verdict.PENALIZED);
        }

        return new RegulationOutcome.Judged(
                Optional.ofNullable(verdicts.get(Tier.FAST)),
                Optional.ofNullable(verdicts.get(Tier.ECON)),
                completeness, invalidTiers);
    }

    /** 包络夹住判定：L ≤ V_target ≤ H（闭区间，边界相接=合格，正式版 §2.3/§2.4.3）。 */
    private static boolean contains(EnvelopeAggregator.Envelope envelope, double vTarget)
    {
        return envelope.low() <= vTarget && vTarget <= envelope.high();
    }
}
