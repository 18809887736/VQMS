package com.ruoyi.vqms.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;

/**
 * VQMS 母线组 Mapper（主库 master）
 */
public interface VqmsBusbarGroupMapper
{
    List<VqmsBusbarGroup> selectList();
    VqmsBusbarGroup selectByGroupNum(Long groupNum);
    int insert(VqmsBusbarGroup entity);
    int update(VqmsBusbarGroup entity);
    int deleteByGroupNum(Long groupNum);
    /** 校验:busbar_group.group_num 是否存在 */
    long countByGroupNum(Long groupNum);
    /** 校验:删除时是否有 busbar 引用该组（逻辑 FK） */
    long countReferencedByBusbar();
}
