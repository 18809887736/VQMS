package com.ruoyi.vqms.statistics;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 调节判定算法注册表 + 实现选择（v5.0 §8.8.5，2026-08-22 Leo 拍板）。
 *
 * <p>注册 ID = Spring bean 名；生效实现经 {@code application.yml} 部署级配置选择：</p>
 * <pre>{@code
 * vqms:
 *   judge:
 *     algorithm: V1_0    # V1_0=正式包络判定（S1）/ STUB=确定性占位（D9）
 * }</pre>
 *
 * <p>fail-fast 两道：① 配置指向未注册 ID → 启动失败；② 重复 ID = bean 重名，
 * Spring Boot 默认禁 bean 覆盖、启动失败。未配置亦 fail-fast（要求显式选择——
 * 防误配上线静默产出占位数字）。生效 ID 随启动日志记录（审计落点）。</p>
 *
 * <p>加变体 = 加一个类 + 一个 {@code @Bean} 注册行，调用方零改动。
 * 消费方注入 {@link RegulationJudge} 即得选中实现（{@code @Primary}）。</p>
 */
@Configuration
public class RegulationJudgeConfig
{
    private static final Logger log = LoggerFactory.getLogger(RegulationJudgeConfig.class);

    @Bean("STUB")
    public RegulationJudge stubJudge()
    {
        return new StubRegulationJudge();
    }

    @Bean("V1_0")
    public RegulationJudge defaultJudge()
    {
        return new DefaultRegulationJudge();
    }

    @Bean
    @Primary
    public RegulationJudge regulationJudge(Map<String, RegulationJudge> registry,
            @Value("${vqms.judge.algorithm:}") String algorithmId)
    {
        if (algorithmId == null || algorithmId.isBlank())
        {
            throw new IllegalStateException("vqms.judge.algorithm 未配置——须显式选择判定算法实现"
                    + "（已注册: " + registry.keySet() + "），拒绝静默默认");
        }
        RegulationJudge judge = registry.get(algorithmId);
        if (judge == null)
        {
            throw new IllegalStateException("vqms.judge.algorithm=" + algorithmId + " 未注册，启动中止"
                    + "（已注册: " + registry.keySet() + "）");
        }
        log.info("VQMS 调节判定算法生效: {} ({}, stub={})",
                algorithmId, judge.getClass().getSimpleName(), judge.isStub());
        return judge;
    }
}
