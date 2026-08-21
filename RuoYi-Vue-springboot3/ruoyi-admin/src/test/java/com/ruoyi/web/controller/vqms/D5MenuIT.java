package com.ruoyi.web.controller.vqms;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;

import com.mysql.cj.jdbc.Driver;

/**
 * D5 验收（L1）：vqms_menu.sql 在 ry_vqms（ry 全量库）应用成功——7 个页面菜单（C）+
 * 导出/CRUD 按钮（F）落库、perms 与控制器一致、component 路径真实存在（前端 7 页）。
 */
class D5MenuIT
{
    private static Connection connection;

    @BeforeAll
    static void setUp() throws Exception
    {
        String password = System.getenv("MYSQL_ROOT_PASSWORD");
        Assertions.assertNotNull(password, "缺少 MYSQL_ROOT_PASSWORD");
        new Driver();
        connection = java.sql.DriverManager.getConnection(
                "jdbc:mysql://10.0.0.9:13306/ry_vqms?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8",
                "root", password);

        // 幂等：先清 2000~2099 段（含 sys_role_menu 绑定）再应用
        try (Statement st = connection.createStatement())
        {
            st.executeUpdate("delete from sys_role_menu where menu_id between 2000 and 2099");
            st.executeUpdate("delete from sys_menu where menu_id between 2000 and 2099");
        }
        Path sql = Paths.get("..", "..", "sql", "vqms_menu.sql").toAbsolutePath().normalize();
        Assertions.assertTrue(Files.exists(sql), "找不到 " + sql);
        org.apache.ibatis.jdbc.ScriptRunner runner = new org.apache.ibatis.jdbc.ScriptRunner(connection);
        runner.setLogWriter(null);
        runner.runScript(new InputStreamReader(Files.newInputStream(sql), StandardCharsets.UTF_8));
    }

    @AfterAll
    static void tearDown() throws Exception
    {
        if (connection != null)
        {
            connection.close();
        }
    }

    @Test
    void assert_七个页面菜单落库() throws Exception
    {
        String[][] expected = {
                { "2001", "vqms:daily:list", "vqms/daily/index" },
                { "2002", "vqms:monthly:list", "vqms/monthly/index" },
                { "2003", "vqms:yearly:list", "vqms/yearly/index" },
                { "2011", "vqms:curve:list", "vqms/curve/index" },
                { "2021", "vqms:avc:runtime:list", "vqms/avc-runtime/index" },
                { "2022", "vqms:avc:regulation:list", "vqms/avc-regulation/index" },
                { "2031", "vqms:threshold:list", "vqms/threshold/index" } };
        for (String[] row : expected)
        {
            try (Statement st = connection.createStatement();
                    ResultSet rs = st.executeQuery(
                            "select perms, component, menu_type, status from sys_menu where menu_id = " + row[0]))
            {
                Assertions.assertTrue(rs.next(), "菜单缺 " + row[0]);
                Assertions.assertEquals(row[1], rs.getString(1), "menu " + row[0] + " perms");
                Assertions.assertEquals(row[2], rs.getString(2), "menu " + row[0] + " component");
                Assertions.assertEquals("C", rs.getString(3));
                Assertions.assertEquals("0", rs.getString(4), "菜单须为启用状态");
            }
        }
    }

    @Test
    void assert_component路径真实存在() throws Exception
    {
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(
                        "select component from sys_menu where menu_type = 'C' and menu_id >= 2000"))
        {
            int checked = 0;
            while (rs.next())
            {
                String component = rs.getString(1);
                if (component == null || component.isBlank())
                {
                    continue;
                }
                Path vue = Paths.get("..", "..", "RuoYi-Vue3", "src", "views",
                        component + ".vue").toAbsolutePath().normalize();
                Assertions.assertTrue(Files.exists(vue), "component 不存在: " + component + " -> " + vue);
                checked++;
            }
            Assertions.assertEquals(7, checked, "应有 7 个 C 菜单");
        }
    }

    @Test
    void assert_按钮perms落库() throws Exception
    {
        String[] expected = { "vqms:daily:export", "vqms:monthly:export", "vqms:yearly:export",
                "vqms:avc:runtime:export", "vqms:avc:regulation:export",
                "vqms:threshold:query", "vqms:threshold:add", "vqms:threshold:edit",
                "vqms:threshold:remove", "vqms:threshold:export", "vqms:vqms_busbar:list" };
        for (String perm : expected)
        {
            try (Statement st = connection.createStatement();
                    ResultSet rs = st.executeQuery(
                            "select count(*) from sys_menu where perms = '" + perm + "' and menu_type = 'F'"))
            {
                rs.next();
                // busbar 下拉 perm 有意挂 5 处 F（每个母线维度页面一处），其余唯一
                int minExpected = "vqms:vqms_busbar:list".equals(perm) ? 5 : 1;
                Assertions.assertEquals(minExpected, rs.getInt(1), "按钮 perm 行数不符: " + perm);
            }
        }
    }

    @Test
    void assert_全部VQMS菜单绑定role2() throws Exception
    {
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(
                        "select count(*) from sys_menu m where m.menu_id between 2000 and 2099 "
                                + "and not exists (select 1 from sys_role_menu rm where rm.menu_id = m.menu_id)"))
        {
            rs.next();
            Assertions.assertEquals(0, rs.getInt(1), "存在未绑定任何角色的 VQMS 菜单（非 admin 用户将不可见/无权）");
        }
    }
}
