package com.ruoyi.vqms.statistics;

/**
 * 逐分钟投运状态四分类（正式 v1_0 §1.3/§1.4，测试方案 §5.0）。
 *
 * <p>投运率是时间记账维度（附件6 §一），与调节合格率两档平行无涉；
 * 投运率的免责 = 电网原因退出时间扣减，不适用 §二 免考。</p>
 */
public enum RuntimeMinuteState
{
    /** 并网且 AVC 投入——进分子也进分母 */
    IN_SERVICE,

    /** 并网但 AVC 退出、原因=电网——免责：从分母扣除（不计惩罚基数） */
    EXIT_GRID,

    /** 并网但 AVC 退出、原因=非电网（含矛盾/滞后态从严归入，见 Classifier 声明）——扣罚 */
    EXIT_NON_GRID,

    /** 未并网——不计入任何分钟（分子分母都不进） */
    OFFLINE
}
