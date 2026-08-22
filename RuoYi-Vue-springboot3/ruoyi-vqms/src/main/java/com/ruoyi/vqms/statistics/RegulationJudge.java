package com.ruoyi.vqms.statistics;

import java.util.List;

/**
 * AVC 指令两档平行判定接口（阶段二：纯包络判定，不含门控/免考/策略处置）。
 *
 * <p>v5.0 §8.2 三阶段管线（Leo 2026-08-14 拍板）：① 门控前置过滤（调用方做，不进 judge）→
 * ② 本接口包络判定 → ③ 免考后置应用（读 yx501，非判定产物）。接口输入/输出契约稳定——
 * 算法定型后只换实现（{@code StubRegulationJudge} → 真实现）、不动调用方。</p>
 *
 * <p>解码归属：V_target 解码留在实现内部，调用方不预算——解码规则变更不波及调用方。</p>
 */
public interface RegulationJudge
{
    /**
     * 对一条 AVC 指令做两档平行判定。
     *
     * @param cmd           指令（warn_info 原始字段 + t₀ 实时母线电压）
     * @param curveByMinute 窗口内逐分钟 (high_SV, low_SV)，已时间对齐；缺分钟不传
     * @param params        整定参数（t_fast / t_econ）
     * @return {@link RegulationOutcome}：Judged（两档结论 + completeness + invalidTiers）
     *         或 Undecodable（解码失败，带原因分类）
     */
    RegulationOutcome judge(AvcCommand cmd, List<MinuteCurve> curveByMinute, JudgeParams params);

    /**
     * stub 标记：占位实现返回 true（v5.0 §8.2「返回值带 stub=true 标记」）——
     * 上层可感知当前结论出自占位实现、不保证正确。
     */
    default boolean isStub()
    {
        return false;
    }
}
