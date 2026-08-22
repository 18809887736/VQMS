package com.ruoyi.web.controller.vqms;

import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.config.FastJson2JsonRedisSerializer;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import com.ruoyi.vqms.management.domain.VqmsJudgeParam;
import com.ruoyi.vqms.management.mapper.VqmsJudgeParamMapper;
import com.ruoyi.vqms.management.service.VqmsJudgeParamService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * D7 验收（L1，测试方案 §4.6 逐条）：值域/锁定双层校验（Service 友好报错 + DB CHECK 旁路拦截）
 * + Redis 缓存即时生效（改值后下一次 getInt 读取即新值）。真实库 ry_vqms + 真实 Redis @10.0.0.9:16379。
 */
class D7JudgeParamIT
{
    private static VqmsJudgeParamMapper mapper;

    private static VqmsJudgeParamService service;

    private static LettuceConnectionFactory redisFactory;

    private static MockMvc mockMvc;

    private static final String REDIS_KEY = "vqms:judgeParam:t_fast";

    @BeforeAll
    static void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://10.0.0.9:13306/ry_vqms"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
        ds.setUsername("root");
        ds.setPassword(java.util.Objects.requireNonNull(System.getenv("MYSQL_ROOT_PASSWORD"),
                "缺少 MYSQL_ROOT_PASSWORD"));

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/vqms/*Mapper.xml"));
        SqlSessionFactory sessionFactory = factoryBean.getObject();
        mapper = sessionFactory.getConfiguration().getMapper(VqmsJudgeParamMapper.class, sessionFactory.openSession());

        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration("10.0.0.9", 16379);
        redisConf.setPassword(System.getenv("VQMS_REDIS_PASSWORD"));
        redisFactory = new LettuceConnectionFactory(redisConf);
        redisFactory.afterPropertiesSet();
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisFactory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        FastJson2JsonRedisSerializer<Object> valueSerializer = new FastJson2JsonRedisSerializer<>(Object.class);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        RedisCache redisCache = new RedisCache();
        redisCache.redisTemplate = template;
        service = new VqmsJudgeParamService();
        inject(service, "judgeParamMapper", mapper);
        inject(service, "redisCache", redisCache);
        service.warmupCache();

        VqmsJudgeParamController controller = new VqmsJudgeParamController();
        inject(controller, "judgeParamService", service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterAll
    static void tearDown()
    {
        // 只归一测试自身可能触碰的 {3,4}；其余值（现场合法整定 1/2）保留不动——共享库保护
        try
        {
            VqmsJudgeParam tFast = mapper.selectByKey("t_fast");
            if (tFast != null && (tFast.getParamValue() == 3 || tFast.getParamValue() == 4))
            {
                VqmsJudgeParam restore = new VqmsJudgeParam();
                restore.setParamKey("t_fast");
                restore.setParamValue(4);
                service.update(restore);
            }
        }
        catch (Exception ignored)
        {
        }
        if (redisFactory != null)
        {
            redisFactory.destroy();
        }
    }

    private static void inject(Object target, String field, Object value) throws Exception
    {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void assert_种子四行在()
    {
        List<VqmsJudgeParam> list = service.selectList();
        Assertions.assertTrue(list.size() >= 4, "判定参数种子至少 4 行（自定义行允许），实际 " + list.size());
        Assertions.assertEquals(5, service.getInt("t_econ"));
        Assertions.assertEquals(1, service.getInt("tier_threshold_fast"));
        Assertions.assertEquals(5, service.getInt("tier_threshold_econ"));
    }

    @Test
    void assert_Service层拒绝非法t_fast()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> updateFast(0), "t_fast=0 低于值域");
        Assertions.assertThrows(IllegalArgumentException.class, () -> updateFast(5), "t_fast=5 越值域且 =t_econ");
        Assertions.assertThrows(IllegalArgumentException.class, () -> updateFast(6), "t_fast=6 越值域");
    }

    @Test
    void assert_Service层拒绝锁定行修改与必需参数删除()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> updateParam("t_econ", 6), "t_econ 锁定");
        Assertions.assertThrows(IllegalArgumentException.class, () -> updateParam("tier_threshold_fast", 2), "分档阈值锁定");
        Long tFastId = service.selectList().stream()
                .filter(p -> "t_fast".equals(p.getParamKey())).findFirst().orElseThrow().getParamId();
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteById(tFastId), "判定必需参数不可删");
    }

    /** 测试方案 §4.6 DB 段断言：绕过 Service 直改 DB 被 CHECK 拒 */
    @Test
    void assert_绕过Service直改DB被CHECK拒()
    {
        VqmsJudgeParam bypass = mapper.selectByKey("t_econ");
        bypass.setParamValue(6);
        Assertions.assertThrows(Exception.class, () -> mapper.update(bypass),
                "直改 t_econ=6 应被 ck_locked_rows 拒（MySQL ERROR 3819）");
    }

    /** 完成标准核心：改值后下一次 getInt 读取即新值（缓存刷新） */
    @Test
    void assert_缓存即时生效() throws Exception
    {
        int before = service.getInt("t_fast");
        Assertions.assertTrue(before == 3 || before == 4, "前置：t_fast 应为种子/还原值，实际 " + before);

        int target = before == 4 ? 3 : 4;
        updateFast(target);
        Assertions.assertEquals(target, service.getInt("t_fast"), "改值后 getInt 应立即读到新值");
        Object redisValue = redisTemplateForCheck().opsForValue().get(REDIS_KEY);
        Assertions.assertNotNull(redisValue, "缓存键应存在: " + REDIS_KEY);

        updateFast(before);
        Assertions.assertEquals(before, service.getInt("t_fast"), "还原后 getInt 读还原值");
    }

    @Test
    void assert_controller_CRUD契约与校验透传() throws Exception
    {
        mockMvc.perform(get("/vqms/judgeParam/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.rows.length()").value(4));

        mockMvc.perform(put("/vqms/judgeParam").contentType("application/json")
                .content("{\"paramKey\":\"t_econ\",\"paramValue\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        mockMvc.perform(put("/vqms/judgeParam").contentType("application/json")
                .content("{\"paramKey\":\"t_fast\",\"paramValue\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** blocker 修复回归锁：自定义键 insert→getInt→delete 回路（蕴含式 CHECK 放行新键） */
    @Test
    void assert_自定义键增删回路()
    {
        VqmsJudgeParam probe = new VqmsJudgeParam();
        probe.setParamKey("d7_it_probe");
        probe.setParamValue(7);
        probe.setName("IT 探针");
        probe.setValueMin(1);
        probe.setValueMax(10);
        Assertions.assertEquals(1, service.insert(probe));
        Assertions.assertEquals(7, service.getInt("d7_it_probe"));
        Long probeId = service.selectList().stream()
                .filter(p -> "d7_it_probe".equals(p.getParamKey())).findFirst().orElseThrow().getParamId();
        Assertions.assertEquals(1, service.deleteById(probeId));
    }

    /** 重复键 insert 友好拒绝（不裸抛 DuplicateKeyException） */
    @Test
    void assert_重复键友好拒绝()
    {
        VqmsJudgeParam dup = new VqmsJudgeParam();
        dup.setParamKey("t_fast");
        dup.setParamValue(2);
        dup.setName("重复键");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.insert(dup),
                "应报「参数键已存在」而非裸 DB 异常");
    }

    /** NULL 安全等值回归锁：原生 SQL 旁路直改 t_fast 值域列为 NULL 被 ck_locked_rows 拒（<=> 语义）。
     *  经 mapper 动态 SQL 置不了 NULL（if 判空跳列），须 JDBC 原生语句才是真旁路。 */
    @Test
    void assert_绕过Service置NULL值域被CHECK拒() throws Exception
    {
        java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:mysql://10.0.0.9:13306/ry_vqms?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8",
                "root", java.util.Objects.requireNonNull(System.getenv("MYSQL_ROOT_PASSWORD")));
        try (java.sql.Statement st = conn.createStatement())
        {
            Assertions.assertThrows(java.sql.SQLException.class,
                    () -> st.executeUpdate("update vqms_judge_param set value_min = null, value_max = null where param_key = 't_fast'"),
                    "值域列置 NULL 应被拒（普通 = 遇 NULL 求值 NULL 放行，须 <=>）");
        }
        finally
        {
            conn.close();
        }
    }

    private static void updateFast(int value)
    {
        updateParam("t_fast", value);
    }

    private static void updateParam(String key, int value)
    {
        VqmsJudgeParam patch = new VqmsJudgeParam();
        patch.setParamKey(key);
        patch.setParamValue(value);
        service.update(patch);
    }

    private static RedisTemplate<String, Object> redisTemplateForCheck() throws Exception
    {
        java.lang.reflect.Field f = VqmsJudgeParamService.class.getDeclaredField("redisCache");
        f.setAccessible(true);
        RedisCache cache = (RedisCache) f.get(service);
        return cache.redisTemplate;
    }
}
