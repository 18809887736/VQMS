package com.ruoyi.vqms.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 戊·自由组合规则行解析器（策略文档 §3.3.2 语法的唯一权威实现，纯函数）。
 *
 * <p>规则行格式：{@code <表达式> -> <处置动作>}，例：
 * {@code A1 | (A2 & !A3) -> EXCLUDE_REPORTED}。</p>
 *
 * <p>语法硬约束（违背即 IllegalArgumentException，消息面向配置者）：</p>
 * <ul>
 *   <li>原子 ∈ {A1,A1A,A1B,A1C,A2,A3,A4}（大小写不敏感）；<b>A5 拒绝</b>并提示其阶段三子表归属；</li>
 *   <li>同一层内 AND/OR 混用须经一层括号分组；括号嵌套上限一层；</li>
 *   <li>NOT 仅作用于单个原子或一个括号组，不可叠加。</li>
 * </ul>
 */
public final class FreeformPolicyParser
{
    private FreeformPolicyParser()
    {
    }

    /** 解析单条规则行（"表达式 -> 动作"）。ruleId 由调用方赋值。 */
    public static FreeformRule parseRule(String line)
    {
        if (line == null || line.isBlank())
        {
            throw new IllegalArgumentException("规则行不可为空");
        }
        String text = line.trim();
        int arrow = text.indexOf("->");
        if (arrow < 0)
        {
            throw new IllegalArgumentException("缺少 \"->\" 处置动作段: " + text);
        }
        if (text.indexOf("->", arrow + 2) >= 0)
        {
            throw new IllegalArgumentException("多个 \"->\"，规则行只能是 表达式->动作: " + text);
        }
        String actionText = text.substring(arrow + 2).trim();
        final Disposition action;
        try
        {
            action = Disposition.valueOf(actionText.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("非法处置动作 \"" + actionText
                    + "\"（合法: COUNT_NORMAL / EXCLUDE_REPORTED / COUNT_UNQUALIFIED / PEND_MARKED）");
        }
        PolicyExpression expr = parseExpression(text.substring(0, arrow));
        return new FreeformRule(null, expr.canonical(), expr, action);
    }

    /** 解析表达式文本（无动作段）。 */
    public static PolicyExpression parseExpression(String text)
    {
        Parser p = new Parser(tokenize(text));
        PolicyExpression e = p.parseLevel(0);
        if (!p.atEnd())
        {
            throw new IllegalArgumentException("表达式存在多余尾随内容: " + p.peekText());
        }
        return e;
    }

    private enum Tok { ATOM, AND, OR, NOT, LP, RP }

    private record Token(Tok type, String text, PolicyAtom atom)
    {
    }

    private static List<Token> tokenize(String text)
    {
        if (text == null || text.isBlank())
        {
            throw new IllegalArgumentException("表达式不可为空");
        }
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < text.length())
        {
            char c = text.charAt(i);
            if (Character.isWhitespace(c))
            {
                i++;
                continue;
            }
            switch (c)
            {
                case '&' -> { out.add(new Token(Tok.AND, "&", null)); i++; }
                case '|' -> { out.add(new Token(Tok.OR, "|", null)); i++; }
                case '!' -> { out.add(new Token(Tok.NOT, "!", null)); i++; }
                case '(' -> { out.add(new Token(Tok.LP, "(", null)); i++; }
                case ')' -> { out.add(new Token(Tok.RP, ")", null)); i++; }
                default ->
                {
                    if (!Character.isLetterOrDigit(c))
                    {
                        throw new IllegalArgumentException("非法字符 \"" + c + "\"（合法: 原子名 与 ! & | ( ) ->）");
                    }
                    int j = i;
                    while (j < text.length() && Character.isLetterOrDigit(text.charAt(j)))
                    {
                        j++;
                    }
                    String word = text.substring(i, j).toUpperCase(Locale.ROOT);
                    if ("A5".equals(word))
                    {
                        throw new IllegalArgumentException("原子 A5（免考旗读取失败）不进入本规则表——"
                                + "属阶段三免考后置子表（策略文档 §3.3.2 对接口径），随 §6.5 拍板另设");
                    }
                    PolicyAtom atom;
                    try
                    {
                        atom = PolicyAtom.valueOf(word);
                    }
                    catch (IllegalArgumentException e)
                    {
                        throw new IllegalArgumentException("未知原子 \"" + word
                                + "\"（合法: A1/A1A/A1B/A1C/A2/A3/A4）");
                    }
                    out.add(new Token(Tok.ATOM, word, atom));
                    i = j;
                }
            }
        }
        if (out.isEmpty())
        {
            throw new IllegalArgumentException("表达式不可为空");
        }
        return out;
    }

    private static final class Parser
    {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens)
        {
            this.tokens = tokens;
        }

        boolean atEnd()
        {
            return pos >= tokens.size();
        }

        String peekText()
        {
            return atEnd() ? "<EOF>" : tokens.get(pos).text();
        }

        private Token peek()
        {
            if (atEnd())
            {
                throw new IllegalArgumentException("表达式意外结束");
            }
            return tokens.get(pos);
        }

        /** 一层内的操作数序列：连接词必须一致，混用即拒（须括号分组）。 */
        PolicyExpression parseLevel(int depth)
        {
            List<PolicyExpression.Node> operands = new ArrayList<>();
            PolicyExpression.Join join = null;
            while (true)
            {
                operands.add(parseOperand(depth));
                if (atEnd() || peek().type() == Tok.RP)
                {
                    break;
                }
                Tok t = peek().type();
                if (t != Tok.AND && t != Tok.OR)
                {
                    throw new IllegalArgumentException("期望连接词 & 或 |，实得 \"" + peekText() + "\"");
                }
                PolicyExpression.Join thisJoin = t == Tok.AND ? PolicyExpression.Join.AND : PolicyExpression.Join.OR;
                if (join == null)
                {
                    join = thisJoin;
                }
                else if (join != thisJoin)
                {
                    throw new IllegalArgumentException("同层 AND/OR 混用须经一层括号分组（如 A1 | (A2 & !A3)）");
                }
                pos++;
            }
            return PolicyExpression.of(join == null ? PolicyExpression.Join.AND : join, operands);
        }

        private PolicyExpression.Node parseOperand(int depth)
        {
            boolean negated = false;
            if (peek().type() == Tok.NOT)
            {
                pos++;
                negated = true;
                if (!atEnd() && peek().type() == Tok.NOT)
                {
                    throw new IllegalArgumentException("NOT 不可叠加（!!）");
                }
            }
            if (peek().type() == Tok.LP)
            {
                pos++;
                Group inner = parseGroupContent();
                if (atEnd() || peek().type() != Tok.RP)
                {
                    throw new IllegalArgumentException("括号未闭合");
                }
                pos++;
                return new PolicyExpression.GroupTerm(inner.join, inner.atoms(), negated);
            }
            if (peek().type() != Tok.ATOM)
            {
                throw new IllegalArgumentException("期望原子或 \"(\"，实得 \"" + peekText() + "\"");
            }
            PolicyAtom atom = peek().atom();
            pos++;
            return new PolicyExpression.AtomTerm(atom, negated);
        }

        /** 括号内：平坦的 取反?原子 序列 + 单一连接词——任何内层 "(" 即超一层嵌套上限。 */
        private Group parseGroupContent()
        {
            List<PolicyExpression.AtomTerm> atoms = new ArrayList<>();
            PolicyExpression.Join join = null;
            while (true)
            {
                boolean neg = false;
                if (peek().type() == Tok.NOT)
                {
                    pos++;
                    neg = true;
                    if (peek().type() == Tok.NOT)
                    {
                        throw new IllegalArgumentException("NOT 不可叠加（!!）");
                    }
                }
                if (peek().type() == Tok.LP)
                {
                    throw new IllegalArgumentException("括号嵌套超过一层上限（审计可读性约束，§3.3.2）");
                }
                if (peek().type() != Tok.ATOM)
                {
                    throw new IllegalArgumentException("括号内期望原子，实得 \"" + peekText() + "\"");
                }
                atoms.add(new PolicyExpression.AtomTerm(peek().atom(), neg));
                pos++;
                if (atEnd() || peek().type() == Tok.RP)
                {
                    break;
                }
                Tok t = peek().type();
                if (t != Tok.AND && t != Tok.OR)
                {
                    throw new IllegalArgumentException("括号内期望连接词 & 或 |，实得 \"" + peekText() + "\"");
                }
                PolicyExpression.Join thisJoin = t == Tok.AND ? PolicyExpression.Join.AND : PolicyExpression.Join.OR;
                if (join == null)
                {
                    join = thisJoin;
                }
                else if (join != thisJoin)
                {
                    throw new IllegalArgumentException("括号内 AND/OR 混用须拆分——嵌套上限一层");
                }
                pos++;
            }
            if (atoms.isEmpty())
            {
                throw new IllegalArgumentException("括号组不可为空");
            }
            return new Group(join == null ? PolicyExpression.Join.AND : join, atoms);
        }
    }

    private record Group(PolicyExpression.Join join, List<PolicyExpression.AtomTerm> atoms)
    {
    }
}
