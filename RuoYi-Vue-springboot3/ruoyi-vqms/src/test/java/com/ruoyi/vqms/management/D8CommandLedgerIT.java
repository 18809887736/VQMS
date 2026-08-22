package com.ruoyi.vqms.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ruoyi.vqms.management.domain.VqmsCommandLedger;
import com.ruoyi.vqms.management.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.management.service.VqmsCommandLedgerService;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.reader.SourceReader;

/**
 * D8 集成测试：指令流水账 vqms_command_ledger（v5.0 §12.1 完成标准 / 测试方案 §4.7）。
 *
 * <p>双数据源直连：外部源 qheatavchisdb @3306（只读，VQMS_AVC_TEST_USER/PASSWORD）+
 * 主库 ry_vqms @13306（MYSQL_ROOT_PASSWORD）。不经 RuoYi @DataSource 路由（同 D1 IT——
 * 该路由是 RuoYi 既有机制）；Mysql57SourceReader 的 mapper 绑外部源 SSF、流水账 mapper
 * 绑主库 SSF，服务真实串起「读外部源 → 摘录落主库」全程。</p>
 *
 * <p>行级自清理：仅哨兵测试行（warn_time 2099-*）用例内自删；场景入账行是外部源合成
 * 数据的忠实摘录、重抓幂等，留库即 D8 功能本体。</p>
 */
@ExtendWith(SpringExtension.class)
class D8CommandLedgerIT
{
    /** 全量窗口：覆盖合成库全部场景日（S/U 系列 2026-03 月内） */
    private static final String WIN_START = "2026-03-01 00:00:00";
    private static final String WIN_END = "2026-04-03 00:00:00";

    /** 哨兵测试行专用时间前缀（远离任何场景日，用例内自删） */
    private static final String SENTINEL_START = "2099-01-01 00:00:00";
    private static final String SENTINEL_END = "2099-12-31 23:59:59";

    private static final String MASTER_URL =
            "jdbc:mysql://10.0.0.9:13306/ry_vqms"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    private static final String SOURCE_URL =
            "jdbc:mysql://10.0.0.9:3306/qheatavchisdb"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";

    @Configuration
    @MapperScan(basePackages = "com.ruoyi.vqms.source.mapper",
                sqlSessionFactoryRef = "sourceSessionFactory")
    @MapperScan(basePackages = "com.ruoyi.vqms.management.mapper",
                sqlSessionFactoryRef = "masterSessionFactory")
    @ComponentScan(basePackages = "com.ruoyi.vqms.source.reader")
    static class TestConfig
    {
        @Bean
        public DataSource sourceDataSource()
        {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl(SOURCE_URL);
            String user = java.util.Objects.requireNonNull(
                    System.getenv("VQMS_AVC_TEST_USER"), "缺少 VQMS_AVC_TEST_USER");
            String password = java.util.Objects.requireNonNull(
                    System.getenv("VQMS_AVC_TEST_PASSWORD"), "缺少 VQMS_AVC_TEST_PASSWORD");
            ds.setUsername(user);
            ds.setPassword(password);
            return ds;
        }

        @Bean
        public DataSource masterDataSource()
        {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl(MASTER_URL);
            // 凭证不入库（CLAUDE.md Security）：密码走环境变量必填
            String password = java.util.Objects.requireNonNull(
                    System.getenv("MYSQL_ROOT_PASSWORD"), "缺少 MYSQL_ROOT_PASSWORD");
            ds.setUsername("root");
            ds.setPassword(password);
            return ds;
        }

        @Bean
        public SqlSessionFactory sourceSessionFactory(@Qualifier("sourceDataSource") DataSource ds) throws Exception
        {
            return buildSessionFactory(ds);
        }

        @Bean
        public SqlSessionFactory masterSessionFactory(@Qualifier("masterDataSource") DataSource ds) throws Exception
        {
            return buildSessionFactory(ds);
        }

        private SqlSessionFactory buildSessionFactory(DataSource ds) throws Exception
        {
            SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
            bean.setDataSource(ds);
            bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:mapper/vqms/*Mapper.xml"));
            return bean.getObject();
        }

        /** 显式声明（不扫 management.service 包——避免拉入依赖 Redis 的其他 Service）；字段注入照常生效 */
        @Bean
        public VqmsCommandLedgerService commandLedgerService()
        {
            return new VqmsCommandLedgerService();
        }
    }

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsCommandLedgerMapper ledgerMapper;

    @Autowired
    private VqmsCommandLedgerService service;

    @Autowired
    private DataSource masterDataSource;

    /** 外部源原文快照（raw SQL 全量 warn_type=5，不经闸门——用于逐字段比对） */
    private List<WarnInfo> externalRawCommands()
    {
        return ctx.getBean(WarnInfoMapper.class).selectByRangeAndType(WIN_START, WIN_END, 5L);
    }

    private String keyOf(String warnTime, String millisecond, Long objNum)
    {
        return warnTime + "|" + millisecond + "|" + objNum;
    }

    @Test
    void ingest_fullWindow_allCommandsFiled_fieldPerfectMatch()
    {
        List<WarnInfo> raw = externalRawCommands();
        Assertions.assertFalse(raw.isEmpty(), "外部源应存在 warn_type=5 合成指令");

        service.ingest(WIN_START, WIN_END);

        List<VqmsCommandLedger> filed = ledgerMapper.selectByWarnTimeRange(WIN_START, WIN_END);
        Assertions.assertEquals(raw.size(), filed.size(),
                "合成库全部 warn_type=5 指令应全量入账（行数与外部源一致）");

        // 双向逐字段比对：账内每行 = 外部源原文，无多余行、无遗漏、无重复
        Set<String> extKeys = new HashSet<>();
        for (WarnInfo c : raw)
        {
            extKeys.add(keyOf(c.getWarnTime(), c.getMillisecond(), c.getObjNum()));
        }
        Set<String> seenKeys = new HashSet<>();
        for (VqmsCommandLedger row : filed)
        {
            String k = keyOf(row.getWarnTime(), row.getMillisecond(), row.getObjNum());
            Assertions.assertTrue(extKeys.contains(k), "账内出现外部源没有的行: " + k);
            Assertions.assertTrue(seenKeys.add(k), "账内重复行: " + k);
            WarnInfo src = raw.stream().filter(c ->
                    keyOf(c.getWarnTime(), c.getMillisecond(), c.getObjNum()).equals(k))
                    .findFirst().get();
            Assertions.assertEquals(src.getWarnType(), row.getWarnType(), "warn_type 不一致: " + k);
            Assertions.assertEquals(src.getWarnContent(), row.getWarnContent(), "warn_content 不一致: " + k);
        }
        Assertions.assertEquals(extKeys.size(), seenKeys.size(), "外部源指令有遗漏未入账");
    }

    @Test
    void ingest_repeat_idempotent_rowCountUnchanged()
    {
        // 测试方案 §4.7 原文形态「重复抓取两遍，行数不变」：第一抓自足预热（ingest 本身幂等，
        // 空库/独立测试库/任意方法序都成立），第二抓断言零新增、行数不变
        service.ingest(WIN_START, WIN_END);
        long before = ledgerMapper.countAll();
        int secondInsert = service.ingest(WIN_START, WIN_END);
        long after = ledgerMapper.countAll();

        Assertions.assertEquals(0, secondInsert, "重复抓取应全部被 uk 拦下（新增 0 行）");
        Assertions.assertEquals(before, after, "重复抓取后总行数不得变化");
    }

    @Test
    void ingest_longMillisecond_fidelityPreserved()
    {
        // 对抗验证吸收回归锁：源 warn_info.millisecond 为 varchar(255) 无约束，账列已与源同宽；
        // 窄列时代超宽值会被 insert ignore 静默截断且截断值进 uk 致假碰撞——此处锁定逐字节保真
        try
        {
            ledgerMapper.deleteByWarnTimeRange(SENTINEL_START, SENTINEL_END);
            VqmsCommandLedger row = new VqmsCommandLedger();
            row.setWarnTime("2099-07-01 00:00:00");
            row.setMillisecond("123456789"); // 9 字符，窄 varchar(8) 下会静默截成 '12345678'
            row.setWarnType(5L);
            row.setObjNum(99L);
            row.setWarnContent("D8 IT 哨兵：超宽毫秒保真");
            ledgerMapper.insertIgnoreBatch(List.of(row));

            List<VqmsCommandLedger> filed = ledgerMapper.selectByWarnTimeRange(
                    "2099-07-01 00:00:00", "2099-07-01 00:00:01");
            Assertions.assertEquals(1, filed.size());
            Assertions.assertEquals("123456789", filed.get(0).getMillisecond(),
                    "毫秒原文须逐字节保真（任何截断即失败）");
        }
        finally
        {
            ledgerMapper.deleteByWarnTimeRange(SENTINEL_START, SENTINEL_END);
        }
    }

    @Test
    void ingest_nullObjNum_dedupAtDbLayer()
    {
        try
        {
            // 先清哨兵区，保证从零开始
            ledgerMapper.deleteByWarnTimeRange(SENTINEL_START, SENTINEL_END);

            VqmsCommandLedger sentinel = new VqmsCommandLedger();
            sentinel.setWarnTime("2099-06-01 12:00:00");
            sentinel.setMillisecond(null);          // millisecond 也为 NULL——双可空键列归一后仍须唯一
            sentinel.setWarnType(5L);
            sentinel.setObjNum(null);               // MySQL 唯一键视 NULL 互不相同——生成列归一后才拦得住
            sentinel.setWarnContent("D8 IT 哨兵：NULL obj_num 去重验证");

            List<VqmsCommandLedger> batch = new ArrayList<>();
            batch.add(sentinel);
            batch.add(sentinel); // 同批内自重复

            int firstInsert = ledgerMapper.insertIgnoreBatch(batch);
            Assertions.assertEquals(1, firstInsert, "NULL 键行同批两条只应落 1 行");
            int repeatInsert = ledgerMapper.insertIgnoreBatch(batch);
            Assertions.assertEquals(0, repeatInsert, "跨批次重复 NULL 键行应被 uk 拦下");

            // DB 层直接拦（非应用层逻辑）：不带 ignore 的原生 INSERT 必炸——约束存在的直接证明
            try (Connection conn = masterDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "insert into vqms_command_ledger (warn_time, millisecond, warn_type, obj_num, warn_content)"
                         + " values ('2099-06-01 12:00:00', null, 5, null, 'D8 IT 原生旁路尝试')"))
            {
                ps.executeUpdate();
                Assertions.fail("uk_cmd 应拒绝 NULL 键重复插入（唯一键冲突异常预期）");
            }
            catch (java.sql.SQLIntegrityConstraintViolationException e)
            {
                // 预期：DB 层结构性拦截成立
            }
            catch (java.sql.SQLException e)
            {
                // Connector/J 可能包一层；只要消息是唯一键冲突就放行，其余异常即失败
                Assertions.assertTrue(
                        e.getMessage() != null && e.getMessage().contains("Duplicate entry"),
                        "应为唯一键冲突异常，实际: " + e);
            }

            // 归一值实证：NULL → '' / -1 真实写进生成列，且哨兵键只 1 行
            try (Connection conn = masterDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "select millisecond_uk, obj_num_uk from vqms_command_ledger"
                         + " where warn_time = '2099-06-01 12:00:00'");
                 ResultSet rs = ps.executeQuery())
            {
                Assertions.assertTrue(rs.next(), "哨兵行应在库");
                Assertions.assertEquals("", rs.getString(1), "millisecond_uk 应归一为空串");
                Assertions.assertEquals(-1L, rs.getLong(2), "obj_num_uk 应归一为 -1");
                Assertions.assertFalse(rs.next(), "哨兵键只应有 1 行");
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new IllegalStateException(e);
        }
        finally
        {
            ledgerMapper.deleteByWarnTimeRange(SENTINEL_START, SENTINEL_END);
        }
    }

    @Test
    void ddl_noVerdictColumns_exactColumnSet()
    {
        // 存储切分铁律例外边界：只记原始事实。列集合精确相等——多出任何列（尤其判定/解码结论列）即失败
        List<String> expected = List.of("id", "warn_time", "millisecond", "warn_type",
                "obj_num", "warn_content", "fetched_at", "millisecond_uk", "obj_num_uk");
        List<String> actual = new ArrayList<>();
        try (Connection conn = masterDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "select column_name from information_schema.columns"
                     + " where table_schema = database() and table_name = 'vqms_command_ledger'"
                     + " order by ordinal_position");
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                actual.add(rs.getString(1));
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new IllegalStateException(e);
        }
        // 列序不敏感（fresh 建表与增量迁移生成列位置不同，语义无差）——比集合不比顺序
        List<String> expectedSorted = new ArrayList<>(expected);
        java.util.Collections.sort(expectedSorted);
        java.util.Collections.sort(actual);
        Assertions.assertEquals(expectedSorted, actual,
                "vqms_command_ledger 列集合必须精确匹配（原始摘录列 + 幂等生成列，无判定/解码结论列）");
    }
}
