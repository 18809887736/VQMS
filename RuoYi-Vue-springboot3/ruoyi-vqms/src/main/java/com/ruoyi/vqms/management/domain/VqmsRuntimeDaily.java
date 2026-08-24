package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AVC 投运率日记账一行（vqms_runtime_daily；S4 RuntimePipeline 写入）。
 *
 * <p>率/缺额/罚款快照由 {@code RuntimeStatistics} 纯函数算出写回；
 * rate_pct 等 NULL = 零并网分钟（无可考核基数，非真 0%）。</p>
 */
public class VqmsRuntimeDaily implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Date statDate;
    private Integer inServiceMin;
    private Integer exitGridMin;
    private Integer exitNonGridMin;
    private Integer offlineMin;
    private BigDecimal ratedCapacityKw;
    private BigDecimal ratePct;
    private BigDecimal shortfallPct;
    private BigDecimal penaltyScore;

    public Date getStatDate() { return statDate; }
    public void setStatDate(Date statDate) { this.statDate = statDate; }
    public Integer getInServiceMin() { return inServiceMin; }
    public void setInServiceMin(Integer inServiceMin) { this.inServiceMin = inServiceMin; }
    public Integer getExitGridMin() { return exitGridMin; }
    public void setExitGridMin(Integer exitGridMin) { this.exitGridMin = exitGridMin; }
    public Integer getExitNonGridMin() { return exitNonGridMin; }
    public void setExitNonGridMin(Integer exitNonGridMin) { this.exitNonGridMin = exitNonGridMin; }
    public Integer getOfflineMin() { return offlineMin; }
    public void setOfflineMin(Integer offlineMin) { this.offlineMin = offlineMin; }
    public BigDecimal getRatedCapacityKw() { return ratedCapacityKw; }
    public void setRatedCapacityKw(BigDecimal ratedCapacityKw) { this.ratedCapacityKw = ratedCapacityKw; }
    public BigDecimal getRatePct() { return ratePct; }
    public void setRatePct(BigDecimal ratePct) { this.ratePct = ratePct; }
    public BigDecimal getShortfallPct() { return shortfallPct; }
    public void setShortfallPct(BigDecimal shortfallPct) { this.shortfallPct = shortfallPct; }
    public BigDecimal getPenaltyScore() { return penaltyScore; }
    public void setPenaltyScore(BigDecimal penaltyScore) { this.penaltyScore = penaltyScore; }
}
