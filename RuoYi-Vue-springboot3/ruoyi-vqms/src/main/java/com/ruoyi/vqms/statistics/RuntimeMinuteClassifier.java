package com.ruoyi.vqms.statistics;

/**
 * 逐分钟投运状态分类器（正式 v1_0 §1.3/§1.4）——纯函数。
 *
 * <p>判定树：未并网 → {@code OFFLINE}；并网 ∧ avc_in=1 → {@code IN_SERVICE}；
 * 并网 ∧ avc_in=0 → 按退出原因分流：1=电网（免责）/ 2=非电网（扣罚）。</p>
 *
 * <p><b>⚠️ 矛盾态从严映射（实现决策，随真实数据核对复核）</b>：avc 已退而退出原因仍
 * =「未退(0)」（对端 JS 引擎滞后 / 未部署的矛盾或滞后态）→ 归 {@code EXIT_NON_GRID}
 * 计罚——免责须有正向电网原因证据，缺证据不免责。信号合理性校验同批分轨待定
 * （v5.0 §14-8 / CLAUDE.md Open follow-ups），届时复核本映射。</p>
 *
 * <p>信号缺失处理不在本组件——{@code YxSignalReader} 返回 empty 时由调用方决定喂入值
 * （阶跃保持语义取 ≤ 当分钟最近一条后仍无数据属采集侧异常，归策略层族）。</p>
 */
public final class RuntimeMinuteClassifier
{
    private RuntimeMinuteClassifier()
    {
    }

    /**
     * @param onGrid     该分钟并网主体是否并网运行（并网点 yc511/512 保持值 ≥10 由调用方判）
     * @param avcIn      AVC 投退点保持值（合成 yc3009 / 真实候选 yx1001），合法域 {0,1}
     * @param exitReason 退出原因点保持值（yc521/522），合法域 {0,1,2}
     */
    public static RuntimeMinuteState classify(boolean onGrid, int avcIn, int exitReason)
    {
        if (avcIn != 0 && avcIn != 1)
        {
            throw new IllegalArgumentException("avcIn 合法域 {0,1}: " + avcIn);
        }
        if (exitReason != 0 && exitReason != 1 && exitReason != 2)
        {
            throw new IllegalArgumentException("exitReason 合法域 {0,1,2}: " + exitReason);
        }
        if (!onGrid)
        {
            return RuntimeMinuteState.OFFLINE;
        }
        if (avcIn == 1)
        {
            return RuntimeMinuteState.IN_SERVICE;
        }
        return exitReason == 1 ? RuntimeMinuteState.EXIT_GRID : RuntimeMinuteState.EXIT_NON_GRID;
    }
}
