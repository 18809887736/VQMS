package com.ruoyi.vqms.statistics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 戊·自由组合 L0：应用校验 fail-fast 矩阵（策略文档 §3.3.2 校验清单）。
 */
class FreeformPolicyValidatorTest
{
    @Test
    void emptyRules_rejected()
    {
        assertTrue(FreeformPolicyValidator.validate(List.of(), 50).hasErrors());
        assertTrue(FreeformPolicyValidator.validate(null, 50).hasErrors());
    }

    @Test
    void ruleCountCap_enforced()
    {
        String[] lines = new String[FreeformPolicyConfig.MAX_RULES + 1];
        for (int i = 0; i < lines.length; i++)
        {
            lines[i] = "A1 -> PEND_MARKED"; // 同表达式重复另计错——此处仅验上限
        }
        FreeformPolicyValidator.Validation v = FreeformPolicyValidator.validate(List.of(lines), 50);
        assertTrue(v.errors().stream().anyMatch(e -> e.contains("超上限")));
    }

    @Test
    void perLineErrors_carryLineNumber()
    {
        FreeformPolicyValidator.Validation v = FreeformPolicyValidator.validate(
                List.of("A1 -> EXCLUDE_REPORTED", "A9 -> PEND_MARKED"), 50);
        assertTrue(v.hasErrors());
        assertEquals(1, v.errors().size(), "合法行不产生错误");
        assertTrue(v.errors().get(0).startsWith("第 2 行"), "错误带行号定位");
        assertTrue(v.errors().get(0).contains("未知原子"));
    }

    @Test
    void duplicateExpression_rejected()
    {
        FreeformPolicyValidator.Validation v = FreeformPolicyValidator.validate(
                List.of("A1 -> PEND_MARKED", "a1 -> COUNT_NORMAL"), 50);
        assertTrue(v.errors().stream().anyMatch(e -> e.contains("完全相同")),
                "大小写规范化后重复仍拒");
    }

    @Test
    void countNormal_requiresA3InTrigger()
    {
        FreeformPolicyValidator.Validation bad = FreeformPolicyValidator.validate(
                List.of("A1 -> COUNT_NORMAL"), 50);
        assertTrue(bad.errors().stream().anyMatch(e -> e.contains("仅当触发含 A3")));

        assertFalse(FreeformPolicyValidator.validate(
                List.of("A3 & !A4 -> COUNT_NORMAL"), 50).hasErrors(),
                "触发含 A3 的用剩余动作合法");
    }

    @Test
    void a4Reference_declaredForThresholdDependency()
    {
        // τ 恒有值（默认 50），A4 引用只声明依赖——合法路径
        assertFalse(FreeformPolicyValidator.validate(
                List.of("A3 & A4 -> EXCLUDE_REPORTED"), 50).hasErrors());
        // τ 越界在 validate 入口即拦
        assertTrue(FreeformPolicyValidator.validate(
                List.of("A3 & A4 -> EXCLUDE_REPORTED"), 150).errors()
                .stream().anyMatch(e -> e.contains("[0,100]")));
    }

    @Test
    void storedValue_lengthGuard()
    {
        // 合法但超长的表达式：60 个操作数 & 连接，规范化后必超 varchar(255)
        StringBuilder expr = new StringBuilder();
        for (int i = 0; i < 60; i++)
        {
            expr.append(i == 0 ? "" : " & ").append(i % 2 == 0 ? "A1" : "!A2");
        }
        FreeformPolicyValidator.Validation v = FreeformPolicyValidator.validate(
                List.of(expr.toString() + " -> EXCLUDE_REPORTED"), 50);
        assertTrue(v.errors().stream().anyMatch(e -> e.contains("存储上限")),
                "规范化后超 varchar(255) 即拒");
    }

    @Test
    void validTable_assignsRuleIdsByOrder()
    {
        FreeformPolicyValidator.Validation v = FreeformPolicyValidator.validate(
                List.of("A1B -> PEND_MARKED", "A1 -> EXCLUDE_REPORTED",
                        "(A2 & !A3) -> PEND_MARKED", "A3 & A4 -> EXCLUDE_REPORTED"), 50);
        assertFalse(v.hasErrors());
        assertEquals("R001", v.rules().get(0).ruleId());
        assertEquals("R004", v.rules().get(3).ruleId());
    }

    @Test
    void okAndHasErrors_areConsistent()
    {
        FreeformPolicyValidator.Validation ok = FreeformPolicyValidator.validate(
                List.of("A1 -> PEND_MARKED"), 50);
        assertTrue(ok.ok());
        assertFalse(ok.hasErrors());
    }
}
