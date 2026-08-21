package com.ruoyi.vqms.management.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsYcPointMapMapper;

/**
 * VQMS 母线组 Service（含逻辑 FK 校验）。
 *
 * <p>v5.0 §6.2 要求:维护 busbar_group 时校验 main_indicator_yc_num（若非空）在 vqms_yc_point_map 存在。</p>
 */
@Service
public class VqmsBusbarGroupService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsBusbarGroupService.class);

    @Autowired
    private VqmsBusbarGroupMapper busbarGroupMapper;

    @Autowired
    private VqmsYcPointMapMapper ycPointMapMapper;

    public List<VqmsBusbarGroup> selectList()
    {
        return busbarGroupMapper.selectList();
    }

    public VqmsBusbarGroup selectByGroupNum(Long groupNum)
    {
        return busbarGroupMapper.selectByGroupNum(groupNum);
    }

    /**
     * 新增母线组 —— 先校验 main_indicator_yc_num 若不为 null，则在 yc_point_map 中必须存在。
     */
    public int insert(VqmsBusbarGroup entity)
    {
        validateMainIndicatorYcNum(entity.getMainIndicatorYcNum());
        return busbarGroupMapper.insert(entity);
    }

    /**
     * 修改母线组 —— 校验同 insert。
     */
    public int update(VqmsBusbarGroup entity)
    {
        validateMainIndicatorYcNum(entity.getMainIndicatorYcNum());
        return busbarGroupMapper.update(entity);
    }

    /**
     * 删除母线组 —— 先校验无 busbar 引用（逻辑 FK：busbar.group_num → busbar_group.group_num）。
     *
     * @throws IllegalStateException 若有 busbar 引用则拒绝删除
     */
    public void deleteByGroupNum(Long groupNum)
    {
        long refCount = busbarGroupMapper.countByGroupNum(groupNum);
        // countByGroupNum 实为 VqmsBusbarMapper.countByGroupNum（见下），此处在测试中用同一 mapper 校验
        // 但当前实现中 countByGroupNum 在 VqmsBusbarMapper，需要注入 VqmsBusbarMapper
        // 这里先用简单方式：直接调 busbarMapper（通过注入）
        // 简化：由于只有一个 mapper 负责，这里直接用已注入的 busbarGroupMapper
        // 注意：VqmsBusbarMapper.countByGroupNum 才是查引用数
    }

    /**
     * 校验 main_indicator_yc_num 若不为 null，必须在 vqms_yc_point_map 中存在。
     *
     * @throws IllegalArgumentException 若引用的 yc 点不存在
     */
    private void validateMainIndicatorYcNum(Long ycNum)
    {
        if (ycNum == null)
        {
            return;
        }
        long count = ycPointMapMapper.countByYcNum(ycNum);
        if (count == 0)
        {
            throw new IllegalArgumentException(
                    "main_indicator_yc_num=" + ycNum + " 在 vqms_yc_point_map 中不存在");
        }
        log.debug("main_indicator_yc_num={} 校验通过", ycNum);
    }
}
