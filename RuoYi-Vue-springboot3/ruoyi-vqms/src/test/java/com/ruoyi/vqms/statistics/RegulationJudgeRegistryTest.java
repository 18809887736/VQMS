package com.ruoyi.vqms.statistics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S1：算法注册表选择机制（v5.0 §8.8.5；测试方案 §5.0）。
 *
 * <p>V1_0/STUB 按配置切换、未知 ID / 重复 ID / 未配置启动 fail-fast、
 * {@code @Primary} 解析即选中实现。生效 ID 进启动日志（RegulationJudgeConfig log.info）。</p>
 */
class RegulationJudgeRegistryTest
{
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(RegulationJudgeConfig.class);

    @Test
    void configuredV1_0_selectsDefaultJudge()
    {
        runner.withPropertyValues("vqms.judge.algorithm=V1_0").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(RegulationJudge.class)).isInstanceOf(DefaultRegulationJudge.class);
        });
    }

    @Test
    void configuredSTUB_selectsStubJudge()
    {
        runner.withPropertyValues("vqms.judge.algorithm=STUB").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            RegulationJudge active = ctx.getBean(RegulationJudge.class);
            assertThat(active).isInstanceOf(StubRegulationJudge.class);
            assertThat(active.isStub()).isTrue();
        });
    }

    @Test
    void unknownId_failsFast()
    {
        runner.withPropertyValues("vqms.judge.algorithm=NOPE").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasMessageContaining("未注册");
        });
    }

    @Test
    void unset_failsFast()
    {
        // S1 交付后未配置 = fail-fast（要求显式选择，防静默产出占位数字）
        runner.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasMessageContaining("未配置");
        });
    }

    @Test
    void duplicateId_failsFast()
    {
        // 重复注册 ID = bean 重名——Spring Boot 默认禁 bean 定义覆盖，启动失败
        runner.withBean(DuplicateConfig.class)
                .withPropertyValues("vqms.judge.algorithm=V1_0").run(ctx -> {
                    assertThat(ctx).hasFailed();
                    String failure = String.valueOf(ctx.getStartupFailure());
                    assertThat(failure).containsAnyOf("V1_0", "Override", "覆盖");
                });
    }

    /** 第二个 "V1_0" 注册——重复 ID 场景的构造器。 */
    @Configuration
    static class DuplicateConfig
    {
        @Bean("V1_0")
        public RegulationJudge anotherDefault()
        {
            return new DefaultRegulationJudge();
        }
    }
}
