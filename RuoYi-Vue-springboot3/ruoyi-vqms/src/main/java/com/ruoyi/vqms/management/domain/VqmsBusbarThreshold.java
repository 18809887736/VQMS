package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;

/**
 * VQMS 母线电压合格阈值（带生效区间）。
 *
 * <p>对应 v5.0 §6.2.4。tolerance_v 占位/角色待定，禁用旧 |average_SV−plan_SV|≤tolerance_v 口径。</p>
 */
public class VqmsBusbarThreshold implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long thresholdId;

    /** 母线编号（逻辑 FK → vqms_busbar.busbar_num） */
    @Excel(name = "母线编号")
    private Long busbarNum;

    /** 口径：AVC=控制达标率 / GB=国标±10% */
    @Excel(name = "口径", readConverterExp = "AVC=AVC 控制达标率,GB=国标±10%")
    private String criterionType;

    /** AVC 容差(kV)：220kV=1.000, 500kV=1.500；GB 口径为空 */
    @Excel(name = "容差(kV)")
    private BigDecimal toleranceV;

    /** plan_SV 废值策略：SKIP/COUNT_UNQUALIFIED/FALLBACK。⚠️旧模型遗留列，暂无消费方 */
    private String planSvInvalidPolicy;

    /** 生效起始日（含） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生效起始日", dateFormat = "yyyy-MM-dd")
    private Date effectiveFrom;

    /** 生效结束日（含），null=至今有效 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生效结束日", dateFormat = "yyyy-MM-dd")
    private Date effectiveTo;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    public Long getThresholdId() { return thresholdId; }
    public void setThresholdId(Long thresholdId) { this.thresholdId = thresholdId; }
    public Long getBusbarNum() { return busbarNum; }
    public void setBusbarNum(Long busbarNum) { this.busbarNum = busbarNum; }
    public String getCriterionType() { return criterionType; }
    public void setCriterionType(String criterionType) { this.criterionType = criterionType; }
    public BigDecimal getToleranceV() { return toleranceV; }
    public void setToleranceV(BigDecimal toleranceV) { this.toleranceV = toleranceV; }
    public String getPlanSvInvalidPolicy() { return planSvInvalidPolicy; }
    public void setPlanSvInvalidPolicy(String planSvInvalidPolicy) { this.planSvInvalidPolicy = planSvInvalidPolicy; }
    public Date getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Date effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Date getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Date effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
