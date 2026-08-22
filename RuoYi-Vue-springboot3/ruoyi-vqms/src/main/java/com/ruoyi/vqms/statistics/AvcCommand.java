package com.ruoyi.vqms.statistics;

/**
 * 判定输入：一条 AVC 指令（warn_info 原始字段 + t₀ 实时母线电压）。
 *
 * <p>V_target 解码留在 judge 实现内部（v5.0 §8.2），调用方不预算——
 * 本对象只搬运原文与采集值，不做任何解码。plan_SV/average_SV 废值不进此对象（Leo 2026-08-14 定）。</p>
 */
public record AvcCommand(
        /** 指令时间原文（warn_info.warn_time，varchar 原样） */
        String warnTime,

        /** 毫秒原文 */
        String millisecond,

        /** 对象编号（分通道用） */
        Long objNum,

        /** 指令文本原文（目标值/增量值编码在此文本内） */
        String warnContent,

        /** t₀ 时刻实时母线电压 kV（增量形态算 V_target 用；目标值形态不用；缺失传 null） */
        Double t0RealtimeVoltageKv)
{
}
