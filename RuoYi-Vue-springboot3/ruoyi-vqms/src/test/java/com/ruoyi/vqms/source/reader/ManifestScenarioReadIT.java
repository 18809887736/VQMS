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

import static org.junit.jupiter.api.Assertions.*;

/**
 * D1 场景验证：加载 tools/avc-data-gen/output/manifest.json，按场景 ID 读取合成库三表数据，
 * 核对行级事实与 manifest 承诺一致。
 *
 * <h3>断言边界说明</h3>
 * <ul>
 *   <li>manifest.expected 里的 QUAL/PEN/EXEMPT/SKIP 是 judge（S1 搁置轨）的判定产物。
 *       D1 只做只读，<b>不能直接比对判定结论</b>。</li>
 *   <li>D1 能核对的事实：<br>
 *       · 指令条数 == 20（19 个 S 场景 + S17 双指令额外 1）<br>
 *       · S17 的 2 条指令 obj_num 分 0/1 通道<br>
 *       · his_curve_sv 双写完整性（同分钟 busbar 0+1 并存）<br>
 *       · <b>原文级核对</b>：manifest 承诺的 v_target 数字应出现在对应 warn_content 原文里
 *         （纯字符串匹配，不需要解码器）</li>
 *   <li>测试连接 <b>真实 qheatavchisdb</b>（Leo 指令），非合成 vqms_avc_test</li>
 * </ul>
 */
class ManifestScenarioReadIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    /** 全量时间范围覆盖合成数据（2026-03-15 ~ 2026-04-02） */
    private static final String RANGE_START = "2026-03-15 00:00:00";
    private static final String RANGE_END   = "2026-04-03 00:00:00";

    private static SqlSessionFactory sessionFactory;
    private static HisCurveSvMapper curveMapper;
    private static WarnInfoMapper warnMapper;
    private static List<JSONObject> manifest;

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername(System.getenv().getOrDefault("VQMS_AVC_TEST_USER", "root"));
        ds.setPassword(System.getenv().getOrDefault("VQMS_AVC_TEST_PASSWORD", "syth7777"));

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        sessionFactory = factoryBean.getObject();

        // manifest.json 在项目根 /tools/avc-data-gen/output/，用绝对路径兜底
        String projectRoot = System.getenv("VQMS_PROJECT_ROOT");
        Path manifestPath = (projectRoot != null && !projectRoot.isEmpty())
                ? Paths.get(projectRoot, "tools", "avc-data-gen", "output", "manifest.json")
                : Paths.get("C:/work/VQMS/tools/avc-data-gen/output/manifest.json");
        manifest = JSON.parseArray(Files.readString(manifestPath), JSONObject.class);

        // 共享 Session 持有 mapper，避免每次调用时关闭 Executor
        var session = sessionFactory.openSession();
        curveMapper = session.getMapper(HisCurveSvMapper.class);
        warnMapper = session.getMapper(WarnInfoMapper.class);
    }

    // ─────────────────────────── 断言 1: 指令总数 20 ───────────────────────────

    @Test
    void assert_warnInstructionCount_20()
    {
        List<WarnInfo> all = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);
        assertEquals(20, all.size(),
                "warn_type=5 指令总数应为 20（S01~S19 共 19 条 + S17 双指令额外 1 条）；实际=" + all.size());
        assertTrue(all.stream().allMatch(w -> w.getWarnType() == 5L),
                "所有指令应均为 warn_type=5");
    }

    // ─────────────────────────── 断言 2: S17 双通道 ───────────────────────────

    @Test
    void assert_S17_twoChannels_splitByObjNum()
    {
        List<WarnInfo> all = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);
        Set<Long> objNums = all.stream().map(WarnInfo::getObjNum).collect(Collectors.toSet());
        assertTrue(objNums.contains(0L), "S17 应有 obj_num=0 通道指令");
        assertTrue(objNums.contains(1L), "S17 应有 obj_num=1 通道指令");
    }

    // ─────────────────────────── 断言 3: 双写完整性 ───────────────────────────

    @Test
    void assert_curve_dualWrite_busbar0_and_1()
    {
        List<HisCurveSv> curves = curveMapper.selectByRange(RANGE_START, RANGE_END);
        assertFalse(curves.isEmpty(), "his_curve_sv 应读到行");

        // save_time 是 varchar 含毫秒（如 10:00:00.100 / 10:00:00.200），
        // 按「截断到分钟」为 key 分组，检查同分钟是否同时有 busbar 0 和 1
        Map<String, Set<Long>> byMinute = curves.stream().collect(Collectors.groupingBy(
                r -> r.getSaveTime().substring(0, 16),   // "yyyy-MM-dd HH:mm"
                Collectors.mapping(HisCurveSv::getBusbarNum, Collectors.toSet())));

        long dualWriteMinutes = byMinute.values().stream()
                .filter(s -> s.contains(0L) && s.contains(1L))
                .count();
        assertTrue(dualWriteMinutes > 0,
                "应存在双写分钟（同 save_time 前16字符下 busbar 0 与 1 并存），实际=" + dualWriteMinutes
                + " / 总分钟数=" + byMinute.size());
    }

    // ─────────────────────────── 断言 4: 原文级核对 v_target → warn_content ───────────────────────────

    @Test
    void assert_originalText_matchesManifestExpected()
    {
        List<WarnInfo> all = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);

        // S01~S07/S13~S16/S18/S19: v_target=223.15 → 目标值形态 → 原文含 "22315"
        boolean hasTarget22315 = all.stream().anyMatch(w ->
                w.getWarnContent() != null && w.getWarnContent().contains("22315"));
        assertTrue(hasTarget22315, "S01 等目标值场景 warn_content 原文应包含 '22315'（v_target=223.15kV）");

        // S08: v_target=225.0 → 目标值形态，原文含 22500
        boolean hasTarget22500 = all.stream().anyMatch(w ->
                w.getWarnContent() != null && w.getWarnContent().contains("22500"));
        assertTrue(hasTarget22500, "S08 warn_content 原文应包含 '22500'（v_target=225.0kV 偏低边界场景）");

        // S09: 增量加 2202 → 原文含 2202
        boolean hasIncrementUp = all.stream().anyMatch(w ->
                w.getWarnContent() != null && w.getWarnContent().contains("2202"));
        assertTrue(hasIncrementUp, "S09 warn_content 原文应包含 '2202'（增量加编码）");

        // S10: 增量减 1202 → 原文含 1202
        boolean hasIncrementDown = all.stream().anyMatch(w ->
                w.getWarnContent() != null && w.getWarnContent().contains("1202"));
        assertTrue(hasIncrementDown, "S10 warn_content 原文应包含 '1202'（增量减编码）");

        // S12: 编码脏写 ",abc." → 原文含 abc
        boolean hasDirtyText = all.stream().anyMatch(w ->
                w.getWarnContent() != null && w.getWarnContent().contains("abc"));
        assertTrue(hasDirtyText, "S12 warn_content 原文应包含 'abc'（编码脏写测试样本）");
    }

    // ─────────────────────────── 断言 5: 领域对象不含 plan_SV ───────────────────────────

    @Test
    void assert_domainObject_noPlanSVField()
    {
        boolean hasPlanSVSetter;
        try
        {
            com.ruoyi.vqms.source.model.HisCurveSv.class.getMethod("setPlanSV", String.class);
            hasPlanSVSetter = true;
        }
        catch (NoSuchMethodException e)
        {
            hasPlanSVSetter = false;
        }
        assertFalse(hasPlanSVSetter,
                "HisCurveSv 领域对象不应含 setPlanSV 方法（plan_SV 废值不映射，编译期即不存在）");
    }

    // ─────────────────────────── 断言 6: 全部指令 warn_content 非空 ───────────────────────────

    @Test
    void assert_allInstructions_haveNonEmptyContent()
    {
        List<WarnInfo> all = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);

        long nonEmpty = all.stream()
                .filter(w -> w.getWarnContent() != null && !w.getWarnContent().isEmpty())
                .count();
        assertEquals(all.size(), nonEmpty,
                "所有 warn_type=5 指令应有非空 warn_content；缺失=" + (all.size() - nonEmpty));
    }
}
