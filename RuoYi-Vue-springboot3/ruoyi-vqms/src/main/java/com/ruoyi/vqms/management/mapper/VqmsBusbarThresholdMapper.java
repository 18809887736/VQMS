package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsBusbarThreshold;

/**
 * VQMS 母线电压阈值 Mapper（主库 master）
 */
public interface VqmsBusbarThresholdMapper
{
    List<VqmsBusbarThreshold> selectList(@Param("busbarNum") Long busbarNum, @Param("vGrade") Integer vGrade);
    VqmsBusbarThreshold selectById(Long thresholdId);
    VqmsBusbarThreshold selectByBusbarAndDate(Long busbarNum, String dateStr);
    int insert(VqmsBusbarThreshold entity);
    int update(VqmsBusbarThreshold entity);
    int deleteById(Long thresholdId);
    /** 校验:busbar_threshold.busbar_num 是否存在 */
    long countByBusbarNum(Long busbarNum);
    /** 查当前生效区间内的阈值行 */
    List<VqmsBusbarThreshold> selectEffectiveByBusbarNum(Long busbarNum, String dateStr);
    /** 闭合旧记录：设置 effective_to 为 effective_from-1 */
    int closeExistingByBusbarNum(Long busbarNum);
}
