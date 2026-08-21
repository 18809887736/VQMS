package com.ruoyi.vqms.source.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.source.model.YcHistory;

/**
 * yc_history 读取 mapper（只读，外部源 SLAVE 数据源）。
 */
public interface YcHistoryMapper
{
    List<YcHistory> selectByRangeAndYc(@Param("startTime") String startTime,
                                      @Param("endTime") String endTime,
                                      @Param("ycNum") Long ycNum);

    List<YcHistory> selectByRange(@Param("startTime") String startTime,
                                 @Param("endTime") String endTime);
}
