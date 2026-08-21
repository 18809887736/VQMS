package com.ruoyi.vqms.management.mapper;

import java.util.List;
import com.ruoyi.vqms.management.domain.VqmsJudgeParam;

/**
 * VQMS 判定整定参数 Mapper（主库 master）
 */
public interface VqmsJudgeParamMapper
{
    List<VqmsJudgeParam> selectList();
    VqmsJudgeParam selectByKey(String paramKey);
    int insert(VqmsJudgeParam entity);
    int update(VqmsJudgeParam entity);
    int deleteById(Long paramId);
    /** 校验:judge_param.param_key 是否存在 */
    long countByKey(String paramKey);
}
