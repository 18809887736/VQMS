package com.ruoyi.vqms.statistics;

/**
 * 戊·自由组合原子条件（策略文档 §3.3.1，2026-08-25 草案落地）。
 *
 * <p>甲/乙/丙/丁预设内部隐含判定条件的原子化拆解——每条可独立判真判假，
 * 互不依赖（依赖关系见各常量注记，求值时由调用方保证前置语义）。</p>
 *
 * <p><b>A5（免考旗读取失败）刻意缺席</b>：它发生于阶段三免考后置、动的是结论覆写
 * 而非记账桶（§3.3.2 对接口径），随 §6.5 拍板另设阶段三子规则表；本枚举出现 A5
 * 即方向性错误。解析器对 A5 显式拒绝并提示。</p>
 */
public enum PolicyAtom
{
    /** A1 解码失败（总）：产物为 Undecodable（A1 成立时 A2/A3/A4 无从评估——管线短路） */
    A1 {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return outcome instanceof RegulationOutcome.Undecodable;
        }
    },
    /** A1a 解码失败·编码脏写；与 A1b/A1c 互斥（DecodeFailureReason MECE 三分），合取 ≡ A1 */
    A1A {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return undecodableWith(outcome, DecodeFailureReason.CORRUPTED_ENCODING);
        }
    },
    /** A1b 解码失败·循环码非法 */
    A1B {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return undecodableWith(outcome, DecodeFailureReason.CYCLE_CODE_INVALID);
        }
    },
    /** A1c 解码失败·缺 t₀ 实时电压 */
    A1C {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return undecodableWith(outcome, DecodeFailureReason.MISSING_T0_VOLTAGE);
        }
    },
    /** A2 档不可判：Judged 且 invalidTiers 非空；与 A3 跨档可并存 */
    A2 {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return judgedOf(outcome).map(j -> !j.invalidTiers().isEmpty()).orElse(false);
        }
    },
    /** A3 窗口数据不完整（部分缺）：Judged 且 completeness &lt; 1；A4 仅在其下有意义 */
    A3 {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return judgedOf(outcome).map(j -> j.completeness() < 1.0).orElse(false);
        }
    },
    /**
     * A4 可用度低于阈值 τ：completeness×100 &lt; τ（边界 =τ 不触发剔除，承现行乙档
     * 「≥阈值=正常记账」实现方向 ⚠ UNVERIFIED，随政策钉死）；依赖 A3（不成立则恒假）。
     */
    A4 {
        @Override
        public boolean eval(RegulationOutcome outcome, int thresholdPct)
        {
            return judgedOf(outcome)
                    .map(j -> j.completeness() < 1.0 && j.completeness() * 100.0 < thresholdPct)
                    .orElse(false);
        }
    };

    abstract boolean eval(RegulationOutcome outcome, int thresholdPct);

    private static boolean undecodableWith(RegulationOutcome outcome, DecodeFailureReason reason)
    {
        return outcome instanceof RegulationOutcome.Undecodable u && u.reason() == reason;
    }

    private static java.util.Optional<RegulationOutcome.Judged> judgedOf(RegulationOutcome outcome)
    {
        return outcome instanceof RegulationOutcome.Judged j
                ? java.util.Optional.of(j) : java.util.Optional.empty();
    }

    /** 事实轴归属（等价规约用）：A1 族=解码失败轴，A2=档无效轴，A3/A4=部分缺轴 */
    public Axis axis()
    {
        return switch (this)
        {
            case A1, A1A, A1B, A1C -> Axis.UNDECODABLE;
            case A2 -> Axis.INVALID_TIER;
            case A3, A4 -> Axis.PARTIAL_MISSING;
        };
    }

    /** 事实轴（与 PolicyConfig 三模式键一一对应） */
    public enum Axis
    {
        UNDECODABLE, INVALID_TIER, PARTIAL_MISSING
    }
}
