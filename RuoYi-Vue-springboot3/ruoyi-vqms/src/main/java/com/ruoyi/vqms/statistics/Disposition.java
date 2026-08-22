package com.ruoyi.vqms.statistics;

/**
 * 数据不可用处置桶（策略评估纯函数的唯一输出，草稿 v5_0 §2.9）。
 *
 * <p>任一 {@link RegulationOutcome} 在任一配置下<b>恰好落一个桶</b>（不重不漏）。
 * 留痕底线（无争议）：剔除/计不合格/挂起均须上报计数，不静默——计数由调用方做，
 * 本枚举只表达处置语义。</p>
 */
public enum Disposition
{
    /** 正常记账：判定结论进分子、指令进分母（甲档部分缺"用剩余"即此） */
    COUNT_NORMAL,

    /** 剔除分母 + 必上报计数（乙档阈值剔除 / 整窗缺剔除）；合规风险项——选套待政策拍板 */
    EXCLUDE_REPORTED,

    /** 计不合格 + 必上报计数（丙档：数据不可用判的不合格，区分真不合格） */
    COUNT_UNQUALIFIED,

    /** 标记挂起人工后审（丁档透明标记；统计表标记列随 S2 解封同批设计） */
    PEND_MARKED
}
