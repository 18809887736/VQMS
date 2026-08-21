package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsBusbar;

/**
 * VQMS 主母线 Mapper（主库 master）
 */
public interface VqmsBusbarMapper
{
    List<VqmsBusbar> selectList();
    VqmsBusbar selectByBusbarNum(Long busbarNum);
    int insert(VqmsBusbar entity);
    int update(VqmsBusbar entity);
    int deleteByBusbarNum(Long busbarNum);
    /** 校验:busbar.busbar_num 是否存在 */
    long countByBusbarNum(Long busbarNum);
    /** 校验:删除 group 时是否有 busbar 引用 */
    long countByGroupNum(Long groupNum);
}
