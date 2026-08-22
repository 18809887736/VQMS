package com.ruoyi.vqms.management.service;

import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.vqms.management.domain.VqmsJudgeParam;
import com.ruoyi.vqms.management.mapper.VqmsJudgeParamMapper;

/**
 * VQMS 判定整定参数 Service（v5.0 §6.2.5，D7）。
 *
 * <p>值域/锁定双层防线：Service 层友好报错（t_fast∈[1,4] 且 &lt; t_econ、锁定行拒绝修改），
 * DB CHECK 结构性兜底（旁路直改被拒——测试方案 §4.6 两段断言之 DB 段）。
 * 读走 Redis 缓存 {@code vqms:judgeParam:{key}}，写后覆盖（带 TTL 兜底收敛，非 evict——规约
 * §6.2.5 字面 CacheEvict，实现取写穿 + TTL，行为契约「改值后下一次读取即新值」等价且省一次回源，
 * 差异已在 D7 报告声明）、启动灌缓存——判定侧（搁置轨 S1）经 {@link #getInt(String)} 取参。</p>
 */
@Service
public class VqmsJudgeParamService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsJudgeParamService.class);

    /** 锁定行：param_value 不可改（t_econ 写死=5，分档阈值=附件6 政策值） */
    private static final Set<String> LOCKED_KEYS = Set.of("t_econ", "tier_threshold_fast", "tier_threshold_econ");

    /** t_fast 跨行约束：须 < t_econ（DB 由「t_econ 钉 5 ∧ t_fast∈[1,4]」传导保证，Service 给友好报错） */
    private static final String FAST_KEY = "t_fast";

    private static final String ECON_KEY = "t_econ";

    /** 缓存 TTL：兜底收敛窗口——写 DB 后、覆盖缓存前崩溃的陈旧值最迟 24h 自愈（无 TTL 则永不收敛） */
    private static final java.time.Duration CACHE_TTL = java.time.Duration.ofHours(24);

    @Autowired
    private VqmsJudgeParamMapper judgeParamMapper;

    @Autowired
    private RedisCache redisCache;

    @PostConstruct
    public void warmupCache()
    {
        java.util.List<VqmsJudgeParam> all = judgeParamMapper.selectList();
        all.forEach(this::putCache);
        log.info("判定参数缓存预热完成（{} 项）", all.size());
    }

    /** 判定侧取参入口：缓存优先，miss 回源并灌缓存 */
    public int getInt(String paramKey)
    {
        String cacheKey = cacheKey(paramKey);
        Integer cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return cached;
        }
        VqmsJudgeParam param = judgeParamMapper.selectByKey(paramKey);
        if (param == null)
        {
            throw new IllegalArgumentException("判定参数不存在: " + paramKey);
        }
        if (!"0".equals(param.getStatus()))
        {
            throw new IllegalArgumentException("判定参数已停用: " + paramKey);
        }
        redisCache.setCacheObject(cacheKey, param.getParamValue(), (int) CACHE_TTL.getSeconds(),
                java.util.concurrent.TimeUnit.SECONDS);
        return param.getParamValue();
    }

    public List<VqmsJudgeParam> selectList()
    {
        return judgeParamMapper.selectList();
    }

    public VqmsJudgeParam selectById(Long paramId)
    {
        return selectAll().stream()
                .filter(p -> paramId.equals(p.getParamId()))
                .findFirst()
                .orElse(null);
    }

    public int insert(VqmsJudgeParam entity)
    {
        validateValueRange(entity);
        if (judgeParamMapper.countByKey(entity.getParamKey()) > 0)
        {
            throw new IllegalArgumentException("参数键已存在: " + entity.getParamKey());
        }
        int rows = judgeParamMapper.insert(entity);
        putCache(entity);
        return rows;
    }

    public int update(VqmsJudgeParam entity)
    {
        VqmsJudgeParam current = judgeParamMapper.selectByKey(entity.getParamKey());
        if (current == null)
        {
            throw new IllegalArgumentException("参数不存在: " + entity.getParamKey());
        }
        if (LOCKED_KEYS.contains(current.getParamKey()))
        {
            throw new IllegalArgumentException("锁定行不可修改: " + current.getParamKey()
                    + "（t_econ 写死=5、分档阈值为附件6 政策值）");
        }
        VqmsJudgeParam merged = merge(current, entity);
        validateValueRange(merged);
        validateCrossRow(merged);
        int rows = judgeParamMapper.update(merged);
        if (rows == 0)
        {
            redisCache.deleteObject(cacheKey(merged.getParamKey()));
            throw new IllegalArgumentException("参数不存在: " + merged.getParamKey());
        }
        putCache(merged);
        return rows;
    }

    public int deleteById(Long paramId)
    {
        VqmsJudgeParam current = selectById(paramId);
        if (current == null)
        {
            return 0;
        }
        if (LOCKED_KEYS.contains(current.getParamKey()) || FAST_KEY.equals(current.getParamKey()))
        {
            throw new IllegalArgumentException("判定必需参数不可删除: " + current.getParamKey());
        }
        int rows = judgeParamMapper.deleteById(paramId);
        redisCache.deleteObject(cacheKey(current.getParamKey()));
        return rows;
    }

    private void validateValueRange(VqmsJudgeParam param)
    {
        if (param.getParamValue() == null)
        {
            throw new IllegalArgumentException("参数值不能为空");
        }
        if (param.getValueMin() != null && param.getParamValue() < param.getValueMin()
                || param.getValueMax() != null && param.getParamValue() > param.getValueMax())
        {
            throw new IllegalArgumentException("参数值 " + param.getParamKey() + "=" + param.getParamValue()
                    + " 超出值域 [" + param.getValueMin() + ", " + param.getValueMax() + "]");
        }
    }

    private void validateCrossRow(VqmsJudgeParam param)
    {
        if (!FAST_KEY.equals(param.getParamKey()))
        {
            return;
        }
        int tEcon = judgeParamMapper.selectByKey(ECON_KEY).getParamValue();
        if (param.getParamValue() >= tEcon)
        {
            throw new IllegalArgumentException(
                    "t_fast=" + param.getParamValue() + " 须小于 t_econ=" + tEcon);
        }
    }

    private static VqmsJudgeParam merge(VqmsJudgeParam current, VqmsJudgeParam patch)
    {
        current.setParamValue(patch.getParamValue());
        if (patch.getDescription() != null)
        {
            current.setDescription(patch.getDescription());
        }
        if (patch.getRemark() != null)
        {
            current.setRemark(patch.getRemark());
        }
        return current;
    }

    private void putCache(VqmsJudgeParam param)
    {
        if (param.getParamKey() != null && param.getParamValue() != null)
        {
            redisCache.setCacheObject(cacheKey(param.getParamKey()), param.getParamValue(),
                    (int) CACHE_TTL.getSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private static String cacheKey(String paramKey)
    {
        return "vqms:judgeParam:" + paramKey;
    }

    private List<VqmsJudgeParam> selectAll()
    {
        return judgeParamMapper.selectList();
    }
}
