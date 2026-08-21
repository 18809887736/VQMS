package com.ruoyi.vqms.management.service;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarThresholdMapper;

/**
 * VQMS 母线电压阈值 Service（含逻辑 FK 校验 + 生效区间自动闭合）。
 *
 * <p>v5.0 §6.2.4 要求:
 * <ul>
 *   <li>维护 busbar_threshold 时校验 busbar_num 在 vqms_busbar 存在；</li>
 *   <li>新增阈值自动闭合旧记录 effective_to（同事务）。</li>
 * </ul></p>
 */
@Service
public class VqmsBusbarThresholdService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsBusbarThresholdService.class);

    @Autowired
    private VqmsBusbarThresholdMapper thresholdMapper;

    @Autowired
    private VqmsBusbarMapper busbarMapper;

    public List<VqmsBusbarThreshold> selectList()
    {
        return thresholdMapper.selectList();
    }

    public VqmsBusbarThreshold selectById(Long thresholdId)
    {
        return thresholdMapper.selectById(thresholdId);
    }

    public VqmsBusbarThreshold selectByBusbarAndDate(Long busbarNum, String dateStr)
    {
        return thresholdMapper.selectByBusbarAndDate(busbarNum, dateStr);
    }

    /**
     * 新增阈值 —— 校验 busbar_num 存在，并自动闭合该母线现有生效区间。
     *
     * @return 插入的行数
     */
    @Transactional
    public int insert(VqmsBusbarThreshold entity)
    {
        validateBusbarNum(entity.getBusbarNum());
        // 闭合旧记录
        thresholdMapper.closeExistingByBusbarNum(entity.getBusbarNum());
        return thresholdMapper.insert(entity);
    }

    /**
     * 修改阈值。
     */
    public int update(VqmsBusbarThreshold entity)
    {
        return thresholdMapper.update(entity);
    }

    /**
     * 删除阈值。
     */
    public int deleteById(Long thresholdId)
    {
        return thresholdMapper.deleteById(thresholdId);
    }

    /**
     * 校验 busbar_num 若在 vqms_busbar 中存在。
     *
     * @throws IllegalArgumentException 若母线编号不存在
     */
    private void validateBusbarNum(Long busbarNum)
    {
        if (busbarNum == null)
        {
            return;
        }
        long count = busbarMapper.countByBusbarNum(busbarNum);
        if (count == 0)
        {
            throw new IllegalArgumentException(
                    "busbar_num=" + busbarNum + " 在 vqms_busbar 中不存在");
        }
        log.debug("busbar_num={} 校验通过", busbarNum);
    }
}
