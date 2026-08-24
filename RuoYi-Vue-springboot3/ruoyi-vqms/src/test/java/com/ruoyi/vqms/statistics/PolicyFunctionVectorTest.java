package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * D9 L0：数据不可用处置纯函数——甲/乙/丙/丁四套候选 = 同一函数的四组配置向量
 * （测试方案 §4.8：候选不是四套代码；分母口径拍板后剔除类候选的相容性待政策复核，
 * 此处仅作<b>测试向量</b>，函数行为断言不受影响）。
 *
 * <p>向量只存在于测试侧（v5.0 §8.6「甲/乙/丙/丁不硬编码进任何代码」——main 侧无预设配置）。</p>
 */
class PolicyFunctionVectorTest
{
    private static RegulationOutcome.Judged judged(double completeness, Set<Tier> invalid)
    {
        Optional<Verdict> fast = invalid.contains(Tier.FAST) ? Optional.empty() : Optional.of(Verdict.QUALIFIED);
        Optional<Verdict> econ = invalid.contains(Tier.ECON) ? Optional.empty() : Optional.of(Verdict.PENALIZED);
        return new RegulationOutcome.Judged(fast, econ, completeness, invalid);
    }

    private static final RegulationOutcome UNDECODABLE = new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING);
    private static final RegulationOutcome FULL = judged(1.0, Set.of());
    private static final RegulationOutcome PARTIAL_80 = judged(0.8, Set.of());
    private static final RegulationOutcome PARTIAL_40 = judged(0.4, Set.of());
    private static final RegulationOutcome TIER_INVALID = judged(1.0, Set.of(Tier.FAST));
    private static final RegulationOutcome ZERO_BOTH_INVALID = judged(0.0, Set.of(Tier.FAST, Tier.ECON));

    /** 四套候选配置向量（策略文档 §3.2 / §4.2）——S5 起改引 main 侧 {@link PolicyPreset} 消重
     *  （§8.7：预设映射唯一权威 = main 侧枚举，测试只引用不复制）。 */
    private enum Candidate
    {
        JIA,
        YI_50,
        BING,
        DING;

        PolicyConfig config()
        {
            return switch (this)
            {
                case JIA -> PolicyPreset.JIA.config();
                case YI_50 -> PolicyPreset.YI.config();
                case BING -> PolicyPreset.BING.config();
                case DING -> PolicyPreset.DING.config();
            };
        }
    }

    private void assertBucket(Candidate c, RegulationOutcome in, Disposition expected)
    {
        Assertions.assertEquals(expected, DataUnavailabilityPolicy.evaluate(in, c.config()),
                c + " 对 " + in + " 的处置");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @EnumSource(Candidate.class)
    void undecodable_followsUndecodableMode(Candidate c)
    {
        Disposition expected = switch (c)
        {
            case JIA, YI_50 -> Disposition.EXCLUDE_REPORTED;
            case BING -> Disposition.COUNT_UNQUALIFIED;
            case DING -> Disposition.PEND_MARKED;
        };
        for (DecodeFailureReason reason : DecodeFailureReason.values())
        {
            assertBucket(c, new RegulationOutcome.Undecodable(reason), expected);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @EnumSource(Candidate.class)
    void fullData_normalCounting_everyCandidate(Candidate c)
    {
        assertBucket(c, FULL, Disposition.COUNT_NORMAL);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @EnumSource(Candidate.class)
    void tierInvalid_followsInvalidTierMode(Candidate c)
    {
        // S16（L>H）/S14（整档全缺）同族：invalidTiers 非空 → invalidTierMode
        Disposition expected = switch (c)
        {
            case JIA, YI_50 -> Disposition.EXCLUDE_REPORTED;
            case BING -> Disposition.COUNT_UNQUALIFIED;
            case DING -> Disposition.PEND_MARKED;
        };
        assertBucket(c, TIER_INVALID, expected);
        assertBucket(c, ZERO_BOTH_INVALID, expected);
    }

    @Test
    void partialMissing_jia_usesRemaining_normal()
    {
        // 甲：部分缺跳过用剩余（判定已按剩余分钟判）→ 正常记账
        assertBucket(Candidate.JIA, PARTIAL_80, Disposition.COUNT_NORMAL);
    }

    @Test
    void partialMissing_yi_thresholdExclude()
    {
        // 乙：50% 阈值——80% 可用 → 正常；40% 可用 → 剔除+计数
        assertBucket(Candidate.YI_50, PARTIAL_80, Disposition.COUNT_NORMAL);
        assertBucket(Candidate.YI_50, PARTIAL_40, Disposition.EXCLUDE_REPORTED);
        // ⚠ UNVERIFIED-口径（对抗验证吸收 2026-08-22）：阈值边界语义两份权威文档相反——
        // 正式 v1_0 §2.8 / v5.0 §8.6 写缺失率「≥50% 整窗剔除」（⇔ 可用度恰 50% 应剔除），
        // 策略文档 §3.2 写「可用 ≥50% 用剩余」。实现取后者（可用 ≥ 阈值 → 正常记账）；
        // 选套拍板时须一并钉死边界方向，届时本用例随拍板复核。
        Assertions.assertEquals(Disposition.COUNT_NORMAL,
                DataUnavailabilityPolicy.evaluate(judged(0.5, Set.of()), new PolicyConfig(
                        Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
                        Disposition.EXCLUDE_REPORTED, 50)));
    }

    @Test
    void partialMissing_bing_countUnqualified()
    {
        // 丙：有缺失即计不合格
        assertBucket(Candidate.BING, PARTIAL_80, Disposition.COUNT_UNQUALIFIED);
    }

    @Test
    void partialMissing_ding_pendMarked()
    {
        assertBucket(Candidate.DING, PARTIAL_80, Disposition.PEND_MARKED);
    }

    @Test
    void config_validation_excludeRequiresThreshold()
    {
        // 记录级契约：EXCLUDE 无阈值 → NPE（record 不变式）；service 层翻译为 ISE（见 VqmsPolicyParamService）
        Assertions.assertThrows(NullPointerException.class,
                () -> new PolicyConfig(Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
                        Disposition.EXCLUDE_REPORTED, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PolicyConfig(Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
                        Disposition.EXCLUDE_REPORTED, 101));
        // 非 EXCLUDE 的 partial 模式不需要阈值
        Assertions.assertDoesNotThrow(() -> new PolicyConfig(Disposition.EXCLUDE_REPORTED,
                Disposition.EXCLUDE_REPORTED, Disposition.COUNT_NORMAL, null));
    }

    @Test
    void determinism_sameInputSameConfig_sameBucket()
    {
        for (Candidate c : Candidate.values())
        {
            for (RegulationOutcome in : java.util.List.of(UNDECODABLE, FULL, PARTIAL_80, PARTIAL_40, TIER_INVALID, ZERO_BOTH_INVALID))
            {
                Assertions.assertEquals(DataUnavailabilityPolicy.evaluate(in, c.config()),
                        DataUnavailabilityPolicy.evaluate(in, c.config()), c + " × " + in);
            }
        }
    }
}
