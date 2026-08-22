package com.ruoyi.vqms.management.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.statistics.Disposition;
import com.ruoyi.vqms.statistics.PolicyConfig;

/**
 * 数据不可用策略参数 Service（D9 骨架，v5.0 §8.7）。
 *
 * <p>无 CRUD UI、无缓存（搁置期无人改、无热路径——与 D7 judge_param 的差别是有意的）；
 * 唯一职责 = 把表行装配成 {@link PolicyConfig} 供策略评估纯函数消费。
 * <b>选套值留空</b>（Leo 2026-08-18 拍板）：表空/缺键 → 返回 {@link Optional#empty()}
 * （策略未定稿态），调用方不得自行猜测默认处置。</p>
 *
 * <p>约定键：undecodable_mode / invalid_tier_mode / partial_missing_mode ∈
 * {@link Disposition} 枚举名；partial_missing_threshold_pct = 整数百分比
 * （partial_missing_mode = EXCLUDE_REPORTED 时必填）。</p>
 */
@Service
public class VqmsPolicyParamService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsPolicyParamService.class);

    static final String KEY_UNDECODABLE_MODE = "undecodable_mode";
    static final String KEY_INVALID_TIER_MODE = "invalid_tier_mode";
    static final String KEY_PARTIAL_MISSING_MODE = "partial_missing_mode";
    static final String KEY_PARTIAL_MISSING_THRESHOLD = "partial_missing_threshold_pct";

    @Autowired
    private VqmsPolicyParamMapper policyParamMapper;

    public List<VqmsPolicyParam> selectList()
    {
        return policyParamMapper.selectList();
    }

    /**
     * 装配策略配置。
     *
     * @return 空表或任一约定键缺失 → empty（策略未定稿态，不猜默认值）
     * @throws IllegalStateException 行存在但值非法（枚举名拼错/阈值非整数）——配置错误显性失败
     */
    public Optional<PolicyConfig> loadConfig()
    {
        List<VqmsPolicyParam> rows = policyParamMapper.selectList();
        if (rows.isEmpty())
        {
            return Optional.empty();
        }
        java.util.Map<String, String> byKey = new java.util.HashMap<>();
        for (VqmsPolicyParam row : rows)
        {
            byKey.put(row.getParamKey(), row.getParamValue());
        }
        String undecodable = byKey.get(KEY_UNDECODABLE_MODE);
        String invalidTier = byKey.get(KEY_INVALID_TIER_MODE);
        String partial = byKey.get(KEY_PARTIAL_MISSING_MODE);
        if (undecodable == null || invalidTier == null || partial == null)
        {
            log.warn("策略参数不完整（缺约定键），视为未定稿: 现有键={}", byKey.keySet());
            return Optional.empty();
        }
        Integer threshold = null;
        String thresholdText = byKey.get(KEY_PARTIAL_MISSING_THRESHOLD);
        Disposition partialMode;
        try
        {
            partialMode = Disposition.valueOf(partial.trim());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("策略参数含非法处置枚举值: " + undecodable + "/" + invalidTier + "/" + partial);
        }
        if (thresholdText != null)
        {
            try
            {
                threshold = Integer.valueOf(thresholdText.trim());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalStateException("策略参数 " + KEY_PARTIAL_MISSING_THRESHOLD + " 非整数: " + thresholdText);
            }
        }
        else if (partialMode == Disposition.EXCLUDE_REPORTED)
        {
            // 显性失败带键名上下文（PolicyConfig 构造器抛的是 NPE，此处先拦并翻译成契约声明的 ISE）
            throw new IllegalStateException("策略参数缺 " + KEY_PARTIAL_MISSING_THRESHOLD
                    + "（partial_missing_mode=EXCLUDE_REPORTED 时必填）");
        }
        try
        {
            return Optional.of(new PolicyConfig(Disposition.valueOf(undecodable.trim()),
                    Disposition.valueOf(invalidTier.trim()),
                    partialMode,
                    threshold));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("策略参数含非法处置枚举值: " + undecodable + "/" + invalidTier + "/" + partial);
        }
    }
}
