package com.ruoyi.vqms.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 确定性占位判定实现（搁置轨 stub，v5.0 §8.2 / 正式 v1_0 §2.5）。
 *
 * <p><b>契约级部分为真</b>（输出契约的稳定面，D9 完成标准「Undecodable 按原因分类、
 * completeness 如实上报」要求）：</p>
 * <ul>
 *   <li>解码<b>分类</b>：目标值/增量形态识别 + 三类失败归因——只判"能不能解"，
 *       分类逻辑已上移 {@link VTargetDecoder}（S1），本类只调用；</li>
 *   <li>窗口结构统计：completeness（[1..t_econ] 有数据分钟占比，如实上报不处置）、
 *       invalidTiers（该档整档全缺或 L>H → 该档不可判，正式版 §2.5 两种成因）；</li>
 * </ul>
 *
 * <p><b>算法核心部分为占位</b>：有效档的 VERDICT 一律 {@link Verdict#QUALIFIED}
 * （确定性、禁随机、<b>不保证正确</b>）。真实包络判定由 {@link DefaultRegulationJudge}
 * 承担（S1 已交付，双注册经 {@code vqms.judge.algorithm} 选择——调用方零改动）。
 * {@link #isStub()} 恒 true。</p>
 */
public class StubRegulationJudge implements RegulationJudge
{
    @Override
    public boolean isStub()
    {
        return true;
    }

    @Override
    public RegulationOutcome judge(AvcCommand cmd, List<MinuteCurve> curves, JudgeParams params)
    {
        Optional<DecodeFailureReason> reason = VTargetDecoder.classify(cmd);
        if (reason.isPresent())
        {
            return new RegulationOutcome.Undecodable(reason.get());
        }

        // 窗口结构统计（契约级，为真）：completeness + 按档无效
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
        for (Tier tier : Tier.values())
        {
            int lo = tier == Tier.FAST ? 1 : params.tFast() + 1;
            int hi = tier == Tier.FAST ? params.tFast() : params.tEcon();
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            boolean any = false;
            for (MinuteCurve c : curves)
            {
                if (c.minuteOffset() >= lo && c.minuteOffset() <= hi)
                {
                    any = true;
                    low = Math.min(low, c.lowSv());
                    high = Math.max(high, c.highSv());
                }
            }
            if (!any || low > high)
            {
                // 整档全缺（S14）/ L>H 数据异常（S16）：该档不可判，VERDICT 必空（构造期不变式）
                invalidTiers.add(tier);
            }
        }

        // 判定结论占位：有效档一律 QUALIFIED（stub=true，禁随机、不保证正确；S1 替换点）
        return new RegulationOutcome.Judged(
                verdictFor(Tier.FAST, invalidTiers),
                verdictFor(Tier.ECON, invalidTiers),
                completeness, invalidTiers);
    }

    private Optional<Verdict> verdictFor(Tier tier, Set<Tier> invalidTiers)
    {
        return invalidTiers.contains(tier) ? Optional.empty() : Optional.of(Verdict.QUALIFIED);
    }
}
