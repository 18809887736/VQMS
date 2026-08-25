package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Objects;

/**
 * 戊·自由组合生效配置：有序规则表 + 全局 τ（策略文档 §3.3.2）。
 *
 * <p>求值契约：规则表<b>首中即断</b>；全不中 → 正常记账（兜底，见
 * {@link FreeformPolicyEvaluator}）。τ 全局单值，同一生效集内唯一。</p>
 */
public record FreeformPolicyConfig(List<FreeformRule> rules, int thresholdPct)
{
    /** 规则表规模上限（防配置爆炸；16 条远超现实需要） */
    public static final int MAX_RULES = 16;

    public FreeformPolicyConfig
    {
        rules = List.copyOf(rules);
        if (rules.isEmpty())
        {
            throw new IllegalArgumentException("规则表至少一条");
        }
        if (rules.size() > MAX_RULES)
        {
            throw new IllegalArgumentException("规则数超上限 " + MAX_RULES + ": " + rules.size());
        }
        if (thresholdPct < 0 || thresholdPct > 100)
        {
            throw new IllegalArgumentException("阈值 τ 须 ∈ [0,100]: " + thresholdPct);
        }
    }
}
