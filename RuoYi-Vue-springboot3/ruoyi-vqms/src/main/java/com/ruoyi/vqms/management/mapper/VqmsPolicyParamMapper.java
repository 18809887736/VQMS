package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;

/**
 * vqms_policy_param 读写 mapper（主库，D9 骨架）。
 *
 * <p>无 CRUD UI（D9 范围拍板）——当前唯一消费方是 {@code VqmsPolicyParamService.loadConfig()}
 * 与集成测试；UI 随选套定稿按 D7 同款代码生成补。</p>
 */
public interface VqmsPolicyParamMapper
{
    List<VqmsPolicyParam> selectList();

    VqmsPolicyParam selectByKey(@Param("paramKey") String paramKey);

    int insert(VqmsPolicyParam entity);

    int updateValue(@Param("paramKey") String paramKey, @Param("paramValue") String paramValue,
            @Param("updateBy") String updateBy);

    int deleteByKey(@Param("paramKey") String paramKey);

    long countAll();
}
