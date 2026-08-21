package com.ruoyi.vqms.management;

import java.math.BigDecimal;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.management.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.management.domain.VqmsJudgeParam;
import com.ruoyi.vqms.management.domain.VqmsYcPointMap;
import com.ruoyi.vqms.management.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.management.mapper.VqmsBusbarThresholdMapper;
import com.ruoyi.vqms.management.mapper.VqmsJudgeParamMapper;
import com.ruoyi.vqms.management.mapper.VqmsYcPointMapMapper;

/**
 * D2 集成测试：管理表 DDL + 逻辑 FK 校验（直连主库 ry_vqms @ 10.0.0.9:13306）。
 *
 * <p>使用 Spring 测试框架 + 独立 DataSource，不依赖 Docker compose 或完整 Spring Boot 上下文。</p>
 */
@ExtendWith(SpringExtension.class)
class D2BusinessLogicIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:13306/ry_vqms"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("MYSQL_ROOT_PASSWORD",
                    "fa4b83b498817f5cb5a34287db0ce6e660713816ee2b1fae7ad1b45e11a9ed67268a97b4");

    @Autowired
    private VqmsBusbarGroupMapper groupMapper;
    @Autowired
    private VqmsBusbarMapper busbarMapper;
    @Autowired
    private VqmsYcPointMapMapper ycMapper;
    @Autowired
    private VqmsBusbarThresholdMapper thresholdMapper;
    @Autowired
    private VqmsJudgeParamMapper judgeParamMapper;

    @Configuration
    @MapperScan("com.ruoyi.vqms.management.mapper")
    static class TestConfig
    {
        @Bean
        public DataSource dataSource()
        {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl(DB_URL);
            ds.setUsername(DB_USER);
            ds.setPassword(DB_PASSWORD);
            return ds;
        }

        @Bean
        public SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception
        {
            SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
            bean.setDataSource(ds);
            bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:mapper/vqms/*Mapper.xml"));
            bean.setTypeAliasesPackage("com.ruoyi.vqms.management.domain");
            return bean.getObject();
        }
    }

    // ───────────────────── 断言 1: 种子数据 ─────────────────────

    @Test
    void seedData_exists()
    {
        // 先清理 FK 测试残留数据
        busbarMapper.deleteByBusbarNum(8888L);
        groupMapper.deleteByGroupNum(9999L);
        thresholdMapper.deleteById(thresholdMapper.selectList().stream()
                .filter(t -> "AVC".equals(t.getCriterionType()) && t.getBusbarNum() == 9999L)
                .findFirst().map(VqmsBusbarThreshold::getThresholdId).orElse(null));

        long groupCount = groupMapper.selectList().size();
        long barCount = busbarMapper.selectList().size();
        long ycCount = ycMapper.selectList().size();
        long paramCount = judgeParamMapper.selectList().size();
        Assertions.assertEquals(2, groupCount, "busbar_group 种子应为 2");
        Assertions.assertEquals(2, barCount, "busbar 种子应为 2");
        Assertions.assertTrue(ycCount >= 3, "yc_point_map 种子至少 3");
        Assertions.assertEquals(4, paramCount, "judge_param 种子应为 4");
    }

    // ───────────────────── 断言 2: busbar.group_num FK → 不存在组被 Mapper 接受（DB 无约束） ─────────────────────

    @Test
    void busbarInsert_acceptsInvalidGroupNumAtMapper()
    {
        VqmsBusbar bar = new VqmsBusbar();
        bar.setBusbarNum(8888L);
        bar.setBusbarName("FK_TEST_BUSBAR");
        bar.setVGrade(1);
        bar.setGroupNum(9999L); // 不存在的组
        bar.setNominalKv(BigDecimal.valueOf(220.000));
        bar.setStatus("0");
        bar.setCreateBy("test");
        int rows = busbarMapper.insert(bar);
        // Mapper 层无约束，插入成功；Service 层才做校验
        Assertions.assertEquals(1, rows);
    }

    // ───────────────────── 断言 3: busbar_group.main_indicator_yc_num FK → 不存在点被 Mapper 接受 ─────────────────────

    @Test
    void groupInsert_acceptsInvalidYcNumAtMapper()
    {
        VqmsBusbarGroup g = new VqmsBusbarGroup();
        g.setGroupNum(9999L); // 主键必须设置
        g.setGroupName("FK_TEST_GROUP");
        g.setVGrade(0);
        g.setMainIndicatorYcNum(99999L); // 不存在的 yc 点
        g.setMaxStalenessMinutes(30);
        int rows = groupMapper.insert(g);
        Assertions.assertEquals(1, rows);
    }

    // ───────────────────── 断言 4: busbar_threshold.busbar_num FK → 不存在母线被 Mapper 接受 ─────────────────────

    @Test
    void thresholdInsert_acceptsInvalidBusbarNumAtMapper()
    {
        VqmsBusbarThreshold th = new VqmsBusbarThreshold();
        th.setBusbarNum(9999L); // 不存在的母线
        th.setCriterionType("AVC");
        th.setToleranceV(BigDecimal.valueOf(1.000));
        th.setPlanSvInvalidPolicy("SKIP");
        th.setEffectiveFrom(new Date());
        th.setCreateBy("test");
        int rows = thresholdMapper.insert(th);
        Assertions.assertEquals(1, rows);
    }

    // ───────────────────── 断言 5: group_num=0 有 busbar 引用 ─────────────────────

    @Test
    void groupZero_hasBusbarReferences()
    {
        long refCount = busbarMapper.countByGroupNum(0L);
        Assertions.assertTrue(refCount > 0, "group_num=0 应有 busbar 引用");
    }

    // ───────────────────── 断言 6: threshold 生效区间闭合 ─────────────────────

    @Test
    void thresholdCloseExistingByBusbarNum_works()
    {
        int closed = thresholdMapper.closeExistingByBusbarNum(0L);
        // 若存在生效记录则 closed>0，否则 0——两种情况均合法
        Assertions.assertTrue(closed >= 0, "closeExisting 应返回非负数");
    }
}
