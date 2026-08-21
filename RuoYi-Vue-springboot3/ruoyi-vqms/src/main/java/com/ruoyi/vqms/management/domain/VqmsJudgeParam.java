package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS 判定整定参数。
 *
 * <p>对应 v5.0 §6.2.5。t_fast∈[1,5) 整数自由整定、t_econ=5 写死锁定。
 * Redis 缓存 key: vqms:judgeParam:{key}。</p>
 */
public class VqmsJudgeParam implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long paramId;

    /** 参数键，如 t_fast / t_econ / tier_threshold_fast */
    private String paramKey;

    /** 参数值（分钟数） */
    private Integer paramValue;

    /** 参数名称 */
    private String name;

    /** 说明 */
    private String description;

    /** 值域下限（含） */
    private Integer valueMin;

    /** 值域上限（含） */
    private Integer valueMax;

    /** 状态：0=正常, 1=停用 */
    private String status;

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

    public Long getParamId() { return paramId; }
    public void setParamId(Long paramId) { this.paramId = paramId; }
    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public Integer getParamValue() { return paramValue; }
    public void setParamValue(Integer paramValue) { this.paramValue = paramValue; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getValueMin() { return valueMin; }
    public void setValueMin(Integer valueMin) { this.valueMin = valueMin; }
    public Integer getValueMax() { return valueMax; }
    public void setValueMax(Integer valueMax) { this.valueMax = valueMax; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
