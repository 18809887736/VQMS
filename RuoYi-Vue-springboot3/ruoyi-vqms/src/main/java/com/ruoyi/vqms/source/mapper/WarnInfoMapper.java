package com.ruoyi.vqms.source.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.source.model.WarnInfo;

/**
 * warn_info 读取 mapper（只读，外部源 SLAVE 数据源）。
 */
public interface WarnInfoMapper
{
    List<WarnInfo> selectByRangeAndType(@Param("startTime") String startTime,
                                       @Param("endTime") String endTime,
                                       @Param("warnType") Long warnType);

    List<WarnInfo> selectByRange(@Param("startTime") String startTime,
                                @Param("endTime") String endTime);
}
