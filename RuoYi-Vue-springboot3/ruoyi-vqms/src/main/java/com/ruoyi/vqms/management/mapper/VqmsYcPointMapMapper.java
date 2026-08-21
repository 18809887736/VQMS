package com.ruoyi.vqms.management.mapper;

import java.util.List;
import com.ruoyi.vqms.management.domain.VqmsYcPointMap;

/**
 * VQMS yc_history 遥测点映射 Mapper（主库 master）
 */
public interface VqmsYcPointMapMapper
{
    List<VqmsYcPointMap> selectList();
    VqmsYcPointMap selectByYcNum(Long ycNum);
    int insert(VqmsYcPointMap entity);
    int update(VqmsYcPointMap entity);
    int deleteByYcNum(Long ycNum);
    /** 校验:yc_point_map.yc_num 是否存在 */
    long countByYcNum(Long ycNum);
}
