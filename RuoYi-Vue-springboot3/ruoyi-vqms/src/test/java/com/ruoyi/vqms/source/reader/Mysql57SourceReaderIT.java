package com.ruoyi.vqms.source.reader;

import java.util.List;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.ruoyi.vqms.source.mapper.HisCurveSvMapper;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.mapper.YcHistoryMapper;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.model.YcHistory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D1 source 只读层集成测试：直连 10.0.0.9 真实库 qheatavchisdb，读通三表。
 *
 * <p>直接用手工 mybatis SqlSessionFactory 调 mapper，验证 SQL + 领域映射（不绕经 @DataSource(SLAVE) 路由——
 * 该路由是 RuoYi 既有机制、与本模块实现无关）。</p>
 */
class Mysql57SourceReaderIT
{
    private static HisCurveSvMapper curveMapper;
    private static WarnInfoMapper warnMapper;
    private static YcHistoryMapper ycMapper;

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
        // 测试库凭证不入库（CLAUDE.md Security）：环境变量必填
        String user = System.getenv("VQMS_AVC_USER");
        String password = System.getenv("VQMS_AVC_PASSWORD");
        assertNotNull(user, "缺少环境变量 VQMS_AVC_USER");
        assertNotNull(password, "缺少环境变量 VQMS_AVC_PASSWORD");
        ds.setUsername(user);
        ds.setPassword(password);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory sessionFactory = factoryBean.getObject();

        curveMapper = sessionFactory.getConfiguration().getMapper(HisCurveSvMapper.class, sessionFactory.openSession());
        warnMapper = sessionFactory.getConfiguration().getMapper(WarnInfoMapper.class, sessionFactory.openSession());
        ycMapper = sessionFactory.getConfiguration().getMapper(YcHistoryMapper.class, sessionFactory.openSession());
    }

    @Test
    void readCurve_returnsRows_withoutWasteFields()
    {
        List<HisCurveSv> rows = curveMapper.selectByRange("2026-03-15 09:57:00", "2026-03-15 10:03:00");
        assertFalse(rows.isEmpty(), "his_curve_sv 应读到行");
        // 废值不映射：领域对象无 average_SV / plan_SV 字段，编译期即不存在
        for (HisCurveSv r : rows)
        {
            assertNotNull(r.getSaveTime());
            assertNotNull(r.getBusbarNum());
            assertNotNull(r.getHighSV());
            assertNotNull(r.getLowSV());
        }
    }

    @Test
    void readCurve_filtersByBusbar()
    {
        List<HisCurveSv> rows = curveMapper.selectByRangeAndBusbar(
                "2026-03-15 09:57:00", "2026-03-15 10:03:00", 0L);
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> r.getBusbarNum() == 0L), "应只含指定母线");
    }

    @Test
    void readWarn_returnsInstructionRows()
    {
        List<WarnInfo> rows = warnMapper.selectByRangeAndType(
                "2026-03-15 00:00:00", "2026-04-03 00:00:00", 5L);
        assertFalse(rows.isEmpty(), "warn_info 应读到 warn_type=5 指令");
        assertTrue(rows.stream().allMatch(r -> r.getWarnType() == 5L));
    }

    @Test
    void readYc_returnsRows()
    {
        List<YcHistory> rows = ycMapper.selectByRange("2026-03-15 00:00:00", "2026-03-16 00:00:00");
        // 合成库 yc_history 由 U 系列场景填充；断言至少读到数据（含非法时间会被 SaveTimeFilter/上层跳过）
        assertFalse(rows.isEmpty(), "yc_history 应读到行");
    }
}
