package com.ruoyi.vqms.source.reader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.vqms.source.mapper.HisCurveSvMapper;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D1 场景验证：加载 tools/avc-data-gen 的 manifest.json，核对 D1 读出的行数据与生成器承诺一致
 * （而非只断言「读到非空行」）。
 *
 * <p>注意 D1 是<b>只读层</b>，不判定——manifest 里的 QUAL/PEN/EXEMPT/SKIP 是判定结论（搁置轨 S1 judge 的事，
 * 见测试方案 §4.2 L2 契约测试）。D1 能核对的「行级事实」是：</p>
 * <ol>
 *   <li>warn_type=5 指令条数 == S 场景数(19) + S17 双指令额外 1 = 20；</li>
 *   <li>S17 恰两条指令、obj_num 分 0/1 两通道；</li>
 *   <li>his_curve_sv 双写完整性：同分钟 busbar 0 与 1 并存；</li>
 *   <li>关键场景话 文（S12 脏文本/S09 增量编码）原文字段无丢列。</li>
 * </ol>
 */
class ManifestScenarioReadIT
{
    /** 合成库全量时间范围（生成器 base_date=2026-03-15 至 2026-04-02） */
    private static final String RANGE_START = "2026-03-15 00:00:00";
    private static final String RANGE_END = "2026-04-03 00:00:00";

    private static SqlSessionFactory sessionFactory;
    private static List<JSONObject> manifest;

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
        ds.setUsername(System.getenv().getOrDefault("VQMS_AVC_TEST_USER", "root"));
        ds.setPassword(System.getenv().getOrDefault("VQMS_AVC_TEST_PASSWORD", "syth7777"));

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        sessionFactory = factoryBean.getObject();

        Path manifestPath = Paths.get("../tools/avc-data-gen/output/manifest.json");
        if (!Files.exists(manifestPath))
        {
            manifestPath = Paths.get("tools/avc-data-gen/output/manifest.json");
        }
        manifest = JSON.parseArray(Files.readString(manifestPath), JSONObject.class);
    }

    @Test
    void warnInstructionCount_equalsRegulationScenariosPlusS17Extra()
    {
        WarnInfoMapper wm = mapper(WarnInfoMapper.class);
        List<WarnInfo> instructions = wm.selectByRangeAndType(RANGE_START, RANGE_END, 5L);

        long regScenarios = manifest.stream()
                .filter(o -> o.getString("id").startsWith("S"))
                .count();

        assertEquals(regScenarios + 1, instructions.size(),
                "warn_type=5 条数应 = S 场景数(" + regScenarios + ") + S17 双指令额外 1");
        assertTrue(instructions.stream().allMatch(w -> w.getWarnType() == 5L));
    }

    @Test
    void s17_twoInstructions_splitByObjNum()
    {
        WarnInfoMapper wm = mapper(WarnInfoMapper.class);
        List<WarnInfo> all = wm.selectByRangeAndType(RANGE_START, RANGE_END, 5L);

        // S17 是唯一双指令场景：obj_num=0 夹住 + obj_num=1 不夹，两条不同 obj_num
        Set<Long> objNums = all.stream().map(WarnInfo::getObjNum).collect(Collectors.toSet());
        assertTrue(objNums.contains(0L), "S17 应有 obj_num=0 通道指令");
        assertTrue(objNums.contains(1L), "S17 应有 obj_num=1 通道指令");
        assertTrue(objNums.size() >= 2, "至少两个不同 obj_num 分通道");
    }

    @Test
    void curve_dualWrite_busbar0And1()
    {
        HisCurveSvMapper cm = mapper(HisCurveSvMapper.class);
        List<HisCurveSv> curves = cm.selectByRange(RANGE_START, RANGE_END);

        Map<String, Set<Long>> byMinute = curves.stream().collect(Collectors.groupingBy(
                HisCurveSv::getSaveTime,
                Collectors.mapping(HisCurveSv::getBusbarNum, Collectors.toSet())));

        long dualWrite = byMinute.values().stream()
                .filter(s -> s.contains(0L) && s.contains(1L))
                .count();
        assertTrue(dualWrite > 0, "存在双写分钟（同 save_time 下 busbar 0 与 1 并存）");
    }

    @Test
    void warnContent_preservesRawInstructionText()
    {
        WarnInfoMapper wm = mapper(WarnInfoMapper.class);
        List<WarnInfo> all = wm.selectByRangeAndType(RANGE_START, RANGE_END, 5L);

        // S12 脏文本原文字段完整读回（",abc."），不外泄解码、不丢列
        boolean hasDirtyText = all.stream().anyMatch(w -> w.getWarnContent() != null
                && w.getWarnContent().contains("abc"));
        assertTrue(hasDirtyText, "S12 编码脏写原文应被完整读回，无丢列");

        // S09 增量编码原文完整读回
        boolean hasIncrement = all.stream().anyMatch(w -> w.getWarnContent() != null
                && w.getWarnContent().contains("2202"));
        assertTrue(hasIncrement, "S09 增量编码 2202 原文应被完整读回");
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(Class<T> type)
    {
        return sessionFactory.getConfiguration().getMapper(type, sessionFactory.openSession());
    }
}
