package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Objects;

/**
 * 戊·自由组合单条规则：{@code 触发表达式 -> 处置动作}（策略文档 §3.3.2）。
 *
 * @param ruleId         规则标识（R001…，按规则表序赋值；null=尚未赋值——审计留痕「命中规则 ID」消费它）
 * @param expressionText 规范文本（canonical，存储/比对口径）
 * @param expression     解析后的表达式树
 * @param action         处置动作 ∈ {@link Disposition} 四桶
 */
public record FreeformRule(String ruleId, String expressionText,
        PolicyExpression expression, Disposition action)
{
    public FreeformRule
    {
        Objects.requireNonNull(expression, "expression 不可为 null");
        Objects.requireNonNull(action, "action 不可为 null");
        if (expressionText == null || expressionText.isBlank())
        {
            throw new IllegalArgumentException("expressionText 不可为空");
        }
    }

    public FreeformRule withRuleId(String newId)
    {
        return new FreeformRule(newId, expressionText, expression, action);
    }
}
