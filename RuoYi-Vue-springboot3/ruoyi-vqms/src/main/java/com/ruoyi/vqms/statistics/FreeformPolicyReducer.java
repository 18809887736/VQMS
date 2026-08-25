package com.ruoyi.vqms.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 戊·自由组合等价规约器（策略文档 §3.3.4 落地件，纯函数）。
 *
 * <p>检测规则表能否规约为「每事实轴一动作」的退化形——能则给出等价的
 * {@link PolicyConfig} 与预设名提示（如「≡乙」）供管理员自查误配；
 * 不能规约者才真正需要扩展求值器。规约是<b>保守的</b>：拿不准即返回 empty，
 * 宁可不提示也不误报等价。</p>
 */
public final class FreeformPolicyReducer
{
    private FreeformPolicyReducer()
    {
    }

    /**
     * @param presetCode 等价预设枚举名（JIA/YI/BING/DING）；规约成功但不对应任何命名预设时为 null
     */
    public record Reduction(PolicyConfig config, String presetCode)
    {
    }

    public static Optional<Reduction> reduce(FreeformPolicyConfig cfg)
    {
        Map<PolicyAtom.Axis, List<FreeformRule>> byAxis = new HashMap<>();
        Map<PolicyAtom.Axis, Integer> firstIndex = new HashMap<>();
        for (int i = 0; i < cfg.rules().size(); i++)
        {
            FreeformRule r = cfg.rules().get(i);
            PolicyAtom.Axis axis = singleAxis(r);
            if (axis == null)
            {
                return Optional.empty(); // 跨轴表达式——扩展求值器的领地
            }
            byAxis.computeIfAbsent(axis, k -> new java.util.ArrayList<>()).add(r);
            firstIndex.putIfAbsent(axis, i);
        }

        // 轴序须与固定优先链同构：UNDECODABLE < INVALID_TIER < PARTIAL_MISSING
        Integer uIdx = firstIndex.get(PolicyAtom.Axis.UNDECODABLE);
        Integer iIdx = firstIndex.get(PolicyAtom.Axis.INVALID_TIER);
        Integer pIdx = firstIndex.get(PolicyAtom.Axis.PARTIAL_MISSING);
        if (uIdx != null && iIdx != null && uIdx >= iIdx)
        {
            return Optional.empty();
        }
        if (iIdx != null && pIdx != null && iIdx >= pIdx)
        {
            return Optional.empty();
        }
        if (uIdx != null && pIdx != null && uIdx >= pIdx)
        {
            return Optional.empty();
        }

        // 解码失败轴：恰一条裸 A1 -> X（子类拆分/取反/组都不规约）——轴上有规则却形状不规约 = 整体拒绝
        List<FreeformRule> uRules = byAxis.get(PolicyAtom.Axis.UNDECODABLE);
        Disposition undecodable = uRules == null ? null : simpleAxisAction(uRules, PolicyAtom.A1);
        if (uRules != null && undecodable == null)
        {
            return Optional.empty();
        }

        // 档无效轴：恰一条裸 A2 -> Y
        List<FreeformRule> iRules = byAxis.get(PolicyAtom.Axis.INVALID_TIER);
        Disposition invalidTier = iRules == null ? null : simpleAxisAction(iRules, PolicyAtom.A2);
        if (iRules != null && invalidTier == null)
        {
            return Optional.empty();
        }

        // 部分缺轴：白名单形状（见 partialModeOf）
        PartialReduction partial = partialModeOf(byAxis.get(PolicyAtom.Axis.PARTIAL_MISSING), cfg.thresholdPct());
        if (partial == null)
        {
            return Optional.empty();
        }

        Disposition u = undecodable == null ? Disposition.COUNT_NORMAL : undecodable;
        Disposition v = invalidTier == null ? Disposition.COUNT_NORMAL : invalidTier;
        PolicyConfig config = new PolicyConfig(u, v, partial.mode(), partial.thresholdPct());

        String presetCode = null;
        for (PolicyPreset p : PolicyPreset.values())
        {
            if (p.config().equals(config))
            {
                presetCode = p.name();
                break;
            }
        }
        return Optional.of(new Reduction(config, presetCode));
    }

    /** 规则引用原子跨轴 → null（不可规约）；否则返回其唯一轴 */
    private static PolicyAtom.Axis singleAxis(FreeformRule rule)
    {
        PolicyAtom.Axis axis = null;
        for (PolicyAtom a : rule.expression().referencedAtoms())
        {
            if (axis == null)
            {
                axis = a.axis();
            }
            else if (axis != a.axis())
            {
                return null;
            }
        }
        return axis;
    }

    /** 轴内恰一条「裸单原子 -> X」规则时返回 X，否则 null */
    private static Disposition simpleAxisAction(List<FreeformRule> rules, PolicyAtom atom)
    {
        if (rules == null || rules.size() != 1)
        {
            return null;
        }
        FreeformRule r = rules.get(0);
        List<PolicyExpression.Node> ops = r.expression().operands();
        if (ops.size() == 1 && ops.get(0) instanceof PolicyExpression.AtomTerm t
                && !t.negated() && t.atom() == atom)
        {
            return r.action();
        }
        return null;
    }

    private record PartialReduction(Disposition mode, Integer thresholdPct)
    {
    }

    /**
     * 部分缺轴白名单：
     * {A3->COUNT_NORMAL} ≡ 甲部分缺；{A3->计不合格}/{A3->挂起} ≡ 丙/丁部分缺；
     * {A3&A4->剔除}（可叠加 {A3&!A4->正常记账} 冗余补全）≡ 乙式阈值剔除@τ；
     * 其余形状（含 A3->剔除、A4 单独成条等）一律不可规约。
     */
    private static PartialReduction partialModeOf(List<FreeformRule> rules, int tau)
    {
        if (rules == null)
        {
            return new PartialReduction(Disposition.COUNT_NORMAL, null);
        }
        Disposition allMissing = null;      // shape A3 -> X
        Disposition belowThreshold = null;  // shape A3 & A4 -> EXCLUDE
        Disposition aboveThreshold = null;  // shape A3 & !A4 -> COUNT_NORMAL
        for (FreeformRule r : rules)
        {
            if (isPlainPair(r.expression(), PolicyExpression.Join.AND,
                    PolicyAtom.A3, false, PolicyAtom.A4, false))
            {
                if (!"EXCLUDE_REPORTED".equals(r.action().name()) || belowThreshold != null)
                {
                    return null;
                }
                belowThreshold = r.action();
            }
            else if (isPlainPair(r.expression(), PolicyExpression.Join.AND,
                    PolicyAtom.A3, false, PolicyAtom.A4, true))
            {
                if (r.action() != Disposition.COUNT_NORMAL || aboveThreshold != null)
                {
                    return null;
                }
                aboveThreshold = r.action();
            }
            else if (isPlainSingle(r.expression(), PolicyAtom.A3))
            {
                if (r.action() == Disposition.EXCLUDE_REPORTED || allMissing != null)
                {
                    return null; // 无阈值限定的「有缺即剔除」≠ PolicyConfig 任何模式
                }
                allMissing = r.action();
            }
            else
            {
                return null;
            }
        }
        if (belowThreshold != null)
        {
            return new PartialReduction(Disposition.EXCLUDE_REPORTED, tau); // τ 即全局值
        }
        if (allMissing != null)
        {
            return new PartialReduction(allMissing, null);
        }
        return new PartialReduction(Disposition.COUNT_NORMAL, null);
    }

    private static boolean isPlainSingle(PolicyExpression e, PolicyAtom atom)
    {
        List<PolicyExpression.Node> ops = e.operands();
        return ops.size() == 1 && ops.get(0) instanceof PolicyExpression.AtomTerm t
                && !t.negated() && t.atom() == atom;
    }

    /** 裸二元组 A&B / A&!B（无取反包裹、无多余操作数） */
    private static boolean isPlainPair(PolicyExpression e, PolicyExpression.Join join,
            PolicyAtom left, boolean leftNegated, PolicyAtom right, boolean rightNegated)
    {
        List<PolicyExpression.Node> ops = e.operands();
        if (e.join() != join || ops.size() != 2)
        {
            return false;
        }
        return matches(ops.get(0), left, leftNegated) && matches(ops.get(1), right, rightNegated);
    }

    private static boolean matches(PolicyExpression.Node n, PolicyAtom atom, boolean negated)
    {
        return n instanceof PolicyExpression.AtomTerm t && t.atom() == atom && t.negated() == negated;
    }
}
