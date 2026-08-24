package com.ruoyi.vqms.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S2/S3 统计落库 L1 IT（主库直连，D2 同款目标 ry_vqms @13306）。
 *
 * <h3>断言口径</h3>
 * ① 七张统计表结构就位（information_schema）；② busbar_group 容量列在（决策⑤）；
 * ③ 明细表 uk 生成列幂等语义（NULL obj 双行只落一行、原生重复 INSERT 必炸——D8 同款）；
 * ④ 日 rollup 对计数求和正确（含 pended/excluded/completeness_sum）；
 * ⑤ 月 rollup 由日表求和（runtime 分钟计数）。测试数据自带自清（新表无消费者，污染面为零）。
 *
 * <h3>迁移自应用</h3>
 * BeforeAll 逐条执行 {@code sql/migrations/2026-08-24_s2_s3_stats_tables.sql}，
 * 吞"已存在"类错误（1050 表存在/1060 列重复）实现幂等——存量库已手工应用过也兼容。
 */
@TestMethodOrder(OrderAnnotation.class)
class S2S3StatsTablesIT
{
    private static final String DB_URL =
            "jdbc:mysql://10.0.0.9:13306/ry_vqms"
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8"
                    + "&allowMultiQueries=true";

    private static final String TEST_DATE_A = "2026-08-20";
    private static final long SENTINEL_OBJ = -777001L; // 测试哨兵 obj_num，清理锚点

    private static Connection conn;
    private static com.ruoyi.vqms.management.mapper.VqmsStatsRollupMapper rollupMapper;

    @BeforeAll
    static void setUp() throws Exception
    {
        String password = System.getenv("MYSQL_ROOT_PASSWORD");
        assertNotNull(password, "缺少环境变量 MYSQL_ROOT_PASSWORD");
        conn = DriverManager.getConnection(DB_URL, "root", password);
        cleanTestData(); // 防御性预清：上次异常中断可能残留测试行（幂等重跑前提）

        // 迁移自应用（幂等：1050 表已存在 / 1060 列已存在 跳过）
        for (String stmt : splitStatements(migrationSql()))
        {
            try (Statement st = conn.createStatement())
            {
                st.execute(stmt);
            }
            catch (SQLException e)
            {
                if (e.getErrorCode() != 1050 && e.getErrorCode() != 1060)
                {
                    throw e;
                }
            }
        }

        // Slice3：rollup 走 mapper 权威 SQL（单一来源）——手工 SqlSessionFactory 装配（D9 IT 同款）
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername("root");
        ds.setPassword(password);
        org.mybatis.spring.SqlSessionFactoryBean factoryBean =
                new org.mybatis.spring.SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        org.apache.ibatis.session.SqlSessionFactory sessionFactory = factoryBean.getObject();
        rollupMapper = sessionFactory.getConfiguration()
                .getMapper(com.ruoyi.vqms.management.mapper.VqmsStatsRollupMapper.class,
                        sessionFactory.openSession());
    }

    /** 测试行清理（哨兵族 obj_num_uk ≤ -777000 全覆盖）。setUp 预清 + tearDown 兜底。 */
    private static void cleanTestData() throws SQLException
    {
        try (Statement st = conn.createStatement())
        {
            st.execute("delete from vqms_regulation_cmd where obj_num_uk between -777010 and -776990");
            st.execute("delete from vqms_regulation_cmd where warn_time='2026-08-20 10:00:00'");
            st.execute("delete from vqms_regulation_daily where stat_date between '"
                    + TEST_DATE_A + "' and '" + TEST_DATE_A + "'");
            st.execute("delete from vqms_runtime_daily where stat_date between '"
                    + TEST_DATE_A + "' and '2026-08-21'");
            st.execute("delete from vqms_runtime_monthly where stat_month = '2026-08'");
            st.execute("delete from vqms_regulation_monthly where stat_month = '2026-08'");
            st.execute("delete from vqms_regulation_yearly where stat_year = 2026");
            st.execute("delete from vqms_runtime_yearly where stat_year = 2026");
        }
    }

    @AfterAll
    static void tearDown() throws Exception
    {
        if (conn == null)
        {
            return;
        }
        try
        {
            cleanTestData();
        }
        finally
        {
            conn.close();
        }
    }

    private static Path migrationPath()
    {
        String root = System.getenv("VQMS_REPO_ROOT");
        if (root != null && !root.isEmpty())
        {
            return Paths.get(root, "sql", "migrations", "2026-08-24_s2_s3_stats_tables.sql");
        }
        return Paths.get("C:/work/VQMS/sql/migrations/2026-08-24_s2_s3_stats_tables.sql");
    }

    private static String migrationSql() throws Exception
    {
        return Files.readString(migrationPath(), StandardCharsets.UTF_8);
    }

    /** 朴素按分号切分：本迁移无存储过程、列注释内无 ASCII 分号（已核）。 */
    static List<String> splitStatements(String sql)
    {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : sql.split("\n"))
        {
            if (line.trim().startsWith("--"))
            {
                continue; // 整行注释不参与切分
            }
            cur.append(line).append('\n');
            if (line.trim().endsWith(";"))
            {
                String s = cur.toString().trim();
                if (s.endsWith(";"))
                {
                    s = s.substring(0, s.length() - 1);
                }
                if (!s.isBlank())
                {
                    out.add(s);
                }
                cur.setLength(0);
            }
        }
        return out;
    }

    private static int count(String sql) throws SQLException
    {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql))
        {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ────────────────── ① 结构 ──────────────────

    @Test
    @Order(1)
    void sevenStatsTables_existWithExpectedColumns()
    throws Exception
    {
        String[][] expect = {
                {"vqms_regulation_cmd", "algorithm_id"},
                {"vqms_regulation_cmd", "t_fast_snapshot"},
                {"vqms_regulation_cmd", "disposition"},
                {"vqms_regulation_daily", "pended_count"},
                {"vqms_regulation_daily", "excluded_count"},
                {"vqms_regulation_daily", "algorithm_id"},
                {"vqms_regulation_monthly", "total_cmds"},
                {"vqms_regulation_yearly", "completeness_sum"},
                {"vqms_runtime_daily", "exit_grid_min"},
                {"vqms_runtime_monthly", "offline_min"},
                {"vqms_runtime_yearly", "penalty_score"},
        };
        for (String[] e : expect)
        {
            assertEquals(1, count("select count(*) from information_schema.columns"
                    + " where table_schema='ry_vqms' and table_name='" + e[0]
                    + "' and column_name='" + e[1] + "'"),
                    e[0] + "." + e[1] + " 应存在");
        }
    }

    @Test
    @Order(2)
    void busbarGroup_hasRatedCapacityKw()
    throws Exception
    {
        assertEquals(1, count("select count(*) from information_schema.columns"
                + " where table_schema='ry_vqms' and table_name='vqms_busbar_group'"
                + " and column_name='rated_capacity_kw'"), "决策⑤容量列应存在");
    }

    // ────────────────── ③ uk 生成列幂等 ──────────────────

    private static int insertCmd(String warnTime, Long objNum, String fastState, String econState)
    throws SQLException
    {
        try (PreparedStatement ps = conn.prepareStatement(
                "insert into vqms_regulation_cmd (stat_date, warn_time, millisecond, obj_num,"
                        + " algorithm_id, t_fast_snapshot, fast_state, econ_state, completeness)"
                        + " values (?, ?, '100', ?, 'V1_0', 4, ?, ?, 1.0)"))
        {
            ps.setString(1, TEST_DATE_A);
            ps.setString(2, warnTime);
            if (objNum == null)
            {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            else
            {
                ps.setLong(3, objNum);
            }
            ps.setString(4, fastState);
            ps.setString(5, econState);
            return ps.executeUpdate();
        }
    }

    @Test
    @Order(3)
    void cmdUk_generatedColumn_nullObj_deduplicated()
    throws Exception
    {
        assertEquals(1, insertCmd("2026-08-20 10:00:00", null,
                "QUALIFIED", "QUALIFIED"), "首插落行");
        assertThrows(SQLException.class, () -> insertCmd("2026-08-20 10:00:00", null,
                        "PENALIZED", "QUALIFIED"),
                "同键（obj=NULL 经 coalesce 归一）原生重插必炸唯一键——结构性幂等");
        assertEquals(1, count("select count(*) from vqms_regulation_cmd"
                + " where warn_time='2026-08-20 10:00:00'"), "仍只有一行（coalesce 归一后同键撞 uk）");
        // 自清：本行不带哨兵 obj（测的就是 NULL 归一），立即删除防泄漏进 rollup 断言
        try (Statement st = conn.createStatement())
        {
            st.execute("delete from vqms_regulation_cmd where warn_time='2026-08-20 10:00:00'");
        }
    }

    // ────────────────── ④⑤ rollup 冒烟 ──────────────────

    @Test
    @Order(4)
    void dailyRollup_countsSumCorrectly()
    throws Exception
    {
        // 布局（fast, econ）：Q/P、E/Q、Q/E、I/Q（invalid）、undecodable 占位 I/I、丁挂起、乙剔除
        insertCmd("2026-08-20 10:01:00", SENTINEL_OBJ + 1, "QUALIFIED", "PENALIZED");
        insertCmd("2026-08-20 10:02:00", SENTINEL_OBJ + 2, "EXEMPTED", "QUALIFIED");
        insertCmd("2026-08-20 10:03:00", SENTINEL_OBJ + 3, "QUALIFIED", "EXEMPTED");
        try (PreparedStatement ps = conn.prepareStatement(
                "insert into vqms_regulation_cmd (stat_date, warn_time, millisecond, obj_num,"
                        + " algorithm_id, t_fast_snapshot, fast_state, econ_state, completeness, invalid_tiers)"
                        + " values (?, '2026-08-20 10:04:00', '100', ?, 'V1_0', 4, 'INVALID', 'QUALIFIED', 0.5, 'FAST')"))
        {
            ps.setString(1, TEST_DATE_A);
            ps.setLong(2, SENTINEL_OBJ + 4);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "insert into vqms_regulation_cmd (stat_date, warn_time, millisecond, obj_num,"
                        + " algorithm_id, t_fast_snapshot, fast_state, econ_state, completeness,"
                        + " undecodable_reason, disposition)"
                        + " values (?, '2026-08-20 10:05:00', '100', ?, 'V1_0', 4, 'INVALID', 'INVALID', 0.0,"
                        + " 'CYCLE_CODE_INVALID', 'PEND_MARKED')"))
        {
            ps.setString(1, TEST_DATE_A);
            ps.setLong(2, SENTINEL_OBJ + 5);
            ps.executeUpdate();
        }

        // 日 rollup（S4 将提升为 mapper 的权威语句）
        try (Statement st = conn.createStatement())
        {
            int n = st.executeUpdate(
                    "insert into vqms_regulation_daily (stat_date, algorithm_id, total_cmds,"
                            + " qualified_fast, penalized_fast, exempted_fast, invalid_fast,"
                            + " qualified_econ, penalized_econ, exempted_econ, invalid_econ,"
                            + " undecodable_count, pended_count, excluded_count, completeness_sum)"
                            + " select stat_date,"
                            + " case when count(distinct algorithm_id)=1 then min(algorithm_id) else 'MIXED' end,"
                            + " count(*),"
                            + " sum(fast_state='QUALIFIED'), sum(fast_state='PENALIZED'),"
                            + " sum(fast_state='EXEMPTED'), sum(fast_state='INVALID'),"
                            + " sum(econ_state='QUALIFIED'), sum(econ_state='PENALIZED'),"
                            + " sum(econ_state='EXEMPTED'), sum(econ_state='INVALID'),"
                            + " sum(undecodable_reason is not null),"
                            + " sum(disposition='PEND_MARKED'), sum(disposition='EXCLUDE_REPORTED'),"
                            + " sum(completeness)"
                            + " from vqms_regulation_cmd where stat_date = '" + TEST_DATE_A + "'"
                            + " group by stat_date"
                            + " on duplicate key update algorithm_id=values(algorithm_id),"
                            + " total_cmds=values(total_cmds),"
                            + " qualified_fast=values(qualified_fast), penalized_fast=values(penalized_fast),"
                            + " exempted_fast=values(exempted_fast), invalid_fast=values(invalid_fast),"
                            + " qualified_econ=values(qualified_econ), penalized_econ=values(penalized_econ),"
                            + " exempted_econ=values(exempted_econ), invalid_econ=values(invalid_econ),"
                            + " undecodable_count=values(undecodable_count), pended_count=values(pended_count),"
                            + " excluded_count=values(excluded_count), completeness_sum=values(completeness_sum)");
            assertEquals(1, n, "日 rollup 应产出一行");

            try (ResultSet rs = st.executeQuery(
                    "select algorithm_id, total_cmds, qualified_fast, penalized_fast, exempted_fast,"
                            + " invalid_fast, qualified_econ, exempted_econ, invalid_econ,"
                            + " undecodable_count, pended_count, completeness_sum"
                            + " from vqms_regulation_daily where stat_date='" + TEST_DATE_A + "'"))
            {
                assertTrue(rs.next());
                assertEquals("V1_0", rs.getString(1), "单算法周期 rollup 记该 ID【决策④】");
                assertEquals(5, rs.getInt(2), "分母=发令总次数（固定分母）");
                assertEquals(2, rs.getInt(3));  // fast Q
                assertEquals(0, rs.getInt(4));  // fast P（本布局无 PENALIZED fast 行）
                assertEquals(1, rs.getInt(5));  // fast E
                assertEquals(2, rs.getInt(6));  // fast INVALID（显式 invalid + undecodable 占位）
                assertEquals(2, rs.getInt(7));  // econ Q（行2、行4）
                assertEquals(1, rs.getInt(8));  // econ E（行3）
                assertEquals(1, rs.getInt(9));  // econ INVALID（undecodable 占位行5）
                assertEquals(1, rs.getInt(10)); // undecodable
                assertEquals(1, rs.getInt(11)); // 丁挂起
                assertEquals(3.5, rs.getDouble(12), 1e-9); // completeness 1+1+1+0.5+0
            }
        }
    }

    @Test
    @Order(5)
    void monthlyRollup_sumsRuntimeMinutes()
    throws Exception
    {
        try (Statement st = conn.createStatement())
        {
            st.execute("insert into vqms_runtime_daily (stat_date, in_service_min, exit_grid_min,"
                    + " exit_nongrid_min, offline_min) values ('" + TEST_DATE_A + "', 600, 60, 30, 750)");
            st.execute("insert into vqms_runtime_daily (stat_date, in_service_min, exit_grid_min,"
                    + " exit_nongrid_min, offline_min) values ('2026-08-21', 500, 40, 30, 870)");

            st.executeUpdate(
                    "insert into vqms_runtime_monthly (stat_month, in_service_min, exit_grid_min,"
                            + " exit_nongrid_min, offline_min)"
                            + " select date_format(stat_date,'%Y-%m'), sum(in_service_min), sum(exit_grid_min),"
                            + " sum(exit_nongrid_min), sum(offline_min)"
                            + " from vqms_runtime_daily where stat_date between '" + TEST_DATE_A
                            + "' and '2026-08-21'"
                            + " group by date_format(stat_date,'%Y-%m')"
                            + " on duplicate key update in_service_min=values(in_service_min),"
                            + " exit_grid_min=values(exit_grid_min), exit_nongrid_min=values(exit_nongrid_min),"
                            + " offline_min=values(offline_min)");

            try (ResultSet rs = st.executeQuery(
                    "select in_service_min, exit_grid_min, exit_nongrid_min, offline_min"
                            + " from vqms_runtime_monthly where stat_month='2026-08'"))
            {
                assertTrue(rs.next());
                assertEquals(1100, rs.getInt(1));
                assertEquals(100, rs.getInt(2));
                assertEquals(60, rs.getInt(3));
                assertEquals(1620, rs.getInt(4), "OFFLINE 求和透传（不进率算术）");
            }
        }
    }

    /**
     * Slice3：mapper 权威 rollup 级联——五语句全走 VqmsStatsRollupMapper（SQL 单一来源），
     * 复用 order(3)~(5) 落下的行，验证幂等重跑与三级聚合。
     */
    @Test
    @Order(6)
    void cascadeRollup_viaMapper_idempotentAndAggregates()
    throws Exception
    {
        // 重跑日级（order(4) 已插过一行 daily）：幂等覆盖不翻倍
        rollupMapper.rollupRegulationDaily(TEST_DATE_A, TEST_DATE_A);
        assertEquals(1, count("select count(*) from vqms_regulation_daily"
                + " where stat_date='" + TEST_DATE_A + "'"));
        assertEquals(5, count("select total_cmds from vqms_regulation_daily"
                + " where stat_date='" + TEST_DATE_A + "'"), "幂等重算后分母不变");

        // 月级/年级（调节）：5 条指令上卷
        rollupMapper.rollupRegulationMonthly(TEST_DATE_A, TEST_DATE_A);
        rollupMapper.rollupRegulationYearly(TEST_DATE_A, TEST_DATE_A);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select algorithm_id, total_cmds from vqms_regulation_monthly"
                             + " where stat_month='2026-08'"))
        {
            assertTrue(rs.next());
            assertEquals("V1_0", rs.getString(1), "月级单一算法记该 ID【决策④】");
            assertEquals(5, rs.getInt(2));
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select total_cmds from vqms_regulation_yearly where stat_year=2026"))
        {
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }

        // 投运侧：mapper 版月级与 order(5) 原生 SQL 同数（SQL 单一来源前先证等价），年级带率重算
        rollupMapper.rollupRuntimeMonthly("2026-08-20", "2026-08-21");
        rollupMapper.rollupRuntimeYearly("2026-08-20", "2026-08-21");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select in_service_min, rate_pct from vqms_runtime_monthly"
                             + " where stat_month='2026-08'"))
        {
            assertTrue(rs.next());
            assertEquals(1100, rs.getInt(1));
            assertEquals(94.828, rs.getDouble(2), 0.001, "率=1100/(1100+60)×100");
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select in_service_min, shortfall_pct from vqms_runtime_yearly"
                             + " where stat_year=2026"))
        {
            assertTrue(rs.next());
            assertEquals(1100, rs.getInt(1));
            assertEquals(99 - 94.828, rs.getDouble(2), 0.001);
        }
    }
}
