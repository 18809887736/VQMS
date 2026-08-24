package com.ruoyi.vqms.statistics;

/**
 * 免考应用后的逐档最终记账状态（正式 v1_0 §2.6 apply_exemption / §2.7 统计输入）。
 *
 * <p>{@link #INVALID} = 该档不可判——两个来源：{@code Judged.invalidTiers} 档透传
 * （{@link ExemptionApplier}）与 {@code Undecodable} 整指令由管线合成的占位
 * （无 Judged 可走免考）。按现行固定分母拍板口径（正式版 §2.7）：计入分母、
 * 拖低披露合格率；其记账细化（剔除/计不合格/挂起）归参数化策略层，
 * 选套落地前不在统计内分支。</p>
 */
public enum FinalTierState
{
    QUALIFIED,

    /** 未夹住且未免考——真罚，进罚款缺额分子 */
    PENALIZED,

    /** PENALIZED 且 yx501=1——免于考核：不进分子、不剔分母（正式版 §2.7 拍板） */
    EXEMPTED,

    /** 该档不可判（invalidTiers 透传 / Undecodable 占位）——计入分母、拖低披露合格率 */
    INVALID
}
