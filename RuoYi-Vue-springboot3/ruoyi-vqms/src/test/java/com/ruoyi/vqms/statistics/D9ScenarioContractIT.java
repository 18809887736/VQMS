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
 * D9 场景契约 IT（L1，外部源直连）：avc-data-gen 26 场景跑通 stub judge——
 * 「Undecodable 按原因分类、completeness 如实上报」完成标准落测（v5.0 §12.1 D9）。
 *
 * <h3>断言边界（关键）</h3>
 * manifest.expected 的 QUAL/PEN/EXEMPT/SKIP 是 <b>S1 真判定的 oracle</b>；D9 只断言
 * <b>输出契约级事实</b>：SKIP→Undecodable 归因 / invalidTiers 承载 / completeness 分数 /
 * 确定性 / stub 占位结论。QUAL/PEN/EXEMPT 的判定正确性<b>不在此测</b>（stub 一律
 * QUALIFIED，替换 S1 后本 IT 升级为全量 oracle 比对）。
 *
 * <h3>参数口径</h3>
 * {@link JudgeParams} 取<b>生成器场景布局 (t_fast=5, t_econ=30)</b>——manifest 期望按此布局
 * 排布（_win_curve：fast 分钟 1-5、econ 分钟 6-30）。与生产整定种子 (4,5) 的对齐属真实数据
 * 回放前置项（测试方案已登记），届时重灌数据 + 重算 manifest 后同步。
 */
class D9ScenarioContractIT
{
    /** 生成器场景布局口径（见类注——非生产种子 4/5） */
    private static final JudgeParams GENERATOR_PARAMS = new JudgeParams(5, 30);

    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    private static final String RANGE_START = "2026-03-15 00:00:00";
    private static final String RANGE_END = "2026-04-03 00:00:00";

    /** manifest 场景日锚点（thresholds.yaml base_date），S01=第 0 天、逐序号 +1 天 */
    private static final LocalDateTime BASE_DATE = LocalDateTime.parse("2026-03-15T00:00:00");

    private static final int REALTIME_YC_MAIN = 4002;

    private static WarnInfoMapper warnMapper;
    private static HisCurveSvMapper curveMapper;
    private static YcHistoryMapper ycMapper;
    private static JSONArray manifest;

    private final StubRegulationJudge judge = new StubRegulationJudge();

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

    /** 单条指令 → stub 输入（t₀=就近取整；realtime=t₀ 时刻 yc4002 值；曲线=主母线偏移 1..30）。
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
        for (int offset = 1; offset <= GENERATOR_PARAMS.tEcon(); offset++)
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
                curves, t0.toLocalDate().toString());
    }

    private record JudgeInput(AvcCommand cmd, List<MinuteCurve> curves, String scenarioDate)
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
        for (YcHistory row : ycMapper.selectByRange(RANGE_START, RANGE_END))
        {
            ycByMinute.computeIfAbsent(
                    MinuteRounder.round(MinuteRounder.parse(row.getYcTime())), k -> new ArrayList<>()).add(row);
        }
        List<JudgeInput> inputs = new ArrayList<>();
        for (WarnInfo cmd : commands)
        {
            inputs.add(toInput(cmd, curvesByMinute, ycByMinute));
        }
        return inputs;
    }

    private String scenarioIdOf(JudgeInput input, Map<String, String> dayById)
    {
        return dayById.entrySet().stream()
                .filter(e -> e.getValue().equals(input.scenarioDate()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("无场景匹配日期: " + input.scenarioDate()));
    }

    // ────────────────── 断言 ──────────────────

    @Test
    void scenarioRun_all20Commands_produceOutcomes_deterministic()
    {
        List<JudgeInput> inputs = allInputs(allCommands());
        assertEquals(20, inputs.size(), "合成库应恰 20 条电压指令（19 S 场景 + S17 双指令）");

        Map<String, RegulationOutcome> firstRun = new HashMap<>();
        for (JudgeInput in : inputs)
        {
            RegulationOutcome o = judge.judge(in.cmd(), in.curves(), GENERATOR_PARAMS);
            firstRun.put(in.cmd().warnTime() + "|" + in.cmd().objNum(), o);
        }
        for (JudgeInput in : inputs)
        {
            RegulationOutcome again = judge.judge(in.cmd(), in.curves(), GENERATOR_PARAMS);
            assertEquals(firstRun.get(in.cmd().warnTime() + "|" + in.cmd().objNum()), again,
                    "确定性：同输入两次判定须相等（禁随机）");
        }
        assertTrue(firstRun.values().stream().allMatch(java.util.Objects::nonNull));
    }

    @Test
    void skipScenarios_S11_S12_mapToUndecodableWithReasons()
    {
        Map<String, RegulationOutcome> byScenario = runByScenario();
        assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.MISSING_T0_VOLTAGE),
                byScenario.get("S11"), "S11 缺 t₀ 实时电压 → Undecodable{缺t₀电压}");
        assertEquals(new RegulationOutcome.Undecodable(DecodeFailureReason.CORRUPTED_ENCODING),
                byScenario.get("S12"), "S12 编码脏写 → Undecodable{编码脏写}");
    }

    @Test
    void tierSkipScenarios_S14_S16_fastInvalidViaInvalidTiers()
    {
        Map<String, RegulationOutcome.Judged> judged = runJudgedByScenario();

        for (String id : new String[] {"S14", "S16"})
        {
            RegulationOutcome.Judged j = judged.get(id);
            assertNotNull(j, id + " 应产出 Judged");
            assertEquals(java.util.Set.of(Tier.FAST), j.invalidTiers(),
                    id + " 快速档不可判（整档全缺 / L>H）");
            assertTrue(j.fast().isEmpty(), id + " 无效档 VERDICT 必空（构造期不变式）");
            assertTrue(j.econ().isPresent(), id + " 经济档正常出结论（stub 占位）");
        }
        // completeness 如实上报（布局 [1..30]）：S14 缺分钟 1-5 → 25/30；S16 全在 → 30/30
        assertEquals(25.0 / 30.0, judged.get("S14").completeness(), 1e-9);
        assertEquals(1.0, judged.get("S16").completeness(), 1e-9);
    }

    @Test
    void partialMissing_S13_completenessFraction_noInvalid()
    {
        RegulationOutcome.Judged j = runJudgedByScenario().get("S13");
        assertNotNull(j);
        assertTrue(j.invalidTiers().isEmpty(), "部分缺不走 invalidTiers");
        assertEquals(29.0 / 30.0, j.completeness(), 1e-9, "缺分钟 3 → 29/30 如实上报");
    }

    @Test
    void regularScenarios_fullCompleteness_stubVerdicts()
    {
        // S01-S10/S15/S18/S19 + EXEMPT 族（S05-S07，免考是阶段三概念、judge 层仍是普通结论）
        Map<String, RegulationOutcome.Judged> judged = runJudgedByScenario();
        for (String id : new String[] {"S01", "S02", "S03", "S04", "S05", "S06", "S07",
                "S08", "S09", "S10", "S15", "S18", "S19"})
        {
            RegulationOutcome.Judged j = judged.get(id);
            assertNotNull(j, id + " 应产出 Judged（解码成功）");
            assertTrue(j.invalidTiers().isEmpty(), id + " 不应有无效档");
            assertEquals(1.0, j.completeness(), 1e-9, id + " 窗口完整度应为满");
            assertEquals(Optional.of(Verdict.QUALIFIED), j.fast(), id + " stub 占位结论");
            assertEquals(Optional.of(Verdict.QUALIFIED), j.econ(), id + " stub 占位结论");
        }
    }

    @Test
    void s17_dualCommand_bothJudgedIndependently()
    {
        List<WarnInfo> commands = allCommands();
        Map<String, List<JudgeInput>> byDate = new HashMap<>();
        // 复用输入组装：只看 S17 日（2026-03-31 = 第 16 天）
        List<JudgeInput> inputs = allInputs(commands).stream()
                .filter(in -> in.scenarioDate().equals(BASE_DATE.plusDays(16).toLocalDate().toString()))
                .toList();
        assertEquals(2, inputs.size(), "S17 应有双指令");
        Set<Long> channels = new java.util.HashSet<>();
        for (JudgeInput in : inputs)
        {
            RegulationOutcome o = judge.judge(in.cmd(), in.curves(), GENERATOR_PARAMS);
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
            out.put(scenarioIdOf(in, dayById), judge.judge(in.cmd(), in.curves(), GENERATOR_PARAMS));
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
