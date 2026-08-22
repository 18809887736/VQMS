package com.ruoyi.vqms.statistics;

/**
 * 两态判定结论（正式 v1_0 §2.5）——<b>不含 EXEMPT</b>：免考是阶段三后置读 yx501 的应用结果，
 * 不是判定产物（v5.0 §8.2 三阶段管线，Leo 2026-08-14 拍板）。
 */
public enum Verdict
{
    /** 综合区间夹住 V_target（L ≤ V_target ≤ H） */
    QUALIFIED,

    /** 未夹住——罚不罚由阶段三读 yx501 后定 */
    PENALIZED
}
