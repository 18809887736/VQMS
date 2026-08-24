package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsRegulationCmd;

/**
 * 调节合格率指令级明细 mapper（S4 Pipeline 专用写入面；读侧随统计查询交付再补）。
 */
public interface VqmsRegulationCmdMapper
{
    /** 幂等重算写入：同 uk（warn_time+millisecond+归一 obj）覆盖更新全部记账列 */
    int upsertBatch(@Param("list") List<VqmsRegulationCmd> list);
}
