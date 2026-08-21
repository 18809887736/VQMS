package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
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

/**
 * D4 三步闸门场景验证（L1，测试方案 §4.4「正例零误杀」）：真实库 qheatavchisdb，
 * 闸门组合（expandBounds → mapper 粗筛 → filter 精滤）与 manifest / 直读条数核对。
 */
class D4GateScenarioIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    private static final String RANGE_START = "2026-03-15 00:00:00";

    private static final String RANGE_END = "2026-04-03 00:00:00";

    private static HisCurveSvMapper curveMapper;

    private static WarnInfoMapper warnMapper;

    private static YcHistoryMapper ycMapper;

    /** 测试库凭证不入库（CLAUDE.md Security）：环境变量必填 */
    private static String requiredEnv(String name)
    {
        String value = System.getenv(name);
        if (value == null || value.isBlank())
        {
            throw new IllegalStateException("缺少环境变量 " + name + "（测试库凭证走环境变量，不入库）");
        }
        return value;
    }

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername(requiredEnv("VQMS_AVC_TEST_USER"));
        ds.setPassword(requiredEnv("VQMS_AVC_TEST_PASSWORD"));

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory sessionFactory = factoryBean.getObject();
        curveMapper = sessionFactory.getConfiguration().getMapper(HisCurveSvMapper.class, sessionFactory.openSession());
        warnMapper = sessionFactory.getConfiguration().getMapper(WarnInfoMapper.class, sessionFactory.openSession());
        ycMapper = sessionFactory.getConfiguration().getMapper(YcHistoryMapper.class, sessionFactory.openSession());
    }

    /** 指令条数经闸门仍 == 20（manifest 承诺，D1 已核对的同一事实）——闸门不丢正例 */
    @Test
    void warn_20条指令全量过闸门_条数不丢()
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(RANGE_START, RANGE_END);
        List<WarnInfo> coarse = warnMapper.selectByRangeAndType(bounds.start(), bounds.end(), 5L);
        List<WarnInfo> gated = SaveTimeGate.filter(coarse, WarnInfo::getWarnTime, RANGE_START, RANGE_END);
        Assertions.assertEquals(20, gated.size(), "闸门后指令条数应仍为 manifest 承诺的 20");
        Assertions.assertTrue(coarse.size() >= gated.size(), "粗筛读到的行数不少于精滤后");
    }

    /** 双写双母线全量：闸门不丢直读的任何行（零误杀），且闸门增留的行必为取整后归属区间的边界行 */
    @Test
    void curve_闸门零误杀_不丢行且增留行必属区间()
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(RANGE_START, RANGE_END);
        LocalDateTime windowStart = LocalDateTime.parse("2026-03-15T00:00:00");
        LocalDateTime windowEnd = LocalDateTime.parse("2026-04-03T00:00:00");
        for (long busbar : new long[] { 0L, 1L })
        {
            List<HisCurveSv> coarse = curveMapper.selectByRangeAndBusbar(bounds.start(), bounds.end(), busbar);
            List<HisCurveSv> gated = SaveTimeGate.filter(coarse, HisCurveSv::getSaveTime, RANGE_START, RANGE_END);
            List<HisCurveSv> plain = curveMapper.selectByRangeAndBusbar(RANGE_START, RANGE_END, busbar);

            java.util.Set<String> gatedTimes = gated.stream().map(HisCurveSv::getSaveTime)
                    .collect(java.util.stream.Collectors.toSet());
            plain.forEach(r -> Assertions.assertTrue(gatedTimes.contains(r.getSaveTime()),
                    "busbar=" + busbar + " 直读行被闸门误杀: " + r.getSaveTime()));

            gated.forEach(r -> {
                LocalDateTime rounded = MinuteRounder.round(MinuteRounder.parse(r.getSaveTime()));
                Assertions.assertFalse(rounded.isBefore(windowStart) || rounded.isAfter(windowEnd),
                        "busbar=" + busbar + " 闸门增留行取整后不在目标区间: " + r.getSaveTime());
            });
            Assertions.assertTrue(gated.size() >= plain.size());
        }
    }

    /** 单分钟窗口：精滤后每行取整恰为该分钟（边界秒行如 09:59:57.100 归入 10:00；区间外剔除） */
    @Test
    void curve_单分钟窗口_精滤语义精确()
    {
        String minute = "2026-03-15 10:00:00";
        LocalDateTime expected = LocalDateTime.parse("2026-03-15T10:00:00");
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(minute, minute);
        for (long busbar : new long[] { 0L, 1L })
        {
            List<HisCurveSv> coarse = curveMapper.selectByRangeAndBusbar(bounds.start(), bounds.end(), busbar);
            List<HisCurveSv> gated = SaveTimeGate.filter(coarse, HisCurveSv::getSaveTime, minute, minute);
            Assertions.assertFalse(gated.isEmpty(), "busbar=" + busbar + " 单分钟应有行");
            gated.forEach(r -> Assertions.assertEquals(expected,
                    MinuteRounder.round(MinuteRounder.parse(r.getSaveTime())),
                    "精滤后所有行取整应恰为该分钟: " + r.getSaveTime()));
        }
    }

    /** 第三表 yc_history（U01–U07 投运率数据所在）：闸门零误杀、不丢直读行（§4.4 26 场景全覆盖收口） */
    @Test
    void yc_闸门零误杀_不丢直读行()
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(RANGE_START, RANGE_END);
        LocalDateTime windowStart = LocalDateTime.parse("2026-03-15T00:00:00");
        LocalDateTime windowEnd = LocalDateTime.parse("2026-04-03T00:00:00");
        List<YcHistory> coarse = ycMapper.selectByRange(bounds.start(), bounds.end());
        List<YcHistory> gated = SaveTimeGate.filter(coarse, YcHistory::getYcTime, RANGE_START, RANGE_END);
        List<YcHistory> plain = ycMapper.selectByRange(RANGE_START, RANGE_END);
        Assertions.assertFalse(gated.isEmpty(), "yc_history 应读到行");
        java.util.Set<String> gatedTimes = gated.stream().map(YcHistory::getYcTime)
                .collect(java.util.stream.Collectors.toSet());
        plain.forEach(r -> Assertions.assertTrue(gatedTimes.contains(r.getYcTime()),
                "yc_history 直读行被闸门误杀: " + r.getYcTime()));
        gated.forEach(r -> {
            LocalDateTime rounded = MinuteRounder.round(MinuteRounder.parse(r.getYcTime()));
            Assertions.assertFalse(rounded.isBefore(windowStart) || rounded.isAfter(windowEnd),
                    "yc_history 闸门增留行取整后不在目标区间: " + r.getYcTime());
        });
    }
}
