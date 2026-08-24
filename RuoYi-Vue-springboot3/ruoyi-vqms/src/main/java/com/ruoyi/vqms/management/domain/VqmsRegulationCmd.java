package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 调节合格率指令级明细一行（vqms_regulation_cmd；S2 DDL / S4 Pipeline 写入）。
 *
 * <p>两档最终记账 {@code FinalTierState} 字符串直存；obj_num_uk 为生成列、写入方不提供。</p>
 */
public class VqmsRegulationCmd implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Date statDate;
    private String warnTime;
    private String millisecond;
    private Long objNum;
    private String algorithmId;
    private Integer tFastSnapshot;
    private String fastState;
    private String econState;
    private BigDecimal completeness;
    private String invalidTiers;
    private String undecodableReason;
    private Integer yx501Fast;
    private Integer yx501Econ;
    private String disposition;
    private Date fetchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getStatDate() { return statDate; }
    public void setStatDate(Date statDate) { this.statDate = statDate; }
    public String getWarnTime() { return warnTime; }
    public void setWarnTime(String warnTime) { this.warnTime = warnTime; }
    public String getMillisecond() { return millisecond; }
    public void setMillisecond(String millisecond) { this.millisecond = millisecond; }
    public Long getObjNum() { return objNum; }
    public void setObjNum(Long objNum) { this.objNum = objNum; }
    public String getAlgorithmId() { return algorithmId; }
    public void setAlgorithmId(String algorithmId) { this.algorithmId = algorithmId; }
    public Integer gettFastSnapshot() { return tFastSnapshot; }
    public void settFastSnapshot(Integer tFastSnapshot) { this.tFastSnapshot = tFastSnapshot; }
    public String getFastState() { return fastState; }
    public void setFastState(String fastState) { this.fastState = fastState; }
    public String getEconState() { return econState; }
    public void setEconState(String econState) { this.econState = econState; }
    public BigDecimal getCompleteness() { return completeness; }
    public void setCompleteness(BigDecimal completeness) { this.completeness = completeness; }
    public String getInvalidTiers() { return invalidTiers; }
    public void setInvalidTiers(String invalidTiers) { this.invalidTiers = invalidTiers; }
    public String getUndecodableReason() { return undecodableReason; }
    public void setUndecodableReason(String undecodableReason) { this.undecodableReason = undecodableReason; }
    public Integer getYx501Fast() { return yx501Fast; }
    public void setYx501Fast(Integer yx501Fast) { this.yx501Fast = yx501Fast; }
    public Integer getYx501Econ() { return yx501Econ; }
    public void setYx501Econ(Integer yx501Econ) { this.yx501Econ = yx501Econ; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public Date getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Date fetchedAt) { this.fetchedAt = fetchedAt; }
}
