package com.ruoyi.vqms.statistics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * S3 L0：RuntimeMinuteClassifier 真值表（测试方案 §5.0，正式 v1_0 §1.3/§1.4）。
 *
 * <p>并网 × 投退 × 退出原因全矩阵（2×2×3=12 行）：未并网不计任何分钟；
 * 并网 ∧ avc_in=1 → 投运；并网 ∧ avc_in=0 → 按原因分流（1=电网免责 / 2=非电网扣罚）。</p>
 */
class RuntimeMinuteClassifierTruthTableTest
{
    @ParameterizedTest
    @CsvSource({
            // 未并网：投退/原因任意组合一律 OFFLINE（6 行——不计任何分钟）
            "false, 0, 0, OFFLINE",
            "false, 0, 1, OFFLINE",
            "false, 0, 2, OFFLINE",
            "false, 1, 0, OFFLINE",
            "false, 1, 1, OFFLINE",
            "false, 1, 2, OFFLINE",
            // 并网 ∧ AVC 投入：原因不参与
            "true, 1, 0, IN_SERVICE",
            "true, 1, 1, IN_SERVICE",
            "true, 1, 2, IN_SERVICE",
            // 并网 ∧ AVC 退出：按退出原因分流
            "true, 0, 1, EXIT_GRID",
            "true, 0, 2, EXIT_NON_GRID",
            // ⚠ 矛盾/滞后态（已退但原因=未退）从严归扣罚——免责须正向证据，实现决策见 Classifier 声明
            "true, 0, 0, EXIT_NON_GRID",
    })
    void truthTable_fullMatrix(boolean onGrid, int avcIn, int exitReason,
            RuntimeMinuteState expected)
    {
        Assertions.assertEquals(expected, RuntimeMinuteClassifier.classify(onGrid, avcIn, exitReason));
    }

    @Test
    void avcIn_outOfDomain_throws()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeMinuteClassifier.classify(true, 2, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeMinuteClassifier.classify(true, -1, 0));
    }

    @Test
    void exitReason_outOfDomain_throws()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeMinuteClassifier.classify(true, 0, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeMinuteClassifier.classify(true, 0, -1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuntimeMinuteClassifier.classify(false, 1, 9),
                "OFFLINE 短路前仍先校验值域——垃圾输入不放行");
    }
}
