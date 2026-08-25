package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 戊·自由组合 L0：等价规约器（策略文档 §3.3.4 落地件）——
 * 「四预设皆退化组合」的机器可检面 + 保守性（拿不准不提示）。
 */
class FreeformPolicyReducerTest
{
    private static Optional<FreeformPolicyReducer.Reduction> reduce(String... lines)
    {
        return FreeformPolicyReducer.reduce(
                new FreeformPolicyConfig(
                        List.of(lines).stream().map(FreeformPolicyParser::parseRule).toList(), 50));
    }

    @Test
    void yiEquivalent_detectedWithPresetName()
    {
        // ≡ 乙(τ=50)：A1/A2 剔除 + 阈值剔除
        Optional<FreeformPolicyReducer.Reduction> r = reduce(
                "A1 -> EXCLUDE_REPORTED",
                "A2 -> EXCLUDE_REPORTED",
                "A3 & A4 -> EXCLUDE_REPORTED");
        assertTrue(r.isPresent());
        assertEquals("YI", r.get().presetCode());
        assertEquals(PolicyPreset.YI.config(), r.get().config());
    }

    @Test
    void jiaBingDing_equivalents_detected()
    {
        assertTrue(reduce("A1 -> EXCLUDE_REPORTED", "A2 -> EXCLUDE_REPORTED", "A3 -> COUNT_NORMAL")
                .map(FreeformPolicyReducer.Reduction::presetCode).orElse("").equals("JIA"));
        assertTrue(reduce("A1 -> COUNT_UNQUALIFIED", "A2 -> COUNT_UNQUALIFIED", "A3 -> COUNT_UNQUALIFIED")
                .map(FreeformPolicyReducer.Reduction::presetCode).orElse("").equals("BING"));
        assertTrue(reduce("A1 -> PEND_MARKED", "A2 -> PEND_MARKED", "A3 -> PEND_MARKED")
                .map(FreeformPolicyReducer.Reduction::presetCode).orElse("").equals("DING"));
    }

    @Test
    void redundantAboveThresholdComplement_stillYi()
    {
        // {A3&A4->剔除, A3&!A4->正常记账} 与省略后者的兜底形式等价
        Optional<FreeformPolicyReducer.Reduction> full = reduce(
                "A1 -> EXCLUDE_REPORTED", "A2 -> EXCLUDE_REPORTED",
                "A3 & A4 -> EXCLUDE_REPORTED", "A3 & !A4 -> COUNT_NORMAL");
        assertTrue(full.isPresent());
        assertEquals("YI", full.get().presetCode());
    }

    @Test
    void customThreshold_reducesToConfigWithoutPresetName()
    {
        FreeformPolicyConfig cfg = new FreeformPolicyConfig(List.of(
                FreeformPolicyParser.parseRule("A1 -> EXCLUDE_REPORTED"),
                FreeformPolicyParser.parseRule("A2 -> EXCLUDE_REPORTED"),
                FreeformPolicyParser.parseRule("A3 & A4 -> EXCLUDE_REPORTED")), 40);
        Optional<FreeformPolicyReducer.Reduction> r = FreeformPolicyReducer.reduce(cfg);
        assertTrue(r.isPresent());
        assertNull(r.get().presetCode(), "τ=40 ≠ 预设默认 50 → 无命名预设对应");
        assertEquals(new PolicyConfig(Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
                Disposition.EXCLUDE_REPORTED, 40), r.get().config());
    }

    @Test
    void decodeSubtypeSplit_notReducible()
    {
        // 子类拆分正是扩展求值器的存在意义——规约器必须拒绝
        assertFalse(reduce("A1B -> COUNT_NORMAL", "A1 -> EXCLUDE_REPORTED").isPresent());
    }

    @Test
    void crossAxisExpression_notReducible()
    {
        assertFalse(reduce("(A2 & !A3) -> PEND_MARKED").isPresent());
    }

    @Test
    void axisOrderMismatch_notReducible()
    {
        // PARTIAL 规则排在 INVALID 前——与固定优先链不同构，保守拒绝
        assertFalse(reduce("A3 -> COUNT_UNQUALIFIED", "A2 -> PEND_MARKED").isPresent());
    }

    @Test
    void a3ExcludeWithoutThreshold_notReducible()
    {
        // 「有缺即剔除」≠ PolicyConfig 任何模式（EXCLUDE 必经阈值门）
        assertFalse(reduce("A1 -> EXCLUDE_REPORTED", "A2 -> EXCLUDE_REPORTED", "A3 -> EXCLUDE_REPORTED")
                .isPresent());
    }
}
