package com.ruoyi.vqms.source.model;

/**
 * warn_info 一行（外部源只读领域对象）。
 *
 * <p>指令目标值/增量值编码在 {@code warn_content} 文本内，解码随搁置轨 judge 实现（v5.0 §8.1）。
 * 本对象只承载原始字段，不做任何解码。</p>
 */
public class WarnInfo
{
    /** 告警/指令时间原文（varchar，含毫秒） */
    private String warnTime;

    /** 毫秒原文 */
    private String millisecond;

    /** 类型；电压指令 = 5（遥调） */
    private Long warnType;

    /** 对象编号 */
    private Long objNum;

    /** 告警/指令描述原文（中文；目标值/增量值编码在此文本内） */
    private String warnContent;

    public String getWarnTime()
    {
        return warnTime;
    }

    public void setWarnTime(String warnTime)
    {
        this.warnTime = warnTime;
    }

    public String getMillisecond()
    {
        return millisecond;
    }

    public void setMillisecond(String millisecond)
    {
        this.millisecond = millisecond;
    }

    public Long getWarnType()
    {
        return warnType;
    }

    public void setWarnType(Long warnType)
    {
        this.warnType = warnType;
    }

    public Long getObjNum()
    {
        return objNum;
    }

    public void setObjNum(Long objNum)
    {
        this.objNum = objNum;
    }

    public String getWarnContent()
    {
        return warnContent;
    }

    public void setWarnContent(String warnContent)
    {
        this.warnContent = warnContent;
    }
}
