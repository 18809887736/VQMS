package com.ruoyi.vqms.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据不可用策略预设（v5.0 §8.7「策略参数页」，2026-08-22 Leo 拍板设计）——
 * 甲/乙/丙/丁四组<b>命名参数向量</b>，零行为分支：行为仍由
 * {@link DataUnavailabilityPolicy#evaluate} 按 {@link PolicyConfig} 纯函数决定。
 *
 * <p>本枚举是预设映射的<b>唯一权威</b>（前端只渲染标签不存映射，防前后端两份漂移；
 * 测试侧 {@code PolicyFunctionVectorTest} 改引本枚举消重）。此为 §8.6 原文
 * 「甲/乙/丙/丁不硬编码进任何代码」的字面修订——禁的是<b>行为</b>硬编码，
 * 命名参数模板不违初衷（2026-08-22 拍板）。</p>
 *
 * <p><b>选套值维持留空</b>：本枚举只是 UI 选择器的候选模板，写入
 * {@code vqms_policy_param} 须经策略参数页 apply 动作（权限默认仅授管理员）；
 * 选套本身属政策拍板动作（S5），拍板前表保持空、策略未生效。</p>
 */
public enum PolicyPreset
{
    /** 甲（宽松跳过）：部分缺用剩余正常记账；判不了/档无效剔除分母 + 上报计数 */
    JIA("甲", "宽松跳过", Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
            Disposition.COUNT_NORMAL, null),

    /** 乙（推荐·稳健）：部分缺按可用度阈值剔除 + 计数（阈值可整定，默认 50%）；其余同甲 */
    YI("乙", "阈值剔除+计数（推荐）", Disposition.EXCLUDE_REPORTED, Disposition.EXCLUDE_REPORTED,
            Disposition.EXCLUDE_REPORTED, 50),

    /** 丙（严）：有缺失即计不合格 */
    BING("丙", "计不合格", Disposition.COUNT_UNQUALIFIED, Disposition.COUNT_UNQUALIFIED,
            Disposition.COUNT_UNQUALIFIED, null),

    /** 丁（透明）：跳过 + 标记挂起人工后审 */
    DING("丁", "标记挂起", Disposition.PEND_MARKED, Disposition.PEND_MARKED,
            Disposition.PEND_MARKED, null);

    /** vqms_policy_param 约定键（D9 建表注记口径） */
    public static final String KEY_UNDECODABLE_MODE = "undecodable_mode";
    public static final String KEY_INVALID_TIER_MODE = "invalid_tier_mode";
    public static final String KEY_PARTIAL_MISSING_MODE = "partial_missing_mode";
    public static final String KEY_PARTIAL_MISSING_THRESHOLD_PCT = "partial_missing_threshold_pct";

    private final String label;
    private final String description;
    private final Disposition undecodableMode;
    private final Disposition invalidTierMode;
    private final Disposition partialMissingMode;
    private final Integer defaultThresholdPct;

    PolicyPreset(String label, String description,
            Disposition undecodableMode, Disposition invalidTierMode,
            Disposition partialMissingMode, Integer defaultThresholdPct)
    {
        this.label = label;
        this.description = description;
        this.undecodableMode = undecodableMode;
        this.invalidTierMode = invalidTierMode;
        this.partialMissingMode = partialMissingMode;
        this.defaultThresholdPct = defaultThresholdPct;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public Disposition getUndecodableMode()
    {
        return undecodableMode;
    }

    public Disposition getInvalidTierMode()
    {
        return invalidTierMode;
    }

    public Disposition getPartialMissingMode()
    {
        return partialMissingMode;
    }

    public Integer getDefaultThresholdPct()
    {
        return defaultThresholdPct;
    }

    /**
     * 预设 → 四约定键参数向量（供策略参数页 apply 整组 upsert）。
     *
     * @param thresholdOverride 乙档阈值覆盖；null 用预设默认（仅 YI 有默认 50）
     * @throws IllegalArgumentException 覆盖值越界或对无阈值预设提供覆盖
     */
    public Map<String, String> params(Integer thresholdOverride)
    {
        Integer threshold = thresholdOverride != null ? thresholdOverride : defaultThresholdPct;
        if (partialMissingMode == Disposition.EXCLUDE_REPORTED)
        {
            if (threshold == null)
            {
                throw new IllegalArgumentException(name() + " 部分缺为阈值剔除但未提供阈值");
            }
            if (threshold < 0 || threshold > 100)
            {
                throw new IllegalArgumentException(name() + " 阈值须 ∈ [0,100]: " + threshold);
            }
        }
        else if (thresholdOverride != null)
        {
            throw new IllegalArgumentException(name() + " 无阈值键，不接受覆盖: " + thresholdOverride);
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put(KEY_UNDECODABLE_MODE, undecodableMode.name());
        params.put(KEY_INVALID_TIER_MODE, invalidTierMode.name());
        params.put(KEY_PARTIAL_MISSING_MODE, partialMissingMode.name());
        params.put(KEY_PARTIAL_MISSING_THRESHOLD_PCT, threshold == null ? null : String.valueOf(threshold));
        return params;
    }

    /** 预设对应 {@link PolicyConfig}（测试侧消重与后端校验共用）。 */
    public PolicyConfig config()
    {
        Integer threshold = partialMissingMode == Disposition.EXCLUDE_REPORTED ? defaultThresholdPct : null;
        return new PolicyConfig(undecodableMode, invalidTierMode, partialMissingMode, threshold);
    }
}
