package com.ruoyi.vqms.source.model;

/**
 * yc_history 一行（外部源只读领域对象）。
 *
 * <p>时间列名是 {@code yc_time}（不是 save_time），见 v5.0 §5 三表列名速查。</p>
 */
public class YcHistory
{
    /** 遥测点编码 */
    private Long ycNum;

    /** 遥测时间原文（varchar，格式同 save_time） */
    private String ycTime;

    /** 遥测值 */
    private Double ycData;

    public Long getYcNum()
    {
        return ycNum;
    }

    public void setYcNum(Long ycNum)
    {
        this.ycNum = ycNum;
    }

    public String getYcTime()
    {
        return ycTime;
    }

    public void setYcTime(String ycTime)
    {
        this.ycTime = ycTime;
    }

    public Double getYcData()
    {
        return ycData;
    }

    public void setYcData(Double ycData)
    {
        this.ycData = ycData;
    }
}
