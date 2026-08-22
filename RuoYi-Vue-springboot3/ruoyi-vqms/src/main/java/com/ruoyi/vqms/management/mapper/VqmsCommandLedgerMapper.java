package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsCommandLedger;

/**
 * vqms_command_ledger 读写 mapper（主库）。
 *
 * <p>只增表——无 update。insertIgnoreBatch 为幂等入口（uk 冲突跳过，
 * 返回实际新增行数）；deleteByWarnTimeRange 仅供测试行级自清理。</p>
 */
public interface VqmsCommandLedgerMapper
{
    /** 批量摘录入账（insert ignore，uk 冲突静默跳过）。@return 实际新增行数 */
    int insertIgnoreBatch(@Param("list") List<VqmsCommandLedger> list);

    long countAll();

    List<VqmsCommandLedger> selectByWarnTimeRange(@Param("startTime") String startTime,
                                                  @Param("endTime") String endTime);

    /** 仅测试行级自清理用（只增表无业务删除路径） */
    int deleteByWarnTimeRange(@Param("startTime") String startTime,
                              @Param("endTime") String endTime);
}
