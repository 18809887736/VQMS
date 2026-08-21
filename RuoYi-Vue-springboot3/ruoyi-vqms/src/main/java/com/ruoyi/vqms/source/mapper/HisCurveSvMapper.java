package com.ruoyi.vqms.source.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.source.model.HisCurveSv;

/**
 * his_curve_sv 读取 mapper（只读，外部源 SLAVE 数据源）。
 */
public interface HisCurveSvMapper
{
    List<HisCurveSv> selectByRangeAndBusbar(@Param("startTime") String startTime,
                                           @Param("endTime") String endTime,
                                           @Param("busbarNum") Long busbarNum);

    List<HisCurveSv> selectByRange(@Param("startTime") String startTime,
                                  @Param("endTime") String endTime);
}
