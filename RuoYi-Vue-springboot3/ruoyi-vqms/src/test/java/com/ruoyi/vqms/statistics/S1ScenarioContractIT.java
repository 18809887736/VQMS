package com.ruoyi.vqms.statistics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.vqms.source.mapper.HisCurveSvMapper;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.mapper.YcHistoryMapper;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.model.YcHistory;
import com.ruoyi.vqms.source.reader.MinuteRounder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景契约 IT（L1，外部源直连）· S1 全量 oracle 比对（测试方案 §4.2）。
 *
 * <h3>断言口径</h3>
 * manifest.expected 经 §4.2 映射表逐场景断言 {@link DefaultRegulationJudge}：
 * QUAL→QUALIFIED；PEN→PENALIZED 且免考层以真实 yc501=0 应用后仍计罚；
 * EXEMPT→judge 层 PENALIZED + 免考层真实 yc501=1 应用得 EXEMPTED（两段式，不直接比对三态期望）；
 * SKIP 按原因分流（S11/S12 → Undecodable 归因；S14/S16 → invalidTiers 承载）。
 * <b>契约准入门槛 = 19 调节场景全量通过</b>（测试方案 §5.0）。
 *
 * <h3>参数口径</h3>
 * {@link JudgeParams} 取<b>生产锁定参数 (t_fast=4, t_econ=5)</b>，与 D7 `vqms_judge_param`
 * 种子/CHECK 一致；生成器窗口布局已于 2026-08-22 对齐（D9 报告 §八）。
 */
class S1ScenarioContractIT
{
    /** 生产锁定参数（D7 vqms_judge_param 种子：t_fast=4 可整定默认 / t_econ=5 写死） */
    private static final JudgeParams PRODUCTION_PARAMS = new JudgeParams(4, 5);

    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    private static final String RANGE_START = "2026-03-15 00:00:00";
    private static final String RANGE_END = "2026-04-03 00:00:00";

    /** manifest 场景日锚点（thresholds.yaml base_date），S01=第 0 天、逐序号 +1 天 */
    private static final LocalDateTime BASE_DATE = LocalDateTime.parse("2026-03-15T00:00:00");

    private static final int REALTIME_YC_MAIN = 4002;
    private static final int EXEMPT_FLAG_YC = 501;

    private static WarnInfoMapper warnMapper;
    private static HisCurveSvMapper curveMapper;
    private static YcHistoryMapper ycMapper;
    private static JSONArray manifest;

    /** 真实现档：全量 oracle 断言生效（Stub 档形状断言由 StubDecodeClassificationTest 等承担） */
    private final DefaultRegulationJudge judge = new DefaultRegulationJudge();

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(DB_URL);
        String user = System.getenv("VQMS_AVC_TEST_USER");
        String password = System.getenv("VQMS_AVC_TEST_PASSWORD");
        assertNotNull(user, "缺少环境变量 VQMS_AVC_TEST_USER");
        assertNotNull(password, "缺少环境变量 VQMS_AVC_TEST_PASSWORD");
        ds.setUsername(user);
        ds.setPassword(password);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory sessionFactory = factoryBean.getObject();
        warnMapper = sessionFactory.getConfiguration().getMapper(WarnInfoMapper.class, sessionFactory.openSession());
        curveMapper = sessionFactory.getConfiguration().getMapper(HisCurveSvMapper.class, sessionFactory.openSession());
        ycMapper = sessionFactory.getConfiguration().getMapper(YcHistoryMapper.class, sessionFactory.openSession());

        manifest = JSON.parseArray(Files.readString(manifestPath(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(26, manifest.size(), "manifest 应含 26 场景");
    }

    private static Path manifestPath()
    {
        String root = System.getenv("VQMS_REPO_ROOT");
        if (root != null && !root.isEmpty())
        {
            return Paths.get(root, "tools", "avc-data-gen", "output", "manifest.json");
        }
        return Paths.get("C:/work/VQMS/tools/avc-data-gen/output/manifest.json");
    }

    // ────────────────── 输入组装 ──────────────────

    private List<WarnInfo> allCommands()
    {
        return warnMapper.selectByRangeAndType(RANGE_START, RANGE_END, 5L);
    }

    /** 场景 id → 当天日期（base_date + 序号偏移，cli.py 同款） */
    private Map<String, String> scenarioDay()
    {
        Map<String, String> dayById = new HashMap<>();
        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD);
        for (int i = 0; i < manifest.size(); i++)
        {
            dayById.put(manifest.getJSONObject(i).getString("id"),
                    BASE_DATE.plusDays(i).format(dateOnly));
        }
        return dayById;
    }

    /** 免考旗 yc501 全区间阶跃行（时间升序），heldAt 取 ≤ 当时刻最近一条 */
    private List<YcHistory> exemptFlagRows;

    /** 单条指令 → 判定输入（t₀=就近取整；realtime=t₀ 时刻 yc4002 值；曲线=主母线偏移 1..t_econ）。
     *  曲线/遥测按<b>取整后分钟</b>索引（对抗验证吸收：日期前缀桶在午夜取整跨界时漏读次日行）。 */
    private JudgeInput toInput(WarnInfo cmd, Map<LocalDateTime, List<HisCurveSv>> curvesByMinute,
                               Map<LocalDateTime, List<YcHistory>> ycByMinute)
    {
        LocalDateTime t0 = MinuteRounder.round(MinuteRounder.parse(cmd.getWarnTime()));
        Double realtime = null;
        for (YcHistory yc : ycByMinute.getOrDefault(t0, List.of()))
        {
            if (yc.getYcNum() != null && yc.getYcNum() == REALTIME_YC_MAIN)
            {
                realtime = yc.getYcData();
                break;
            }
        }
        List<MinuteCurve> curves = new ArrayList<>();
        for (int offset = 1; offset <= PRODUCTION_PARAMS.tEcon(); offset++)
        {
            for (HisCurveSv row : curvesByMinute.getOrDefault(t0.plusMinutes(offset), List.of()))
            {
                if (row.getBusbarNum() != null && row.getBusbarNum() == 0L)
                { // 主母线 bn=0（生成器约定：bn0 主 / bn1 副）；同分钟多行取首行
                    curves.add(new MinuteCurve(offset, row.getHighSV().intValue(), row.getLowSV().intValue()));
                    break;
                }
            }
        }
        return new JudgeInput(new AvcCommand(cmd.getWarnTime(), cmd.getMillisecond(),
                cmd.getObjNum(), cmd.getWarnContent(), realtime),
                curves, t0, t0.toLocalDate().toString());
    }

    private record JudgeInput(AvcCommand cmd, List<MinuteCurve> curves, LocalDateTime t0, String scenarioDate)
    {
    }

    /** 全部指令的判定输入（一次性预取，按取整后分钟索引，避免逐指令查库） */
    private List<JudgeInput> allInputs(List<WarnInfo> commands)
    {
        Map<LocalDateTime, List<HisCurveSv>> curvesByMinute = new HashMap<>();
        for (HisCurveSv row : curveMapper.selectByRange(RANGE_START, RANGE_END))
        {
            curvesByMinute.computeIfAbsent(
                    MinuteRounder.round(MinuteRounder.parse(row.getSaveTime())), k -> new ArrayList<>()).add(row);
        }
        Map<LocalDateTime, List<YcHistory>> ycByMinute = new HashMap<>();
        exemptFlagRows = new ArrayList<>();
        for (YcHistory row : ycMapper.selectByRange(RANGE_START, RANGE_END))
        {
            ycByMinute.computeIfAbsent(
                    MinuteRounder.round(MinuteRounder.parse(row.getYcTime())), k -> new ArrayList<>()).add(row);
            if (row.getYcNum() != null && row.getYcNum() == EXEMPT_FLAG_YC)
            {
                exemptFlagRows.add(row);
            }
        }
        exemptFlagRows.sort(java.util.Comparator.comparing(
                r -> MinuteRounder.round(MinuteRounder.parse(r.getYcTime()))));
        List<JudgeInput> inputs = new ArrayList<>();
        for (WarnInfo cmd : commands)
        {
            inputs.add(toInput(cmd, curvesByMinute, ycByMinute));
        }
        return inputs;
    }

    /** 阶跃保持语义：≤ 当时刻最近一条 yc501 值（正式版 §1.2 同族）；无先行数据返回 -1 */
    private int heldExemptFlag(LocalDateTime at)
    {
        Integer held = null;
        for (YcHistory row : exemptFlagRows)
        {
            if (!MinuteRounder.round(MinuteRounder.parse(row.getYcTime())).isAfter(at))
            {
                held = row.getYcData().intValue();
            }
        }
        return held == null ? -1 : held;
    }

    private String scenarioIdOf(JudgeInput input, Map<String, String> dayById)
    {
        return dayById.entrySet().stream()
                .filter(e -> e.getValue().equals(input.scenarioDate()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("无场景匹配日期: " + input.scenarioDate()));
    }

    // ────────────────── 契约准入：19 调节场景全量 oracle ──────────────────

    @Test
    void contractAdmission_all19Scenarios_matchManifestOracle()
    {
        Map<String, RegulationOutcome> byScenario = runByScenario();
        Map<String, String> dayById = scenarioDay();
        List<JudgeInput> inputs = allInputs(allCommands());

        for (int i = 0; i < 19; i++)
        {
            JSONObject scene = manifest.getJSONObject(i);
            String id = scene.getString("id");
            JSONObject expected = scene.getJSONObject("expected");

            if ("S17".equals(id))
            {
                continue; // 双指令单独测（双通道各判各的）
            }

            RegulationOutcome outcome = byScenario.get(id);
            if ("SKIP".equals(expected.getString("fast")))
            {
                assertSkipMapping(id, outcome);
                continue;
            }
            RegulationOutcome.Judged j = assertInstanceOf(RegulationOutcome.Judged.class, outcome,
                    id + " 非 SKIP 期望应产出 Judged");

            // 免考旗真实值（指令时点阶跃保持）：本批合成数据窗口内恒定，取 t₀ 时点
            JudgeInput input = inputs.stream()
                    .filter(in -> dayById.get(id).equals(in.scenarioDate()))
                    .findFirst().orElseThrow();
            int yx501 = heldExemptFlag(input.t0());

            assertTierVerdict(id, "fast", expected.getString("fast"), j.fast());
            assertTierVerdict(id, "econ", expected.getString("econ"), j.econ());

            // 免考层两段式（正式版 §2.6 后置应用，结论不跨档）
            TierFinalDisposition disposition = ExemptionApplier.apply(j, yx501);
            assertTierFinal(id, "fast", expected.getString("fast"), yx501, disposition.fast());
            assertTierFinal(id, "econ", expected.getString("econ"), yx501, disposition.econ());
        }
    }

    /** §4.2 映射表：judge 层 QUAL/PEN/EXEMPT→PENALIZED。 */
    private void assertTierVerdict(String id, String tier, String expect, Optional<Verdict> actual)
    {
        switch (expect)
        {
            case "QUAL" -> assertEquals(Optional.of(Verdict.QUALIFIED), actual,
                    id + " " + tier + " 期望合格");
            case "PEN", "EXEMPT" -> assertEquals(Optional.of(Verdict.PENALIZED), actual,
                    id + " " + tier + " 期望计罚（EXEMPT 为免考层概念，judge 层仍是 PENALIZED）");
            default -> fail(id + " " + tier + " 未预期的期望值: " + expect);
        }
    }

    /** §4.2 映射表：免考层 EXEMPT→EXEMPTED（yx501=1）/ PEN→PENALIZED（yx501=0）/ QUAL 透传。 */
    private void assertTierFinal(String id, String tier, String expect, int yx501, FinalTierState state)
    {
        switch (expect)
        {
            case "QUAL" -> assertEquals(FinalTierState.QUALIFIED, state, id + " " + tier + " 合格透传");
            case "PEN" -> {
                assertEquals(0, yx501, id + " 计罚场景合成库免考旗应为 0");
                assertEquals(FinalTierState.PENALIZED, state, id + " " + tier + " 计罚");
            }
            case "EXEMPT" -> {
                assertEquals(1, yx501, id + " 免考场景合成库免考旗应为 1");
                assertEquals(FinalTierState.EXEMPTED, state, id + " " + tier + " 免考应用");
            }
            default -> fail(id + " " + tier + " 未预期的期望值: " + expect);
        }
    }

    /** SKIP 分流：S11 缺t₀ / S12 编码脏写 → Undecodable 归因；S14/S16 → invalidTiers 承载。 */
    private void assertSkipMapping(String id, RegulationOutcome outcome)
    {
        switch (id)
        {
            case "S11" -> assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE),
                    outcome, "S11 缺 t₀ 实时电压 → Undecodable{缺t₀电压}");
            case "S12" -> assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                    outcome, "S12 编码脏写 → Undecodable{编码脏写}");
            case "S14", "S16" -> {
                RegulationOutcome.Judged j = assertInstanceOf(RegulationOutcome.Judged.class, outcome,
                        id + " 应产出 Judged（invalidTiers 承载，不进 Undecodable）");
                assertEquals(Set.of(Tier.FAST), j.invalidTiers(),
                        id + " 快速档不可判（整档全缺 / L>H）");
                assertTrue(j.fast().isEmpty(), id + " 无效档 VERDICT 必空（构造期不变式）");
                assertEquals(Optional.of(Verdict.QUALIFIED), j.econ(), id + " 经济档正常出结论");
                assertEquals("S14".equals(id) ? 0.2 : 1.0, j.completeness(), 1e-9,
                        id + " completeness 如实上报（布局 [1..5]）");
                assertEquals(FinalTierState.INVALID, ExemptionApplier.apply(j, 1).fast(),
                        id + " 无效档免考层透传 INVALID");
            }
            default -> fail("未登记的 SKIP 场景: " + id);
        }
    }

    // ────────────────── 结构级断言（沿 D9，oracle 化补充） ──────────────────

    @Test
    void scenarioRun_all20Commands_deterministic()
    {
        List<JudgeInput> inputs = allInputs(allCommands());
        assertEquals(20, inputs.size(), "合成库应恰 20 条电压指令（19 S 场景 + S17 双指令）");

        Map<String, RegulationOutcome> firstRun = new HashMap<>();
        for (JudgeInput in : inputs)
        {
            firstRun.put(in.cmd().warnTime() + "|" + in.cmd().objNum(),
                    judge.judge(in.cmd(), in.curves(), PRODUCTION_PARAMS));
        }
        for (JudgeInput in : inputs)
        {
            RegulationOutcome again = judge.judge(in.cmd(), in.curves(), PRODUCTION_PARAMS);
            assertEquals(firstRun.get(in.cmd().warnTime() + "|" + in.cmd().objNum()), again,
                    "确定性：同输入两次判定须相等（禁随机）");
        }
    }

    @Test
    void partialMissing_S13_completenessFraction_noInvalid()
    {
        RegulationOutcome.Judged j = runJudgedByScenario().get("S13");
        assertNotNull(j);
        assertTrue(j.invalidTiers().isEmpty(), "部分缺不走 invalidTiers");
        assertEquals(0.8, j.completeness(), 1e-9, "缺分钟 3 → 4/5 如实上报");
    }

    @Test
    void s17_dualCommand_bothJudgedIndependently()
    {
        List<JudgeInput> inputs = allInputs(allCommands()).stream()
                .filter(in -> in.scenarioDate().equals(BASE_DATE.plusDays(16).toLocalDate().toString()))
                .toList();
        assertEquals(2, inputs.size(), "S17 应有双指令");
        Set<Long> channels = new java.util.HashSet<>();
        for (JudgeInput in : inputs)
        {
            RegulationOutcome o = judge.judge(in.cmd(), in.curves(), PRODUCTION_PARAMS);
            RegulationOutcome.Judged j = assertInstanceOf(RegulationOutcome.Judged.class, o,
                    "S17 两指令均解码成功");
            channels.add(in.cmd().objNum());
            assertEquals(1.0, j.completeness(), 1e-9);
            assertTrue(j.invalidTiers().isEmpty());
        }
        assertEquals(java.util.Set.of(0L, 1L), channels, "obj_num 0/1 双通道独立入判");
    }

    @Test
    void uptimeScenarios_emitNoCommands()
    {
        Map<String, String> dayById = scenarioDay();
        List<JudgeInput> inputs = allInputs(allCommands());
        Set<String> commandDates = new java.util.HashSet<>();
        for (JudgeInput in : inputs)
        {
            commandDates.add(in.scenarioDate());
        }
        for (int i = 19; i < 26; i++)
        {
            String uId = manifest.getJSONObject(i).getString("id");
            assertFalse(commandDates.contains(dayById.get(uId)),
                    uId + " 投运率场景不应有电压指令");
        }
    }

    // ────────────────── 共用 ──────────────────

    private Map<String, RegulationOutcome> runByScenario()
    {
        Map<String, RegulationOutcome> out = new HashMap<>();
        Map<String, String> dayById = scenarioDay();
        for (JudgeInput in : allInputs(allCommands()))
        {
            out.put(scenarioIdOf(in, dayById), judge.judge(in.cmd(), in.curves(), PRODUCTION_PARAMS));
        }
        return out;
    }

    private Map<String, RegulationOutcome.Judged> runJudgedByScenario()
    {
        Map<String, RegulationOutcome.Judged> out = new HashMap<>();
        runByScenario().forEach((id, o) -> {
            if (o instanceof RegulationOutcome.Judged j)
            {
                out.put(id, j);
            }
        });
        return out;
    }
}
