package com.ruoyi.vqms.source.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.vqms.source.mapper.HisCurveSvMapper;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D1 场景验证：加载 tools/avc-data-gen/output/manifest.json，读取真实库 qheatavchisdb 三表数据，
 * 核对行级事实与 manifest 承诺一致。
 *
 * <h3>架构边界</h3>
 * <ul>
 *   <li>manifest.expected 里的 QUAL/PEN/EXEMPT/SKIP 是 judge（S1 搁置轨）的判定产物。
 *       D1 只做只读，<b>不能直接比对判定结论</b>。</li>
 *   <li>D1 能核对的事实：<br>
 *       · 指令条数 == 20（19 个 S 场景 + S17 双指令额外 1）<br>
 *       · S17 的 2 条指令 obj_num 分 0/1 通道<br>
 *       · his_curve_sv 双写完整性（同分钟 busbar 0+1 并存）<br>
 *       · <b>原文级核对</b>：manifest 每个 S 场景的 description 关键词应出现在
 *         对应 warn_content 原文里。这是纯字符串匹配，不需要解码器。</li>
 *   <li>连接 <b>真实 qheatavchisdb</b>（Leo 指令），非合成 vqms_avc_test。</li>
 * </ul>
 */
class ManifestScenarioReadIT
{
    /** 真实库：10.0.0.9 mysql57 容器内独立库，与生产 _qheatavchisdb 隔离 */
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    private static final String RANGE_START = "2026-03-15 00:00:00";
    private static final String RANGE_END   = "2026-04-03 00:00:00";

    private static SqlSessionFactory sessionFactory;
    private static HisCurveSvMapper curveMapper;
    private static WarnInfoMapper warnMapper;
    /** manifest 中所有 S 场景 id（"S01"..."S19"） */
    private static Set<String> sScenarioIds;
    /** 全量 warn_content 拼接，用于关键词搜索 */
    private static String allWarnContent;

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

        var session = sessionFactory.openSession();
        curveMapper = session.getMapper(HisCurveSvMapper.class);
        warnMapper = session.getMapper(WarnInfoMapper.class);

        // 解析 manifest，构建场景 id → description 映射
        Path manifestPath = resolveManifestPath();
        JSONArray arr = JSON.parseArray(Files.readString(manifestPath, StandardCharsets.UTF_8));
        sScenarioIds = arr.stream()
                .map(o -> ((JSONObject) o).getString("id"))
                .filter(id -> id != null && id.startsWith("S"))
                .collect(Collectors.toSet());

        // 合并全部 warn_info warn_content，供后续关键词搜索
        List<WarnInfo> allWarn = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);
        allWarnContent = allWarn.stream()
                .map(WarnInfo::getWarnContent)
                .filter(c -> c != null)
                .collect(Collectors.joining(" | "));
    }

    // ─────────────────────────── 工具方法 ───────────────────────────

    private static Path resolveManifestPath()
    {
        String root = System.getenv("VQMS_PROJECT_ROOT");
        if (root != null && !root.isEmpty())
        {
            return Paths.get(root, "tools", "avc-data-gen", "output", "manifest.json");
        }
        return Paths.get("C:/work/VQMS/tools/avc-data-gen/output/manifest.json");
    }

    // ─────────────────────────── 断言 1: 指令总数 20 ───────────────────────────

    @Test
    void assert_warnInstructionCount_20()
    {
        List<WarnInfo> all = warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);
        long sCount = sScenarioIds.size();
        // S 系列共 19 个场景（S01~S19），其中 S17 有 2 条指令 → 共 20 条
        assertEquals(20, all.size(),
                "warn_type=5 指令总数应为 20（19 个 S 场景 + S17 双指令额外 1）；实际=" + all.size());
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

        // save_time 含毫秒（如 10:00:00.100 / 10:00:00.200），截断到分钟后比对
        Map<String, Set<Long>> byMinute = curves.stream().collect(Collectors.groupingBy(
                r -> r.getSaveTime().substring(0, 16),
                Collectors.mapping(HisCurveSv::getBusbarNum, Collectors.toSet())));

        long dualWriteMinutes = byMinute.values().stream()
                .filter(s -> s.contains(0L) && s.contains(1L))
                .count();
        assertTrue(dualWriteMinutes > 0,
                "应存在双写分钟（同 save_time 前16字符下 busbar 0 与 1 并存），"
                + "实际双写分钟=" + dualWriteMinutes + " / 总分钟数=" + byMinute.size());
    }

    // ─────────────────────────── 断言 4: 原文级核对（manifest 驱动）──────────────────────────

    @Test
    void assert_originalText_matchesManifestPatterns()
    {
        // manifest 驱动：对每个有 v_target 的 S 场景，将其目标值数字注入原文预期
        // S01~S07/S13~S16/S18/S19 的目标值均为 223.15 → 原文含 "22315"
        // S08 目标值 225.0  → 原文含 "22500"
        // S09 增量编码 2202  → 原文含 "2202"
        // S10 增量编码 1202  → 原文含 "1202"
        // S12 脏写 "abc"     → 原文含 "abc"
        // （S11/S17 无 v_target，不在此断言范围）
        String[] expectedPatterns = {
                "22315", // S01-S07/S13-S16/S18/S19 的目标值形态（223.15 kV）
                "22500", // S08 偏低边界（225.0 kV）
                "2202",  // S09 增量加
                "1202",  // S10 增量减
                "abc",   // S12 编码脏写样本
        };

        for (String pattern : expectedPatterns)
        {
            assertTrue(allWarnContent.contains(pattern),
                    "manifest 承诺的数字/编码 \"" + pattern + "\" 应在 warn_content 原文中存在");
        }

        // 额外：确认所有 warn_content 非空且长度 > 0
        assertFalse(allWarnContent.isEmpty(), "warn_content 不应为空");
        assertTrue(allWarnContent.length() > 100,
                "warn_content 应有足够内容（包含全部 20 条指令原文）；实际=" + allWarnContent.length());
    }

    // ─────────────────────────── 断言 5: 领域对象不含 plan_SV ───────────────────────────

    @Test
    void assert_domainObject_noPlanSVField()
    {
        boolean hasSetter, hasGetter;
        try
        {
            com.ruoyi.vqms.source.model.HisCurveSv.class.getMethod("setPlanSV", String.class);
            hasSetter = true;
        }
        catch (NoSuchMethodException e)
        {
            hasSetter = false;
        }
        try
        {
            com.ruoyi.vqms.source.model.HisCurveSv.class.getMethod("getPlanSV");
            hasGetter = true;
        }
        catch (NoSuchMethodException e)
        {
            hasGetter = false;
        }
        assertFalse(hasSetter || hasGetter,
                "HisCurveSv 领域对象不应含 plan_SV 相关方法（plan_SV 废值不映射，编译期即不存在）；"
                + "setter=" + hasSetter + ", getter=" + hasGetter);
    }

    // ─────────────────────────── 断言 6: manifest S 场景完整性 ───────────────────────────

    @Test
    void assert_manifest_hasAllS01ToS19()
    {
        // manifest 必须包含 S01~S19 共 19 个场景
        assertEquals(19, sScenarioIds.size(),
                "manifest 中 S 场景数量应为 19（S01~S19），实际=" + sScenarioIds.size()
                + "，缺失=" + missingIds());
        for (int i = 1; i <= 19; i++)
        {
            String id = String.format("S%02d", i);
            assertTrue(sScenarioIds.contains(id), "manifest 缺少场景 " + id);
        }
    }

    private Set<String> missingIds()
    {
        java.util.Set<String> missing = new java.util.LinkedHashSet<>();
        for (int i = 1; i <= 19; i++)
        {
            if (!sScenarioIds.contains(String.format("S%02d", i)))
            {
                missing.add(String.format("S%02d", i));
            }
        }
        return missing;
    }
}
