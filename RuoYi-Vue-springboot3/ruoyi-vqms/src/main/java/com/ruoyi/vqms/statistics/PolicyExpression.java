package com.ruoyi.vqms.statistics;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 戊·自由组合触发表达式（策略文档 §3.3.2 语法树，不可变纯数据）。
 *
 * <p>语法（解析器强制，本类只承载结果）：</p>
 * <ul>
 *   <li>顶层：操作数经<b>单一连接词</b>（AND/OR）连接——混用须经一层括号分组；</li>
 *   <li>括号组：组内为原子/取反原子的平坦列表 + 组内连接词，<b>嵌套上限一层</b>；</li>
 *   <li>NOT 仅作用于单个原子或一个括号组。</li>
 * </ul>
 *
 * <p>求值语义：AND=全真、OR=任一真、NOT=异或取反；短路无副作用（原子求值本身纯）。</p>
 */
public final class PolicyExpression
{
    public enum Join { AND, OR }

    /** 叶子：原子 ± 取反 */
    public record AtomTerm(PolicyAtom atom, boolean negated) implements Node
    {
        public AtomTerm
        {
            Objects.requireNonNull(atom, "atom 不可为 null");
        }
    }

    /** 括号组：平坦原子项列表 + 组内连接词 ± 整组取反（NOT(…)） */
    public record GroupTerm(Join join, List<AtomTerm> atoms, boolean negated) implements Node
    {
        public GroupTerm
        {
            Objects.requireNonNull(join, "join 不可为 null");
            atoms = List.copyOf(atoms);
            if (atoms.isEmpty())
            {
                throw new IllegalArgumentException("括号组不可为空");
            }
            Objects.requireNonNull(negated);
        }
    }

    public sealed interface Node permits AtomTerm, GroupTerm
    {
    }

    private final Join join;
    private final List<Node> operands;

    private PolicyExpression(Join join, List<Node> operands)
    {
        Objects.requireNonNull(join, "join 不可为 null");
        this.operands = List.copyOf(operands);
        if (this.operands.isEmpty())
        {
            throw new IllegalArgumentException("表达式至少含一个操作数");
        }
        this.join = join;
    }

    public static PolicyExpression of(Join join, List<Node> operands)
    {
        return new PolicyExpression(join, operands);
    }

    /** 单操作数便捷构造（单原子 / 单括号组 / 取反形式），join 无语义取 AND */
    public static PolicyExpression single(Node node)
    {
        return new PolicyExpression(Join.AND, List.of(node));
    }

    public Join join()
    {
        return join;
    }

    public List<Node> operands()
    {
        return operands;
    }

    /** 纯函数求值：thresholdPct 即全局 τ（A4 消费）。 */
    public boolean eval(RegulationOutcome outcome, int thresholdPct)
    {
        boolean folded = join == Join.AND;
        for (Node n : operands)
        {
            boolean v = evalNode(n, outcome, thresholdPct);
            folded = join == Join.AND ? (folded && v) : (folded || v);
        }
        return folded;
    }

    private static boolean evalNode(Node n, RegulationOutcome outcome, int thresholdPct)
    {
        if (n instanceof AtomTerm a)
        {
            return a.atom().eval(outcome, thresholdPct) ^ a.negated();
        }
        GroupTerm g = (GroupTerm) n;
        boolean folded = g.join() == Join.AND;
        for (AtomTerm t : g.atoms())
        {
            boolean v = t.atom().eval(outcome, thresholdPct) ^ t.negated();
            folded = g.join() == Join.AND ? (folded && v) : (folded || v);
        }
        return folded ^ g.negated();
    }

    /** 引用的原子集合（去重保序）——A3 动作约束 / A4 阈值依赖 / 轴归约都消费它。 */
    public Set<PolicyAtom> referencedAtoms()
    {
        Set<PolicyAtom> out = new LinkedHashSet<>();
        collect(operands, out);
        return Collections.unmodifiableSet(out);
    }

    private static void collect(List<Node> nodes, Set<PolicyAtom> out)
    {
        for (Node n : nodes)
        {
            if (n instanceof AtomTerm a)
            {
                out.add(a.atom());
            }
            else
            {
                GroupTerm g = (GroupTerm) n;
                for (AtomTerm t : g.atoms())
                {
                    out.add(t.atom());
                }
            }
        }
    }

    /** 规范文本（大写、符号连接词、可被解析器原样重读）——重复规则检测与存储值共用。 */
    public String canonical()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < operands.size(); i++)
        {
            if (i > 0)
            {
                sb.append(join == Join.AND ? " & " : " | ");
            }
            appendNode(sb, operands.get(i));
        }
        return sb.toString();
    }

    private static void appendNode(StringBuilder sb, Node n)
    {
        if (n instanceof AtomTerm a)
        {
            if (a.negated())
            {
                sb.append('!');
            }
            sb.append(a.atom().name());
        }
        else
        {
            GroupTerm g = (GroupTerm) n;
            if (g.negated())
            {
                sb.append('!');
            }
            sb.append('(');
            for (int i = 0; i < g.atoms().size(); i++)
            {
                AtomTerm t = g.atoms().get(i);
                if (i > 0)
                {
                    sb.append(g.join() == Join.AND ? " & " : " | ");
                }
                if (t.negated())
                {
                    sb.append('!');
                }
                sb.append(t.atom().name());
            }
            sb.append(')');
        }
    }
}
