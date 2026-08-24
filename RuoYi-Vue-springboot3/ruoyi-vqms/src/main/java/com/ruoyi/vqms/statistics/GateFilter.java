package com.ruoyi.vqms.statistics;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.vqms.management.domain.VqmsYcPointMap;
import com.ruoyi.vqms.management.mapper.VqmsYcPointMapMapper;
import com.ruoyi.vqms.source.reader.YxSignalReader;

/**
 * 门控前置过滤（三阶段管线阶段一，v5.0 §8.8.2；正式 v1_0 §2.0）。
 *
 * <p>读投退 + 远方就地等门控点（经 {@link YxSignalReader} 阶跃保持语义），
 * 全部启用点保持值 = 1 才进入判定。<b>无启用点 → 直通</b>
 * （{@code gate_enabled=0} 保守默认已拍板空转——种子 3009/2003 均为 0；
 * 真实现场点号核对置 1 后拦截行为随联调验证）。</p>
 */
@Component
public class GateFilter
{
    private final VqmsYcPointMapMapper pointMapMapper;
    private final YxSignalReader yxSignalReader;

    public GateFilter(VqmsYcPointMapMapper pointMapMapper, YxSignalReader yxSignalReader)
    {
        this.pointMapMapper = pointMapMapper;
        this.yxSignalReader = yxSignalReader;
    }

    /**
     * @param commandMinute 指令时刻（t₀，就近取整后）
     * @return true = 可进入判定；false = 门控拦截（AVC 未投入 / 就地模式 / 信号缺失）
     */
    public boolean shouldJudge(LocalDateTime commandMinute)
    {
        List<Long> gatePoints = pointMapMapper.selectList().stream()
                .filter(p -> p.getGateEnabled() != null && p.getGateEnabled() == 1)
                .filter(p -> "yx".equals(p.getPointType()))
                .map(VqmsYcPointMap::getYcNum)
                .toList();
        if (gatePoints.isEmpty())
        {
            return true; // 直通：无启用门控点（拍板空转语义）
        }
        return gatePoints.stream().allMatch(pt ->
                yxSignalReader.heldValue(pt, commandMinute).orElse(0) == 1);
    }
}
