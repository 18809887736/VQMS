package com.ruoyi.vqms.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.management.service.VqmsPolicyParamService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * S5 L1：策略一键切换 IT（主库直连）——{@code applyPreset} 轮换甲/丙/丁/乙四套，
 * 逐套断言 vqms_policy_param 四约定键库态与 currentState 判定；终态恢复已拍板的乙。
 *
 * <p>RedisCache 用 mock（写穿行为已在 L0 验证）；本 IT 证明的是「换套即换配置」的
 * 实库切换链路。运行需 {@code MYSQL_ROOT_PASSWORD}。</p>
 */
class S5PolicySwitchIT
{
    private static VqmsPolicyParamService service;

    @BeforeAll
    static void setUp() throws Exception
    {
        String password = System.getenv("MYSQL_ROOT_PASSWORD");
        assertNotNull(password, "缺少环境变量 MYSQL_ROOT_PASSWORD");

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://10.0.0.9:13306/ry_vqms?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
        ds.setUsername("root");
        ds.setPassword(password);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory factory = factoryBean.getObject();
        VqmsPolicyParamMapper mapper = factory.getConfiguration()
                .getMapper(VqmsPolicyParamMapper.class, factory.openSession());

        service = new VqmsPolicyParamService();
        java.lang.reflect.Field f = VqmsPolicyParamService.class.getDeclaredField("policyParamMapper");
        f.setAccessible(true);
        f.set(service, mapper);
        java.lang.reflect.Field r = VqmsPolicyParamService.class.getDeclaredField("redisCache");
        r.setAccessible(true);
        r.set(service, mock(RedisCache.class));
    }

    @AfterAll
    static void restoreRatified()
    {
        // 终态恢复已拍板的乙（50），不留测试残留
        service.applyPreset("YI", null, "it-restore");
    }

    private static Map<String, String> dbKeyValues()
    throws Exception
    {
        List<VqmsPolicyParam> rows = service.selectList();
        Map<String, String> kv = new java.util.HashMap<>();
        for (VqmsPolicyParam r : rows)
        {
            kv.put(r.getParamKey(), r.getParamValue()); // 值可为 null（无阈值预设），toMap 会 NPE
        }
        return kv;
    }

    private static final String[][] EXPECTED = {
            // preset, undecodable, invalidTier, partial, threshold
            {"JIA", "EXCLUDE_REPORTED", "EXCLUDE_REPORTED", "COUNT_NORMAL", "null"},
            {"BING", "COUNT_UNQUALIFIED", "COUNT_UNQUALIFIED", "COUNT_UNQUALIFIED", "null"},
            {"DING", "PEND_MARKED", "PEND_MARKED", "PEND_MARKED", "null"},
            {"YI", "EXCLUDE_REPORTED", "EXCLUDE_REPORTED", "EXCLUDE_REPORTED", "50"},
    };

    @Test
    void oneClickSwitch_cyclesAllFourPresets()
    throws Exception
    {
        for (String[] e : EXPECTED)
        {
            assertEquals(e[0], service.applyPreset(e[0], null, "it-switch"),
                    e[0] + " 应用返回");
            Map<String, String> kv = dbKeyValues();
            assertEquals(4, kv.size(), e[0] + " 整组四键");
            assertEquals(e[1], kv.get("undecodable_mode"), e[0] + " undecodable_mode");
            assertEquals(e[2], kv.get("invalid_tier_mode"), e[0] + " invalid_tier_mode");
            assertEquals(e[3], kv.get("partial_missing_mode"), e[0] + " partial_missing_mode");
            if ("null".equals(e[4]))
            {
                assertNull(kv.get("partial_missing_threshold_pct"), e[0] + " 无阈值键值");
            }
            else
            {
                assertEquals(e[4], kv.get("partial_missing_threshold_pct"), e[0] + " 阈值默认");
            }
            assertEquals(e[0], service.currentState().get("selectedCode"),
                    e[0] + " 状态判定应识别所选套别");
        }
    }

    @Test
    void yiThresholdOverride_switchPersistsCustomValue()
    throws Exception
    {
        service.applyPreset("YI", 40, "it-switch");
        Map<String, String> kv = dbKeyValues();
        assertEquals("40", kv.get("partial_missing_threshold_pct"), "乙阈值覆盖应持久化");
        assertEquals("YI", service.currentState().get("selectedCode"));
    }
}
