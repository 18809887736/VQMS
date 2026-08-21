package com.ruoyi.vqms.source.model;

import java.math.BigDecimal;

/**
 * his_curve_sv 一行（外部源只读领域对象）。
 *
 * <p>按 v5.0 §8.1：`high_SV`/`low_SV` 为判定用观测极值；
 * `average_SV`/`plan_SV` 为废值，<b>不映射</b>（编译期即不存在）。</p>
 */
public class HisCurveSv
{
    /** 采集时间原文（varchar，含毫秒，无时区；解析/取整在读取层） */
    private String saveTime;

    /** 母线编号 */
    private Long busbarNum;

    /** 窗口内电压最高观测值（kV） */
    private BigDecimal highSV;

    /** 窗口内电压最低观测值（kV） */
    private BigDecimal lowSV;

    public String getSaveTime()
    {
        return saveTime;
    }

    public void setSaveTime(String saveTime)
    {
        this.saveTime = saveTime;
    }

    public Long getBusbarNum()
    {
        return busbarNum;
    }

    public void setBusbarNum(Long busbarNum)
    {
        this.busbarNum = busbarNum;
    }

    public BigDecimal getHighSV()
    {
        return highSV;
    }

    public void setHighSV(BigDecimal highSV)
    {
        this.highSV = highSV;
    }

    public BigDecimal getLowSV()
    {
        return lowSV;
    }

    public void setLowSV(BigDecimal lowSV)
    {
        this.lowSV = lowSV;
    }
}
