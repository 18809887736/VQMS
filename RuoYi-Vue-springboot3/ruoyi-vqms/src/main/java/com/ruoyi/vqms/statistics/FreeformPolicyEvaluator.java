package com.ruoyi.vqms.statistics;

/**
 * 戊·自由组合求值器（策略文档 §3.3.2，纯函数；与 {@link DataUnavailabilityPolicy}
 * 同族——输入事实 + 生效配置，输出恰好一个处置桶）。
 *
 * <p>规则表首中即断；全不中 → 兜底 COUNT_NORMAL（ruleId=null）。
 * 留痕底线沿用：调用方对「命中不可用事实却落正常记账」的指令照常按归因键计数，
 * 不因兜底路径静默（§3.3.2 留痕条）。</p>
 */
public final class FreeformPolicyEvaluator
{
    private FreeformPolicyEvaluator()
    {
    }

    /**
     * 求值结果：处置桶 + 命中规则 ID（兜底时为 null——拟议「命中规则 ID」留痕列的消费口径）。
     */
    public record Decision(Disposition disposition, String ruleId)
    {
    }

    public static Decision evaluate(RegulationOutcome outcome, FreeformPolicyConfig config)
    {
        for (FreeformRule rule : config.rules())
        {
            if (rule.expression().eval(outcome, config.thresholdPct()))
            {
                return new Decision(rule.action(), rule.ruleId());
            }
        }
        return new Decision(Disposition.COUNT_NORMAL, null);
    }
}
