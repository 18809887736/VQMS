package com.ruoyi.vqms.statistics;

/**
 * 判定输入：窗口内逐分钟曲线一行（主母线，已时间对齐）。
 *
 * <p>minuteOffset = 相对 t₀ 的分钟偏移（1..t_econ）；缺分钟由调用方不传（缺失即如实进
 * completeness / invalidTiers，v5.0 §8.2「缺分钟由调用方以缺省标记传入」的落地 = 不传）。
 * high/low 为窗口观测极值 kV（判定用）；average_SV / plan_SV 废值不上浮到此层。</p>
 */
public record MinuteCurve(int minuteOffset, int highSv, int lowSv)
{
}
