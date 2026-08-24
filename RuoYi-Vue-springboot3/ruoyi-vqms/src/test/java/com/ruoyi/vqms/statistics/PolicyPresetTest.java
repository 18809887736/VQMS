package com.ruoyi.vqms.statistics;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 L0：PolicyPreset 预设向量（策略文档 §3.2/§4.2 矩阵；main 侧唯一权威，2026-08-22 拍板）。
 *
 * <p>零行为分支——本枚举只验「命名参数向量与政策候选矩阵一致」+ 四约定键出参正确。</p>
 */
class PolicyPresetTest
{
    @Test
    void fourPresets_matchCandidateMatrix()
    {
        // 甲（宽松跳过）：部分缺用剩余正常记账；判不了/档无效剔除+上报
        assertEquals(Disposition.EXCLUDE_REPORTED, PolicyPreset.JIA.getUndecodableMode());
        assertEquals(Disposition.EXCLUDE_REPORTED, PolicyPreset.JIA.getInvalidTierMode());
        assertEquals(Disposition.COUNT_NORMAL, PolicyPreset.JIA.getPartialMissingMode());
        assertNull(PolicyPreset.JIA.getDefaultThresholdPct());

        // 乙（推荐·稳健）：部分缺阈值剔除（默认 50）+ 计数；其余同甲
        assertEquals(Disposition.EXCLUDE_REPORTED, PolicyPreset.YI.getUndecodableMode());
        assertEquals(Disposition.EXCLUDE_REPORTED, PolicyPreset.YI.getInvalidTierMode());
        assertEquals(Disposition.EXCLUDE_REPORTED, PolicyPreset.YI.getPartialMissingMode());
        assertEquals(50, PolicyPreset.YI.getDefaultThresholdPct());

        // 丙（严）：有缺失即计不合格
        assertEquals(Disposition.COUNT_UNQUALIFIED, PolicyPreset.BING.getUndecodableMode());
        assertEquals(Disposition.COUNT_UNQUALIFIED, PolicyPreset.BING.getInvalidTierMode());
        assertEquals(Disposition.COUNT_UNQUALIFIED, PolicyPreset.BING.getPartialMissingMode());

        // 丁（透明）：挂起标记
        assertEquals(Disposition.PEND_MARKED, PolicyPreset.DING.getUndecodableMode());
        assertEquals(Disposition.PEND_MARKED, PolicyPreset.DING.getInvalidTierMode());
        assertEquals(Disposition.PEND_MARKED, PolicyPreset.DING.getPartialMissingMode());
    }

    @Test
    void params_emitsFourConventionKeys()
    {
        Map<String, String> yi = PolicyPreset.YI.params(null);
        assertEquals(4, yi.size());
        assertEquals("EXCLUDE_REPORTED", yi.get(PolicyPreset.KEY_UNDECODABLE_MODE));
        assertEquals("EXCLUDE_REPORTED", yi.get(PolicyPreset.KEY_INVALID_TIER_MODE));
        assertEquals("EXCLUDE_REPORTED", yi.get(PolicyPreset.KEY_PARTIAL_MISSING_MODE));
        assertEquals("50", yi.get(PolicyPreset.KEY_PARTIAL_MISSING_THRESHOLD_PCT));

        // 无阈值预设：阈值键为 null 值（表行 param_value 可空）
        Map<String, String> jia = PolicyPreset.JIA.params(null);
        assertTrue(jia.containsKey(PolicyPreset.KEY_PARTIAL_MISSING_THRESHOLD_PCT));
        assertNull(jia.get(PolicyPreset.KEY_PARTIAL_MISSING_THRESHOLD_PCT));
    }

    @Test
    void thresholdOverride_appliesToYi_only()
    {
        assertEquals("40", PolicyPreset.YI.params(40)
                .get(PolicyPreset.KEY_PARTIAL_MISSING_THRESHOLD_PCT));
        // 越界拒绝
        assertThrows(IllegalArgumentException.class, () -> PolicyPreset.YI.params(-1));
        assertThrows(IllegalArgumentException.class, () -> PolicyPreset.YI.params(101));
        // 对无阈值预设提供覆盖即拒（防脏组合）
        assertThrows(IllegalArgumentException.class, () -> PolicyPreset.JIA.params(50));
    }

    @Test
    void config_matchesVectors_pureFunctionConsumable()
    {
        // 预设 → PolicyConfig 与纯函数签名对接（零行为分支的验证面）
        for (PolicyPreset p : PolicyPreset.values())
        {
            assertNotNull(p.config(), p.name() + " 应可构造 PolicyConfig");
            assertEquals(p.getUndecodableMode(), p.config().undecodableMode());
        }
    }
}
