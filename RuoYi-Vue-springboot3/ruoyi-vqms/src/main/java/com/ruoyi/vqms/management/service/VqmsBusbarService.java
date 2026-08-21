package com.ruoyi.vqms.management.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;

/**
 * VQMS 主母线 Service（含逻辑 FK 校验）。
 *
 * <p>v5.0 §6.2 要求:维护 busbar 时校验 group_num 在 vqms_busbar_group 存在。</p>
 */
@Service
public class VqmsBusbarService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsBusbarService.class);

    @Autowired
    private VqmsBusbarMapper busbarMapper;

    @Autowired
    private VqmsBusbarGroupMapper busbarGroupMapper;

    public List<VqmsBusbar> selectList()
    {
        return busbarMapper.selectList();
    }

    public VqmsBusbar selectByBusbarNum(Long busbarNum)
    {
        return busbarMapper.selectByBusbarNum(busbarNum);
    }

    /**
     * 新增主母线 —— 先校验 group_num 若不为 null，则在 vqms_busbar_group 中必须存在。
     */
    public int insert(VqmsBusbar entity)
    {
        validateGroupNum(entity.getGroupNum());
        return busbarMapper.insert(entity);
    }

    /**
     * 修改主母线 —— 校验同 insert。
     */
    public int update(VqmsBusbar entity)
    {
        validateGroupNum(entity.getGroupNum());
        return busbarMapper.update(entity);
    }

    /**
     * 删除主母线。
     */
    public int deleteByBusbarNum(Long busbarNum)
    {
        return busbarMapper.deleteByBusbarNum(busbarNum);
    }

    /**
     * 删除母线组前校验：该组不能有 busbar 引用。
     *
     * @throws IllegalStateException 若有 busbar 引用则拒绝删除
     */
    public void validateGroupNotReferenced(Long groupNum)
    {
        long refCount = busbarMapper.countByGroupNum(groupNum);
        if (refCount > 0)
        {
            throw new IllegalStateException(
                    "无法删除 group_num=" + groupNum + "：有 " + refCount + " 条 busbar 引用该组");
        }
        log.debug("group_num={} 无 busbar 引用，可安全删除", groupNum);
    }

    /**
     * 校验 group_num 若不为 null，必须在 vqms_busbar_group 中存在。
     *
     * @throws IllegalArgumentException 若引用的组不存在
     */
    private void validateGroupNum(Long groupNum)
    {
        if (groupNum == null)
        {
            return;
        }
        long count = busbarGroupMapper.countByGroupNum(groupNum);
        if (count == 0)
        {
            throw new IllegalArgumentException(
                    "group_num=" + groupNum + " 在 vqms_busbar_group 中不存在");
        }
        log.debug("group_num={} 校验通过", groupNum);
    }
}
