package com.ruoyi.web.controller.vqms;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * D5 验收（L0）：perms 三方一致——v5.0 §10.1 权威清单 ⊆ Controller @PreAuthorize ⊆ sql/vqms_menu.sql。
 * 防漂移：任何一方改动（URL/perms 段式）本测试即红。
 */
class D5PermsConsistencyTest
{
    /** §10.1 全部业务 perms（含占位导出） */
    private static final Set<String> SPEC_PERMS = Set.of(
            "vqms:curve:list",
            "vqms:daily:list", "vqms:monthly:list", "vqms:yearly:list",
            "vqms:daily:export", "vqms:monthly:export", "vqms:yearly:export",
            "vqms:avc:runtime:list", "vqms:avc:regulation:list",
            "vqms:avc:runtime:export", "vqms:avc:regulation:export",
            "vqms:threshold:list", "vqms:threshold:query", "vqms:threshold:add",
            "vqms:threshold:edit", "vqms:threshold:remove", "vqms:threshold:export",
            "vqms:vqms_busbar:list");

    private static final Class<?>[] CONTROLLERS = {
            VqmsCurveController.class, VqmsStatsController.class, VqmsAvcController.class,
            VqmsThresholdController.class, VqmsBusbarController.class };

    @Test
    void assert_控制器perms完全覆盖权威清单() throws Exception
    {
        Set<String> actual = controllerPerms();
        for (String perm : SPEC_PERMS)
        {
            Assertions.assertTrue(actual.contains(perm), "Controller 缺 §10.1 perm: " + perm);
        }
        Assertions.assertEquals(SPEC_PERMS.size(), actual.size(),
                () -> "Controller perms 与 §10.1 清单有出入: " + diff(SPEC_PERMS, actual));
    }

    @Test
    void assert_菜单SQLperms与控制器一致() throws Exception
    {
        Set<String> menuPerms = menuSqlPerms();
        Set<String> controllers = controllerPerms();
        for (String perm : controllers)
        {
            Assertions.assertTrue(menuPerms.contains(perm), "vqms_menu.sql 缺 perm: " + perm);
        }
    }

    private static Set<String> controllerPerms()
    {
        Set<String> perms = new HashSet<>();
        for (Class<?> clazz : CONTROLLERS)
        {
            for (Method method : clazz.getDeclaredMethods())
            {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                if (anno != null)
                {
                    Matcher m = Pattern.compile("'([^']+)'").matcher(anno.value());
                    Assertions.assertTrue(m.find(), clazz.getSimpleName() + " @PreAuthorize 无字面量: " + anno.value());
                    perms.add(m.group(1));
                }
            }
        }
        return perms;
    }

    private static Set<String> menuSqlPerms() throws Exception
    {
        Path sql = Paths.get("..", "..", "sql", "vqms_menu.sql").toAbsolutePath().normalize();
        Assertions.assertTrue(Files.exists(sql), "找不到 " + sql);
        Set<String> perms = new HashSet<>();
        Matcher m = Pattern.compile("'(vqms:[a-z_:]+)'").matcher(Files.readString(sql, StandardCharsets.UTF_8));
        while (m.find())
        {
            perms.add(m.group(1));
        }
        return perms;
    }

    private static String diff(Set<String> expected, Set<String> actual)
    {
        StringBuilder sb = new StringBuilder();
        actual.stream().filter(p -> !expected.contains(p)).forEach(p -> sb.append("多出:").append(p).append(" "));
        expected.stream().filter(p -> !actual.contains(p)).forEach(p -> sb.append("缺失:").append(p));
        return sb.toString();
    }
}
