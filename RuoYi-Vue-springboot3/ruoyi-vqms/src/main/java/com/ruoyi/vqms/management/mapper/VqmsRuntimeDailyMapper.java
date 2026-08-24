package com.ruoyi.vqms.management.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsRuntimeDaily;

/**
 * AVC 投运率日记账 mapper（S4 RuntimePipeline 专用写入面；读侧随统计查询交付再补）。
 */
public interface VqmsRuntimeDailyMapper
{
    /** 幂等重算写入：同 stat_date 覆盖更新全部记账列 */
    int upsert(@Param("row") VqmsRuntimeDaily row);
}
