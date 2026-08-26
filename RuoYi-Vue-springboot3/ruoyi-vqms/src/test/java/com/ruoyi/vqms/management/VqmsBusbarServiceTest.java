package com.ruoyi.vqms.management;

import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarThresholdMapper;
import com.ruoyi.vqms.management.service.VqmsBusbarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 母线台账 Service L0：删除 RESTRICT 级联守卫（busbar→threshold 引用）+ group_num 逻辑 FK 校验。
 * 镜像 validateGroupNotReferenced 先例（group→busbar 同构守卫）。
 */
@ExtendWith(MockitoExtension.class)
class VqmsBusbarServiceTest
{
    @Mock
    private VqmsBusbarMapper busbarMapper;
    @Mock
    private VqmsBusbarGroupMapper groupMapper;
    @Mock
    private VqmsBusbarThresholdMapper thresholdMapper;
    @InjectMocks
    private VqmsBusbarService service;

    private VqmsBusbar busbar(Long num, Long groupNum)
    {
        VqmsBusbar b = new VqmsBusbar();
        b.setBusbarNum(num);
        b.setBusbarName("测试母线" + num);
        b.setGroupNum(groupNum);
        b.setNominalKv(new java.math.BigDecimal("220.000"));
        return b;
    }

    @Test
    void delete_blocked_whenThresholdsReferenceBusbar()
    {
        when(thresholdMapper.countByBusbarNum(0L)).thenReturn(3L);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.deleteByBusbarNum(0L));
        assertTrue(e.getMessage().contains("3 条阈值配置"), "报文含引用数");
        assertTrue(e.getMessage().contains("阈值管理"), "报文指引清理入口");
        // 守卫拒绝时不得触达删除
        org.mockito.Mockito.verify(busbarMapper, org.mockito.Mockito.never()).deleteByBusbarNum(0L);
    }

    @Test
    void delete_passes_andCountsZero()
    {
        when(thresholdMapper.countByBusbarNum(2L)).thenReturn(0L);
        when(busbarMapper.deleteByBusbarNum(2L)).thenReturn(1);

        assertEquals(1, service.deleteByBusbarNum(2L));
        verify(busbarMapper).deleteByBusbarNum(2L);
    }

    @Test
    void countThresholds_delegates()
    {
        when(thresholdMapper.countByBusbarNum(1L)).thenReturn(7L);
        assertEquals(7L, service.countThresholds(1L));
    }

    @Test
    void insert_rejectsUnknownGroup()
    {
        when(groupMapper.countByGroupNum(99L)).thenReturn(0L);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.insert(busbar(9L, 99L)));
        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    void insert_acceptsKnownGroup_andNullGroup()
    {
        when(groupMapper.countByGroupNum(0L)).thenReturn(1L);
        when(busbarMapper.insert(any())).thenReturn(1);
        assertEquals(1, service.insert(busbar(9L, 0L)));
        assertEquals(1, service.insert(busbar(10L, null)), "group_num=null 免校验");
    }
}
