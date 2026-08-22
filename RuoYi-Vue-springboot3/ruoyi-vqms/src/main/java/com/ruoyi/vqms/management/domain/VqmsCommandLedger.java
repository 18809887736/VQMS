package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS AVC 指令流水账一行（原始事实，只增）。
 *
 * <p>对应 v5.0 §6.2.6——外部源 warn_info 电压指令（warn_type=5）的原始字段摘录，
 * <b>不含任何判定/解码结论</b>（undecodable 标志等随搁置轨解封后从本表原文重算）。
 * 存储切分铁律的唯一有界例外表。uk 幂等的生成列（millisecond_uk/obj_num_uk）由
 * DB 维护，应用不读写。</p>
 */
public class VqmsCommandLedger implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 指令时间原文（外部源 warn_info.warn_time，varchar 原样保留，格式校验在读取层） */
    private String warnTime;

    /** 毫秒原文 */
    private String millisecond;

    /** 类型；电压指令 = 5（遥调） */
    private Long warnType;

    /** 对象编号（现场整定；非 VQMS 管理表引用，不参与逻辑 FK 校验） */
    private Long objNum;

    /** 指令文本原文（目标值/增量值编码在此文本内；解码随搁置轨 judge 实现） */
    private String warnContent;

    /** 抓取入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fetchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWarnTime() { return warnTime; }
    public void setWarnTime(String warnTime) { this.warnTime = warnTime; }
    public String getMillisecond() { return millisecond; }
    public void setMillisecond(String millisecond) { this.millisecond = millisecond; }
    public Long getWarnType() { return warnType; }
    public void setWarnType(Long warnType) { this.warnType = warnType; }
    public Long getObjNum() { return objNum; }
    public void setObjNum(Long objNum) { this.objNum = objNum; }
    public String getWarnContent() { return warnContent; }
    public void setWarnContent(String warnContent) { this.warnContent = warnContent; }
    public Date getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Date fetchedAt) { this.fetchedAt = fetchedAt; }
}
