package com.ruoyi.vqms.management;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.management.service.VqmsPolicyParamService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S5 L0：策略参数页 Service——apply 整组 upsert + 写穿缓存（§8.7 规格）+ 三态状态判定。
 */
class VqmsPolicyParamServiceTest
{
    private final VqmsPolicyParamMapper mapper = mock(VqmsPolicyParamMapper.class);
    private final RedisCache redisCache = mock(RedisCache.class);

    private VqmsPolicyParamService service;

    @BeforeEach
    void setUp()
    {
        service = new VqmsPolicyParamService();
        inject("policyParamMapper", mapper);
        inject("redisCache", redisCache);
        when(mapper.selectList()).thenReturn(List.of());
        when(mapper.selectByKey(anyString())).thenReturn(null);
    }

    private void inject(String field, Object value)
    {
        try
        {
            java.lang.reflect.Field f = VqmsPolicyParamService.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(service, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static VqmsPolicyParam row(String key, String value)
    {
        VqmsPolicyParam r = new VqmsPolicyParam();
        r.setParamKey(key);
        r.setParamValue(value);
        return r;
    }

    @Test
    void unknownPreset_throwsFriendly()
    {
        assertThrows(ServiceException.class, () -> service.applyPreset("NOPE", null, "admin"));
    }

    @Test
    void applyOnEmptyTable_insertsFourRows_andWritesThroughCache()
    {
        String applied = service.applyPreset("YI", 50, "admin");
        assertEquals("YI", applied);

        ArgumentCaptor<VqmsPolicyParam> captor = ArgumentCaptor.forClass(VqmsPolicyParam.class);
        verify(mapper, times(4)).insert(captor.capture());
        assertEquals(4, captor.getAllValues().size(), "整组四约定键一次写入");
        // 写穿：per-key 四次 setCacheObject，24h TTL
        verify(redisCache, times(4)).setCacheObject(anyString(), any(), eq(24), any());

        assertNotNull(captor.getAllValues().get(0).getName(), "name 非空列须有值");
    }

    @Test
    void applyOverExisting_updatesInsteadOfInsert()
    {
        when(mapper.selectByKey(anyString()))
                .thenReturn(row("undecodable_mode", "COUNT_UNQUALIFIED"));
        service.applyPreset("BING", null, "leo");

        // 第 4 键（threshold）值为 null——验证器须用 nullable(String.class)，anyString() 不匹配 null
        verify(mapper, times(4)).updateValue(anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class), eq("leo"));
        verify(mapper, never()).insert(any(VqmsPolicyParam.class));
    }

    @Test
    void currentState_threeStates()
    {
        // 未选套：表空
        Map<String, Object> empty = service.currentState();
        assertNull(empty.get("selectedCode"));
        assertTrue(String.valueOf(empty.get("stateLabel")).contains("未选套"));

        // 已选套·乙默认 50：精确匹配
        when(mapper.selectList()).thenReturn(List.of(
                row("undecodable_mode", "EXCLUDE_REPORTED"),
                row("invalid_tier_mode", "EXCLUDE_REPORTED"),
                row("partial_missing_mode", "EXCLUDE_REPORTED"),
                row("partial_missing_threshold_pct", "50")));
        Map<String, Object> yi = service.currentState();
        assertEquals("YI", yi.get("selectedCode"));

        // 已选套·乙自定义阈值 40：宽容匹配标注自定义
        when(mapper.selectList()).thenReturn(List.of(
                row("undecodable_mode", "EXCLUDE_REPORTED"),
                row("invalid_tier_mode", "EXCLUDE_REPORTED"),
                row("partial_missing_mode", "EXCLUDE_REPORTED"),
                row("partial_missing_threshold_pct", "40")));
        Map<String, Object> custom = service.currentState();
        assertEquals("YI", custom.get("selectedCode"));
        assertTrue(String.valueOf(custom.get("stateLabel")).contains("40"), "自定义阈值应标注在状态里");
    }

    @Test
    void loadConfig_stillConsumable_afterApply()
    {
        // apply 落库后 loadConfig 可装配（管线消费面不回归）
        when(mapper.selectList()).thenReturn(List.of(
                row("undecodable_mode", "PEND_MARKED"),
                row("invalid_tier_mode", "PEND_MARKED"),
                row("partial_missing_mode", "PEND_MARKED")));
        Optional<com.ruoyi.vqms.statistics.PolicyConfig> config = service.loadConfig();
        assertTrue(config.isPresent());
        assertEquals(com.ruoyi.vqms.statistics.Disposition.PEND_MARKED,
                config.get().undecodableMode());
    }
}
