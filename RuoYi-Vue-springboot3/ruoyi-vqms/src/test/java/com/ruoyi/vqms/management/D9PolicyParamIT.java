package com.ruoyi.vqms.management;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.management.service.VqmsJudgeParamService;
import com.ruoyi.vqms.management.service.VqmsPolicyParamService;
import com.ruoyi.vqms.statistics.Disposition;
import com.ruoyi.vqms.statistics.PolicyConfig;
import com.ruoyi.vqms.statistics.RegulationOutcome;
import com.ruoyi.vqms.statistics.Tier;

/**
 * D9 集成测试（L1，管理侧）：vqms_policy_param 表 + 策略配置装配 + 「改配置行为即变」。
 *
 * <p>直连共享主库 ry_vqms @13306（MYSQL_ROOT_PASSWORD），行级自清理（用例内自删约定键）。</p>
 */
@ExtendWith(SpringExtension.class)
class D9PolicyParamIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:13306/ry_vqms"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    private static final String[] CANONICAL_KEYS = {
            "undecodable_mode", "invalid_tier_mode", "partial_missing_mode", "partial_missing_threshold_pct"};

    @Configuration
    @MapperScan("com.ruoyi.vqms.management.mapper")
    @ComponentScan(basePackages = "com.ruoyi.vqms.management.service",
            excludeFilters = {
                    // D7 Service 依赖 RedisCache；D8 Service 依赖 SourceReader——本上下文都不载入
                    @org.springframework.context.annotation.ComponentScan.Filter(
                            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                            classes = VqmsJudgeParamService.class),
                    @org.springframework.context.annotation.ComponentScan.Filter(
                            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                            classes = com.ruoyi.vqms.management.service.VqmsCommandLedgerService.class)})
    static class TestConfig
    {
        @Bean
        public DataSource dataSource()
        {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl(DB_URL);
            // 凭证不入库（CLAUDE.md Security）：密码走环境变量必填
            String password = java.util.Objects.requireNonNull(
                    System.getenv("MYSQL_ROOT_PASSWORD"), "缺少 MYSQL_ROOT_PASSWORD");
            ds.setUsername("root");
            ds.setPassword(password);
            return ds;
        }

        @Bean
        public SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception
        {
            SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
            bean.setDataSource(ds);
            bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:mapper/vqms/*Mapper.xml"));
            return bean.getObject();
        }
    }

    @Autowired
    private VqmsPolicyParamMapper mapper;

    @Autowired
    private VqmsPolicyParamService service;

    /** 共享库防护：约定键的既有行先快照、用例后恢复——选套定稿后本 IT 在共享 ry_vqms 上跑也不得清掉线上配置 */
    private final Map<String, VqmsPolicyParam> savedRows = new java.util.HashMap<>();

    @BeforeEach
    void snapshotAndClearCanonicalKeys()
    {
        for (String key : CANONICAL_KEYS)
        {
            VqmsPolicyParam existing = mapper.selectByKey(key);
            if (existing != null)
            {
                savedRows.put(key, existing);
            }
            mapper.deleteByKey(key);
        }
    }

    @AfterEach
    void deleteInjectedAndRestoreSaved()
    {
        for (String key : CANONICAL_KEYS)
        {
            mapper.deleteByKey(key);
        }
        // 恢复语义字段；时间戳经 insert 重置（测试兜底路径，可接受）
        for (VqmsPolicyParam row : savedRows.values())
        {
            mapper.insert(row);
        }
        savedRows.clear();
    }

    private void insertRow(String key, String value, String name)
    {
        VqmsPolicyParam row = new VqmsPolicyParam();
        row.setParamKey(key);
        row.setParamValue(value);
        row.setName(name);
        row.setDescription("D9 IT 注入");
        row.setCreateBy("d9-it");
        Assertions.assertEquals(1, mapper.insert(row));
    }

    /** 乙档配置向量注入（50% 阈值） */
    private void insertYiConfig(String threshold)
    {
        insertRow("undecodable_mode", "EXCLUDE_REPORTED", "解码失败处置");
        insertRow("invalid_tier_mode", "EXCLUDE_REPORTED", "档无效处置");
        insertRow("partial_missing_mode", "EXCLUDE_REPORTED", "部分缺处置");
        insertRow("partial_missing_threshold_pct", threshold, "部分缺可用度阈值%");
    }

    @Test
    void table_startsEmpty_noSeedDispositionValues()
    {
        // 选套值留空（Leo 2026-08-18 拍板）：断言策略参数表不预设处置值（空表/无处置行）
        Assertions.assertEquals(0, mapper.countAll(), "vqms_policy_param 应零种子行");
        for (String key : CANONICAL_KEYS)
        {
            Assertions.assertNull(mapper.selectByKey(key), "约定键不应有预置行: " + key);
        }
    }

    @Test
    void emptyOrIncomplete_config_absent_notGuessed()
    {
        // 空表 → 未定稿态；缺键同样视为未定稿（不猜默认值）
        Assertions.assertTrue(service.loadConfig().isEmpty(), "空表应返回 empty");

        insertRow("undecodable_mode", "COUNT_UNQUALIFIED", "x");
        Assertions.assertTrue(service.loadConfig().isEmpty(), "缺键应返回 empty");
    }

    @Test
    void loadConfig_yiVector_assembledFromRows()
    {
        insertYiConfig("50");
        Optional<PolicyConfig> config = service.loadConfig();
        Assertions.assertTrue(config.isPresent());
        Assertions.assertEquals(Disposition.EXCLUDE_REPORTED, config.get().undecodableMode());
        Assertions.assertEquals(Disposition.EXCLUDE_REPORTED, config.get().invalidTierMode());
        Assertions.assertEquals(Disposition.EXCLUDE_REPORTED, config.get().partialMissingMode());
        Assertions.assertEquals(50, config.get().partialMissingThresholdPct());

        // 装配出的配置直接驱动策略纯函数：40% 可用 < 50% → 剔除
        RegulationOutcome.Judged partial40 = new RegulationOutcome.Judged(
                java.util.Optional.of(com.ruoyi.vqms.statistics.Verdict.QUALIFIED),
                java.util.Optional.of(com.ruoyi.vqms.statistics.Verdict.QUALIFIED),
                0.4, java.util.Set.of());
        Assertions.assertEquals(Disposition.EXCLUDE_REPORTED,
                com.ruoyi.vqms.statistics.DataUnavailabilityPolicy.evaluate(partial40, config.get()));
    }

    @Test
    void changeConfigValue_behaviorChanges_withoutRedeploy()
    {
        // 完成标准「改策略参数行为即变、代码不动」：阈值 50→30，同一输入 0.4 从剔除翻正常记账
        insertYiConfig("50");
        PolicyConfig before = service.loadConfig().orElseThrow();

        RegulationOutcome.Judged partial40 = new RegulationOutcome.Judged(
                java.util.Optional.of(com.ruoyi.vqms.statistics.Verdict.QUALIFIED),
                java.util.Optional.of(com.ruoyi.vqms.statistics.Verdict.QUALIFIED),
                0.4, java.util.Set.of());
        Assertions.assertEquals(Disposition.EXCLUDE_REPORTED,
                com.ruoyi.vqms.statistics.DataUnavailabilityPolicy.evaluate(partial40, before));

        Assertions.assertEquals(1, mapper.updateValue("partial_missing_threshold_pct", "30", "d9-it"));
        PolicyConfig after = service.loadConfig().orElseThrow();
        Assertions.assertEquals(30, after.partialMissingThresholdPct());
        Assertions.assertEquals(Disposition.COUNT_NORMAL,
                com.ruoyi.vqms.statistics.DataUnavailabilityPolicy.evaluate(partial40, after));
    }

    @Test
    void illegalEnumValue_explicitFailure_notSilent()
    {
        insertRow("undecodable_mode", "NOT_A_MODE", "x");
        insertRow("invalid_tier_mode", "EXCLUDE_REPORTED", "x");
        insertRow("partial_missing_mode", "EXCLUDE_REPORTED", "x");
        Assertions.assertThrows(IllegalStateException.class, () -> service.loadConfig(),
                "枚举值拼错须显性失败，不得静默当未定稿");
    }

    @Test
    void nonIntegerThreshold_explicitFailure()
    {
        insertRow("undecodable_mode", "EXCLUDE_REPORTED", "x");
        insertRow("invalid_tier_mode", "EXCLUDE_REPORTED", "x");
        insertRow("partial_missing_mode", "EXCLUDE_REPORTED", "x");
        insertRow("partial_missing_threshold_pct", "fifty", "x");
        Assertions.assertThrows(IllegalStateException.class, () -> service.loadConfig());
    }

    @Test
    void missingThresholdRow_explicitIllegalState_notBareNpe()
    {
        // 对抗验证吸收：三行 mode 齐、阈值行漏写 → 契约声明的 ISE（带键名上下文），非裸 NPE
        insertRow("undecodable_mode", "EXCLUDE_REPORTED", "x");
        insertRow("invalid_tier_mode", "EXCLUDE_REPORTED", "x");
        insertRow("partial_missing_mode", "EXCLUDE_REPORTED", "x");
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> service.loadConfig());
        Assertions.assertTrue(ex.getMessage().contains("partial_missing_threshold_pct"),
                "报错须点名缺失键: " + ex.getMessage());
    }

    @Test
    void selectList_roundTripsChineseFields()
    {
        insertRow("undecodable_mode", "PEND_MARKED", "解码失败处置（挂起）");
        List<VqmsPolicyParam> rows = mapper.selectList();
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("解码失败处置（挂起）", rows.get(0).getName(), "中文经 utf8mb4 往返无乱码");
    }
}
