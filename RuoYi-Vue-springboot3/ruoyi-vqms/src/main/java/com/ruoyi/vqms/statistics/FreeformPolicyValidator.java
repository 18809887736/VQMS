package com.ruoyi.vqms.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 戊·自由组合应用校验（策略文档 §3.3.2 fail-fast 清单，纯函数）。
 *
 * <p>任一不满足 → errors 非空，Service 层整体拒绝应用、原生效策略保持不变。</p>
 */
public final class FreeformPolicyValidator
{
    private FreeformPolicyValidator()
    {
    }

    /** vqms_policy_param.param_value varchar(255) 存储上限（canonical 行 + " -> " + 动作） */
    public static final int MAX_STORED_LENGTH = 255;

    /**
     * @param ruleLines   规则行原文（"表达式 -> 动作"），顺序即规则表序
     * @param thresholdPct 全局 τ（调用方已填默认 50；此处只做范围与依赖校验）
     */
    public static Validation validate(List<String> ruleLines, int thresholdPct)
    {
        List<String> errors = new ArrayList<>();
        if (ruleLines == null || ruleLines.isEmpty())
        {
            errors.add("规则表至少一条");
            return new Validation(List.copyOf(errors), List.of());
        }
        if (ruleLines.size() > FreeformPolicyConfig.MAX_RULES)
        {
            errors.add("规则数超上限 " + FreeformPolicyConfig.MAX_RULES + ": " + ruleLines.size());
        }
        if (thresholdPct < 0 || thresholdPct > 100)
        {
            errors.add("阈值 τ 须 ∈ [0,100]: " + thresholdPct);
        }

        boolean anyA4 = false;
        Set<String> seenCanonical = new HashSet<>();
        List<FreeformRule> rules = new ArrayList<>();
        for (int i = 0; i < ruleLines.size(); i++)
        {
            String line = ruleLines.get(i);
            int lineNo = i + 1;
            final FreeformRule parsed;
            try
            {
                parsed = FreeformPolicyParser.parseRule(line);
            }
            catch (IllegalArgumentException e)
            {
                errors.add("第 " + lineNo + " 行: " + e.getMessage());
                continue;
            }
            String canonicalLine = storedValue(parsed);
            if (canonicalLine.length() > MAX_STORED_LENGTH)
            {
                errors.add("第 " + lineNo + " 行: 规范化后超存储上限 " + MAX_STORED_LENGTH
                        + " 字符（现 " + canonicalLine.length() + "）");
            }
            if (!seenCanonical.add(parsed.expressionText()))
            {
                errors.add("第 " + lineNo + " 行: 与先前规则表达式完全相同（" + parsed.expressionText() + "）");
            }
            if (parsed.action() == Disposition.COUNT_NORMAL
                    && !parsed.expression().referencedAtoms().contains(PolicyAtom.A3))
            {
                errors.add("第 " + lineNo + " 行: 处置动作 COUNT_NORMAL（用剩余正常记账）仅当触发含 A3 时可选");
            }
            if (parsed.expression().referencedAtoms().contains(PolicyAtom.A4))
            {
                anyA4 = true;
            }
            rules.add(parsed.withRuleId(ruleId(i)));
        }
        // A4 引用即依赖全局 τ——τ 恒有值（默认 50），此处仅声明语义关联，无额外错误路径

        return new Validation(List.copyOf(errors), rules.isEmpty() ? List.of() : List.copyOf(rules));
    }

    /** 规则行存储值（= param_value）：规范文本 + " -> " + 动作 */
    public static String storedValue(FreeformRule rule)
    {
        return rule.expressionText() + " -> " + rule.action().name();
    }

    /** R001…（按序零补三位）；加载侧按行号同式重建，两侧口径一致 */
    public static String ruleId(int zeroBasedIndex)
    {
        return String.format("R%03d", zeroBasedIndex + 1);
    }

    /**
     * @param errors 非空即整体拒绝应用
     * @param rules  解析成功且已赋 ruleId 的规则表（errors 非空时内容不完整，不得使用）
     */
    public record Validation(List<String> errors, List<FreeformRule> rules)
    {
        public boolean ok()
        {
            return errors.isEmpty();
        }

        public boolean hasErrors()
        {
            return !errors.isEmpty();
        }
    }
}
