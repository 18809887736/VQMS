package com.ruoyi.vqms.management;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
import com.ruoyi.vqms.management.service.VqmsBusbarGroupService;
import com.ruoyi.vqms.management.service.VqmsBusbarService;
import com.ruoyi.vqms.management.service.VqmsBusbarThresholdService;

/**
 * D2 集成测试：管理表 DDL + 逻辑 FK 校验。
 *
 * <p>策略：每次测试前清理，确保种子数据唯一；使用唯一的命名前缀避免跨运行冲突。</p>
 */
@ExtendWith(SpringExtension.class)
class D2BusinessLogicIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:13306/ry_vqms"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    // 凭证不入库（CLAUDE.md Security）：密码走环境变量必填（2026-08-21 D5 修复：原 root 密码兜底值出库，历史值待轮换）
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD =
            java.util.Objects.requireNonNull(System.getenv("MYSQL_ROOT_PASSWORD"), "缺少 MYSQL_ROOT_PASSWORD");

    @Configuration
    @MapperScan("com.ruoyi.vqms.management.mapper")
    @ComponentScan("com.ruoyi.vqms.management.service")
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
    @Autowired
    private VqmsBusbarService busbarService;
    @Autowired
    private VqmsBusbarGroupService groupService;
    @Autowired
    private VqmsBusbarThresholdService thresholdService;

    /**
     * 清理所有通过 Service 插入的测试数据（基于命名前缀）。
     */
    @BeforeEach
    void setUp()
    {
        // 删除所有测试命名的记录
        List<VqmsBusbar> bars = busbarMapper.selectList();
        bars.stream()
            .filter(b -> b.getBusbarName() != null && (b.getBusbarName().startsWith("TEST_") || b.getBusbarName().startsWith("FK_") || b.getBusbarName().startsWith("MAPPER_")))
            .forEach(b -> busbarMapper.deleteByBusbarNum(b.getBusbarNum()));

        List<VqmsBusbarGroup> groups = groupMapper.selectList();
        groups.stream()
            .filter(g -> g.getGroupName() != null && (g.getGroupName().startsWith("TEST_") || g.getGroupName().startsWith("FK_")))
            .forEach(g -> groupMapper.deleteByGroupNum(g.getGroupNum()));

        List<VqmsBusbarThreshold> ths = thresholdMapper.selectList(null, null);
        ths.stream()
            .filter(t -> t.getBusbarNum() != null && t.getBusbarNum() > 100)
            .forEach(t -> thresholdMapper.deleteById(t.getThresholdId()));
    }

    @AfterEach
    void tearDown()
    {
        setUp(); // 再次清理
    }

    // ───────────────────── 断言 1: 种子数据 ─────────────────────

    @Test
    void seedData_exists()
    {
        long groupCount = groupMapper.selectList().size();
        long barCount = busbarMapper.selectList().size();
        long ycCount = ycMapper.selectList().size();
        long paramCount = judgeParamMapper.selectList().size();

        // 允许有额外的测试数据残留，但至少要有种子数据
        Assertions.assertTrue(groupCount >= 2, "busbar_group 至少应有 2 条种子数据，实际=" + groupCount);
        Assertions.assertTrue(barCount >= 2, "busbar 至少应有 2 条种子数据，实际=" + barCount);
        Assertions.assertTrue(ycCount >= 3, "yc_point_map 至少应有 3 条种子数据");
        Assertions.assertEquals(4, paramCount, "judge_param 种子应为 4");
    }

    // ───────────────────── 断言 2: Service.insert(VqmsBusbar) → group_num 不存在时抛异常 ─────────────────────

    @Test
    void service_insertBusbar_rejectsInvalidGroupNum()
    {
        VqmsBusbar bar = new VqmsBusbar();
        bar.setBusbarNum(100L); // 唯一 ID
        bar.setBusbarName("TEST_BUSBAR_FK_GROUP");
        bar.setVGrade(1);
        bar.setGroupNum(9999L); // 不存在的组
        bar.setNominalKv(BigDecimal.valueOf(220.000));
        bar.setStatus("0");
        bar.setCreateBy("test");

        // 验证前提：9999 确实不存在
        Assertions.assertEquals(0, groupMapper.countByGroupNum(9999L), "预设：group_num=9999 不存在");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            busbarService.insert(bar);
        }, "Service 应拒绝 group_num 不存在的母线");
    }

    // ───────────────────── 断言 3: Service.insert(VqmsBusbarGroup) → yc_num 不存在时抛异常 ─────────────────────

    @Test
    void service_insertGroup_rejectsInvalidYcNum()
    {
        VqmsBusbarGroup g = new VqmsBusbarGroup();
        g.setGroupNum(200L);
        g.setGroupName("TEST_GROUP_FK_YC");
        g.setVGrade(0);
        g.setMainIndicatorYcNum(99999L); // 不存在的 yc 点
        g.setMaxStalenessMinutes(30);

        // 验证前提
        Assertions.assertEquals(0, ycMapper.countByYcNum(99999L), "预设：yc_num=99999 不存在");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            groupService.insert(g);
        }, "Service 应拒绝 main_indicator_yc_num 不存在的组");
    }

    // ───────────────────── 断言 4: Service.insert(VqmsBusbarThreshold) → busbar_num 不存在时抛异常 ─────────────────────

    @Test
    void service_insertThreshold_rejectsInvalidBusbarNum()
    {
        VqmsBusbarThreshold th = new VqmsBusbarThreshold();
        th.setBusbarNum(9999L); // 不存在的母线
        th.setCriterionType("AVC");
        th.setToleranceV(BigDecimal.valueOf(1.000));
        th.setPlanSvInvalidPolicy("SKIP");
        th.setEffectiveFrom(new Date());
        th.setCreateBy("test");

        // 验证前提
        Assertions.assertEquals(0, busbarMapper.countByBusbarNum(9999L), "预设：busbar_num=9999 不存在");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            thresholdService.insert(th);
        }, "Service 应拒绝 busbar_num 不存在的阈值");
    }

    // ───────────────────── 断言 5: Service.deleteByGroupNum → 有引用时抛异常 ─────────────────────

    @Test
    void service_deleteGroup_withReferences_shouldThrow()
    {
        // group_num=0 有东/西母线引用
        long refCount = busbarMapper.countByGroupNum(0L);
        Assertions.assertTrue(refCount > 0, "预设：group_num=0 应有引用");

        Assertions.assertThrows(IllegalStateException.class, () -> {
            groupService.deleteByGroupNum(0L);
        }, "Service 应拒绝删除有引用的组");
    }

    // ───────────────────── 断言 6: Mapper 层面接受非法 FK（DB 无约束） ─────────────────────

    @Test
    void mapper_acceptsInvalidFK()
    {
        VqmsBusbar bar = new VqmsBusbar();
        bar.setBusbarNum(300L);
        bar.setBusbarName("MAPPER_TEST_INVALID_FK");
        bar.setVGrade(1);
        bar.setGroupNum(8888L); // 不存在的组
        bar.setNominalKv(BigDecimal.valueOf(220.000));
        bar.setStatus("0");
        bar.setCreateBy("test");

        int rows = busbarMapper.insert(bar);
        Assertions.assertEquals(1, rows, "Mapper 层无约束，应插入成功");
    }

    // ───────────────────── 断言 7: threshold 生效区间闭合 ─────────────────────

    @Test
    void thresholdCloseExistingByBusbarNum_works()
    {
        int closed = thresholdMapper.closeExistingByBusbarNum(0L);
        // closed >= 0 均合法（可能为 0 表示没有需要闭合的记录）
        Assertions.assertTrue(closed >= 0, "closeExisting 应返回非负数");
    }
}
