package com.ruoyi.web.controller.vqms;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarThresholdMapper;
import com.ruoyi.vqms.management.service.VqmsBusbarService;
import com.ruoyi.vqms.management.service.VqmsBusbarThresholdService;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.model.YcHistory;
import com.ruoyi.vqms.source.reader.SourceReader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * D5 验收（L1，测试方案 §4.6）：standalone MockMvc + 真实 Service（ry_vqms @ 10.0.0.9:13306）
 * + SourceReader 桩（真实读取路径已由 D1/D4 IT 覆盖）。
 *
 * <p>凭证走环境变量 MYSQL_ROOT_PASSWORD（必填，不入库）。测试数据自清理（busbar 9876 + 其阈值）。</p>
 */
class D5ControllerIT
{
    private static final long TEST_BUSBAR = 9876L;

    private static MockMvc mockMvc;

    private static VqmsBusbarMapper busbarMapper;

    private static VqmsBusbarThresholdMapper thresholdMapper;

    /** SourceReader 桩：确定性返回 3 行曲线，验证 controller 分页/契约 */
    private static SourceReader stubReader()
    {
        return new SourceReader()
        {
            @Override
            public List<HisCurveSv> readCurve(String startTime, String endTime, Long busbarNum)
            {
                List<HisCurveSv> rows = new ArrayList<>();
                for (int i = 0; i < 3; i++)
                {
                    HisCurveSv row = new HisCurveSv();
                    row.setSaveTime("2026-08-21 10:0" + i + ":00.000");
                    row.setBusbarNum(busbarNum);
                    row.setHighSV(new java.math.BigDecimal("234.5"));
                    row.setLowSV(new java.math.BigDecimal("233.5"));
                    rows.add(row);
                }
                return rows;
            }

            @Override
            public List<YcHistory> readYc(String startTime, String endTime, Long ycNum)
            {
                return List.of();
            }

            @Override
            public List<WarnInfo> readWarn(String startTime, String endTime, Long warnType)
            {
                return List.of();
            }
        };
    }

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://10.0.0.9:13306/ry_vqms"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
        ds.setUsername("root");
        ds.setPassword(java.util.Objects.requireNonNull(System.getenv("MYSQL_ROOT_PASSWORD"),
                "缺少 MYSQL_ROOT_PASSWORD"));

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory sessionFactory = factoryBean.getObject();
        busbarMapper = sessionFactory.getConfiguration().getMapper(VqmsBusbarMapper.class, sessionFactory.openSession());
        thresholdMapper = sessionFactory.getConfiguration().getMapper(VqmsBusbarThresholdMapper.class,
                sessionFactory.openSession());

        VqmsBusbarService busbarService = new VqmsBusbarService();
        inject(busbarService, VqmsBusbarService.class, "busbarMapper", busbarMapper);
        VqmsBusbarThresholdService thresholdService = new VqmsBusbarThresholdService();
        inject(thresholdService, VqmsBusbarThresholdService.class, "thresholdMapper", thresholdMapper);
        inject(thresholdService, VqmsBusbarThresholdService.class, "busbarMapper", busbarMapper);

        VqmsThresholdController thresholdController = new VqmsThresholdController();
        inject(thresholdController, VqmsThresholdController.class, "thresholdService", thresholdService);
        VqmsBusbarController busbarController = new VqmsBusbarController();
        inject(busbarController, VqmsBusbarController.class, "busbarService", busbarService);
        VqmsCurveController curveController = new VqmsCurveController();
        inject(curveController, VqmsCurveController.class, "sourceReader", stubReader());

        mockMvc = MockMvcBuilders.standaloneSetup(
                thresholdController, busbarController, curveController,
                new VqmsStatsController(), new VqmsAvcController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 测试母线自清理前置
        cleanup();
        VqmsBusbar busbar = new VqmsBusbar();
        busbar.setBusbarNum(TEST_BUSBAR);
        busbar.setBusbarName("D5 测试母线");
        busbar.setVGrade(1);
        busbar.setNominalKv(new java.math.BigDecimal("220"));
        busbarMapper.insert(busbar);
    }

    private static void cleanup()
    {
        List<VqmsBusbarThreshold> existing = thresholdMapper.selectList(TEST_BUSBAR, null);
        existing.forEach(t -> thresholdMapper.deleteById(t.getThresholdId()));
        busbarMapper.deleteByBusbarNum(TEST_BUSBAR);
    }

    @AfterAll
    static void tearDown()
    {
        cleanup();
    }

    private static void inject(Object target, Class<?> clazz, String field, Object value) throws Exception
    {
        java.lang.reflect.Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void threshold_增查改删闭环() throws Exception
    {
        // list 前置为空
        MvcResult before = mockMvc.perform(get("/vqms/threshold/list").param("busbarNum", String.valueOf(TEST_BUSBAR)))
                .andExpect(status().isOk()).andReturn();
        Assertions.assertEquals(0, JSON.parseObject(before.getResponse().getContentAsString()).getIntValue("total"));

        // add
        mockMvc.perform(post("/vqms/threshold").contentType("application/json")
                .content("{\"busbarNum\":" + TEST_BUSBAR + ",\"criterionType\":\"AVC\",\"toleranceV\":1.000,"
                        + "\"effectiveFrom\":\"2026-08-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // list 有 1 行
        MvcResult after = mockMvc.perform(get("/vqms/threshold/list").param("busbarNum", String.valueOf(TEST_BUSBAR)))
                .andExpect(status().isOk()).andReturn();
        JSONObject table = JSON.parseObject(after.getResponse().getContentAsString());
        Assertions.assertEquals(1, table.getIntValue("total"));
        Long id = table.getJSONArray("rows").getJSONObject(0).getLong("thresholdId");

        // getInfo
        mockMvc.perform(get("/vqms/threshold/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.busbarNum").value(TEST_BUSBAR));

        // edit
        mockMvc.perform(put("/vqms/threshold").contentType("application/json")
                .content("{\"thresholdId\":" + id + ",\"busbarNum\":" + TEST_BUSBAR
                        + ",\"criterionType\":\"AVC\",\"toleranceV\":1.500,\"effectiveFrom\":\"2026-08-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // remove
        mockMvc.perform(delete("/vqms/threshold/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void threshold_非法FK被Service层拒绝() throws Exception
    {
        mockMvc.perform(post("/vqms/threshold").contentType("application/json")
                .content("{\"busbarNum\":999999,\"criterionType\":\"AVC\",\"toleranceV\":1.000,"
                        + "\"effectiveFrom\":\"2026-08-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void busbar_下拉含vGrade() throws Exception
    {
        mockMvc.perform(get("/vqms/vqms_busbar/list")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].vGrade").exists());
    }

    @Test
    void curve_分页与数据契约() throws Exception
    {
        mockMvc.perform(get("/vqms/curve/list")
                .param("startTime", "2026-08-21 10:00:00").param("endTime", "2026-08-21 10:05:00")
                .param("busbarNum", "0").param("pageNum", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].highSV").exists())
                .andExpect(jsonPath("$.rows[0].lowSV").exists())
                .andExpect(jsonPath("$.rows[0].saveTime").value("2026-08-21 10:00:00.000"));
    }

    @Test
    void curve_范围超31天被拒() throws Exception
    {
        mockMvc.perform(get("/vqms/curve/list")
                .param("startTime", "2026-08-01 00:00:00").param("endTime", "2026-09-15 00:00:00")
                .param("busbarNum", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void curve_缺母线被拒() throws Exception
    {
        mockMvc.perform(get("/vqms/curve/list")
                .param("startTime", "2026-08-21 10:00:00").param("endTime", "2026-08-21 10:05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void 搁置轨端点_空表与显式拒绝()
    {
        assertEmptyList("/vqms/stats/daily/list");
        assertEmptyList("/vqms/stats/monthly/list");
        assertEmptyList("/vqms/stats/yearly/list");
        assertEmptyList("/vqms/avc/runtime/list");
        assertEmptyList("/vqms/avc/regulation/list");
    }

    private void assertEmptyList(String url)
    {
        try
        {
            mockMvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.rows.length()").value(0));
        }
        catch (Exception e)
        {
            throw new RuntimeException(url, e);
        }
    }
}
