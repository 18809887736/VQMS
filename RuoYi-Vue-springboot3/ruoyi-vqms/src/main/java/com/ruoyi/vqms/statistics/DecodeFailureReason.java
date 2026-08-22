package com.ruoyi.vqms.statistics;

/**
 * 解码失败原因三分类（草稿 v5_0 §2.5 / §2.8；vqms_command_ledger 流水重算的归因口径）。
 */
public enum DecodeFailureReason
{
    /** 增量编码第 2 位循环码 ∉ {0..5}（Leo 2026-08-19 告知值域；轮转规律待生产数据实证） */
    CYCLE_CODE_INVALID,

    /** 增量形态缺 t₀ 时刻实时母线电压（V_target = 实时 ± 幅值，缺被加数判不了） */
    MISSING_T0_VOLTAGE,

    /** 编码脏写：文本结构烂（无尾码/长度不对/非数字槽位/方向非法），不属前两类的编码侧异常 */
    CORRUPTED_ENCODING
}
