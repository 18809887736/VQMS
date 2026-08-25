package com.ruoyi.vqms.statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 戊·自由组合 L0：表达式解析器（策略文档 §3.3.2 语法硬约束逐条）。
 */
class FreeformPolicyParserTest
{
    private static PolicyExpression parse(String text)
    {
        return FreeformPolicyParser.parseExpression(text);
    }

    @Test
    void simpleAtom_andCanonicalNormalization()
    {
        PolicyExpression e = parse("a1");
        assertEquals("A1", e.canonical());
        assertTrue(e.eval(new RegulationOutcome.Undecodable(DecodeFailureReason.CYCLE_CODE_INVALID), 50));
    }

    @Test
    void caseInsensitive_whitespaceInsensitive()
    {
        assertEquals(parse("A1&A2").canonical(), parse(" a1 & a2 ").canonical());
    }

    @Test
    void mixedConnectors_requireParenGrouping()
    {
        assertThrows(IllegalArgumentException.class, () -> parse("A1 & A2 | A3"),
                "顶层 AND/OR 混用须括号分组");
        // 分组后合法，结合律由括号显式表达
        PolicyExpression e = parse("A1 | (A2 & A3)");
        assertEquals(PolicyExpression.Join.OR, e.join());
    }

    @Test
    void nestingBeyondOneLevel_rejected()
    {
        assertThrows(IllegalArgumentException.class, () -> parse("(A1 & (A2 | A3))"),
                "嵌套超一层上限");
        assertThrows(IllegalArgumentException.class, () -> parse("(A1) & (A2 | (A3))"));
    }

    @Test
    void notOnAtomAndGroup_doubleNotRejected()
    {
        assertTrue(parse("!A1").operands().get(0) instanceof PolicyExpression.AtomTerm t && t.negated());
        PolicyExpression g = parse("!(A1 | A2)");
        assertTrue(g.operands().get(0) instanceof PolicyExpression.GroupTerm gt && gt.negated());
        assertThrows(IllegalArgumentException.class, () -> parse("!!A1"));
    }

    @Test
    void unknownAtom_andReservedA5_rejectedWithGuidance()
    {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> parse("A9 & A1"));
        assertTrue(unknown.getMessage().contains("A9"));

        IllegalArgumentException a5 = assertThrows(IllegalArgumentException.class, () -> parse("A5"));
        assertTrue(a5.getMessage().contains("阶段三"), "A5 应提示其阶段三子表归属");
    }

    @Test
    void structuralErrors_rejected()
    {
        assertThrows(IllegalArgumentException.class, () -> parse(""));
        assertThrows(IllegalArgumentException.class, () -> parse("()"));
        assertThrows(IllegalArgumentException.class, () -> parse("(A1"));
        assertThrows(IllegalArgumentException.class, () -> parse("A1 )"));
        assertThrows(IllegalArgumentException.class, () -> parse("A1 &"));
        assertThrows(IllegalArgumentException.class, () -> parse("A1 @ A2"));
        assertThrows(IllegalArgumentException.class, () -> parse("(A1 & )"));
    }

    @Test
    void referencedAtoms_collectedFromGroups()
    {
        assertEquals(3, parse("A1 | (A2 & !A3)").referencedAtoms().size());
        assertTrue(parse("!(A4 & A2)").referencedAtoms().contains(PolicyAtom.A4));
    }

    @Test
    void ruleLine_parsesToRuleWithAction()
    {
        FreeformRule r = FreeformPolicyParser.parseRule("a1 | (A2 & !a3) -> exclude_reported");
        assertEquals("A1 | (A2 & !A3)", r.expressionText());
        assertEquals(Disposition.EXCLUDE_REPORTED, r.action());
    }

    @Test
    void ruleLine_actionErrors()
    {
        assertThrows(IllegalArgumentException.class,
                () -> FreeformPolicyParser.parseRule("A1 -> WHATEVER"));
        assertThrows(IllegalArgumentException.class, () -> FreeformPolicyParser.parseRule("A1"));
        assertThrows(IllegalArgumentException.class,
                () -> FreeformPolicyParser.parseRule("A1 -> PEND_MARKED -> COUNT_NORMAL"));
    }
}
