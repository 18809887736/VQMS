package com.ruoyi.vqms.management.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.statistics.Disposition;
import com.ruoyi.vqms.statistics.PolicyConfig;
import com.ruoyi.vqms.statistics.PolicyPreset;

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

    @Autowired
    private RedisCache redisCache;

    /** 策略缓存键前缀（§8.7 策略参数页 apply 规格：per-key 写穿 + 24h TTL，D7 同款） */
    static final String CACHE_PREFIX = "vqms:policyParam:";

    public List<VqmsPolicyParam> selectList()
    {
        return policyParamMapper.selectList();
    }

    /**
     * 选套应用（策略参数页唯一写路径，§8.7）：预设整组 upsert 四约定键 + 写穿刷新缓存 +
     * 换套留痕日志。不开放逐行 add/edit/remove——杜绝绕过预设写出脏组合。
     *
     * @param presetCode        预设代码（PolicyPreset 枚举名：JIA/YI/BING/DING）
     * @param thresholdOverride 乙档阈值覆盖（null=用预设默认 50）；对无阈值预设提供覆盖即拒
     * @param updateBy          操作人（审计；@Log 另有操作日志）
     * @return 应用的预设代码
     */
    public String applyPreset(String presetCode, Integer thresholdOverride, String updateBy)
    {
        PolicyPreset preset;
        try
        {
            preset = PolicyPreset.valueOf(presetCode);
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException("未知策略套别: " + presetCode + "（合法: JIA/YI/BING/DING）");
        }
        Map<String, String> params = preset.params(thresholdOverride);
        String oldState = describeCurrentSelection();

        for (Map.Entry<String, String> entry : params.entrySet())
        {
            VqmsPolicyParam existing = policyParamMapper.selectByKey(entry.getKey());
            if (existing == null)
            {
                VqmsPolicyParam row = new VqmsPolicyParam();
                row.setParamKey(entry.getKey());
                row.setParamValue(entry.getValue());
                row.setName("数据不可用处置·" + entry.getKey());
                row.setDescription(preset.getLabel() + "（" + preset.getDescription() + "）选套写入 "
                        + java.time.LocalDate.now());
                row.setCreateBy(updateBy);
                policyParamMapper.insert(row);
            }
            else
            {
                policyParamMapper.updateValue(entry.getKey(), entry.getValue(), updateBy);
            }
            // 写穿（§8.7 规格）：per-key + 24h TTL，D7 judge_param 实现同款
            redisCache.setCacheObject(CACHE_PREFIX + entry.getKey(), entry.getValue(),
                    24, TimeUnit.HOURS);
        }
        log.info("VQMS 策略换套: {} → {}（{}），操作人 {}", oldState,
                preset.name(), preset.getLabel() + "·" + preset.getDescription(), updateBy);
        return preset.name();
    }

    /**
     * 当前选套状态（页面三态，§8.7）：未选套 / 已选套。
     *
     * @return selectedCode=null 即未选套（表空或键不完整）；params=当前四键原值
     */
    public Map<String, Object> currentState()
    {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("selectedCode", null);
        state.put("stateLabel", "未选套：策略未生效，搁置期只记不判");
        Map<String, String> current = currentKeyValues();
        state.put("params", current);
        if (!current.isEmpty())
        {
            for (PolicyPreset p : PolicyPreset.values())
            {
                if (matchesPreset(current, p))
                {
                    state.put("selectedCode", p.name());
                    state.put("stateLabel", "已选套：" + p.getLabel() + "（" + p.getDescription()
                            + "）；记账生效随统计管线（S4 调度启用后）");
                    break;
                }
            }
            // 宽容匹配：三模式键对上但阈值为自定义覆盖（如乙+40%）——仍算已选套，标注自定义
            if (state.get("selectedCode") == null)
            {
                for (PolicyPreset p : PolicyPreset.values())
                {
                    Map<String, String> expected = p.params(null);
                    boolean modesMatch =
                            expected.get(KEY_UNDECODABLE_MODE).equals(current.get(KEY_UNDECODABLE_MODE))
                            && expected.get(KEY_INVALID_TIER_MODE).equals(current.get(KEY_INVALID_TIER_MODE))
                            && expected.get(KEY_PARTIAL_MISSING_MODE).equals(current.get(KEY_PARTIAL_MISSING_MODE))
                            && current.containsKey(KEY_PARTIAL_MISSING_THRESHOLD)
                                    == expected.containsKey(KEY_PARTIAL_MISSING_THRESHOLD);
                    if (modesMatch)
                    {
                        state.put("selectedCode", p.name());
                        state.put("stateLabel", "已选套：" + p.getLabel() + "（阈值自定义 "
                                + current.get(KEY_PARTIAL_MISSING_THRESHOLD) + "%）；记账生效随统计管线");
                        break;
                    }
                }
            }
        }
        return state;
    }

    private Map<String, String> currentKeyValues()
    {
        List<VqmsPolicyParam> rows = policyParamMapper.selectList();
        Map<String, String> byKey = new HashMap<>();
        for (VqmsPolicyParam row : rows)
        {
            byKey.put(row.getParamKey(), row.getParamValue());
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : new String[] {KEY_UNDECODABLE_MODE, KEY_INVALID_TIER_MODE,
                KEY_PARTIAL_MISSING_MODE, KEY_PARTIAL_MISSING_THRESHOLD})
        {
            if (byKey.containsKey(key))
            {
                out.put(key, byKey.get(key));
            }
        }
        return out;
    }

    private boolean matchesPreset(Map<String, String> current, PolicyPreset p)
    {
        try
        {
            Map<String, String> expected = p.params(null);
            return expected.equals(current);
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    private String describeCurrentSelection()
    {
        Map<String, Object> state = currentState();
        return state.get("selectedCode") == null ? "未选套" : String.valueOf(state.get("selectedCode"));
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
