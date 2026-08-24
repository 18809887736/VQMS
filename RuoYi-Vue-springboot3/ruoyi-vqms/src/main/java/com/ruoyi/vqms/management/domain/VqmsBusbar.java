package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS 主母线元数据。
 *
 * <p>对应 v5.0 §6.2.1，field v_grade 是母线属性，不建独立统计维度。</p>
 */
public class VqmsBusbar implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主母线编号，对齐 his_curve_sv.busbar_num */
    private Long busbarNum;

    /** 母线名称 */
    private String busbarName;

    /** 电压等级编码：0=500kV, 1=220kV, 2=66kV及以下(预留)，与字典 vqms_v_grade 严格对齐勿改 */
    private Integer vGrade;

    /** Jackson 默认会把 getVGrade 序列化为 "vgrade"，前端契约是 vGrade（v5.0 §9.3） */
    @com.fasterxml.jackson.annotation.JsonProperty("vGrade")
    public Integer getVGrade() { return vGrade; }

    /** 所属母线组（逻辑 FK → vqms_busbar_group.group_num） */
    /** 该母线 t0 实时电压 yc 点（增量指令算 V_target；NULL=未接入）【S4 Slice1】 */
    private Long realtimeYcNum;

    private Long groupNum;

    /** 标称电压 kV */
    private BigDecimal nominalKv;

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

    public Long getBusbarNum() { return busbarNum; }
    public void setBusbarNum(Long busbarNum) { this.busbarNum = busbarNum; }
    public String getBusbarName() { return busbarName; }
    public void setBusbarName(String busbarName) { this.busbarName = busbarName; }
    public void setVGrade(Integer vGrade) { this.vGrade = vGrade; }
    public Long getRealtimeYcNum() { return realtimeYcNum; }
    public void setRealtimeYcNum(Long realtimeYcNum) { this.realtimeYcNum = realtimeYcNum; }
    public Long getGroupNum() { return groupNum; }
    public void setGroupNum(Long groupNum) { this.groupNum = groupNum; }
    public BigDecimal getNominalKv() { return nominalKv; }
    public void setNominalKv(BigDecimal nominalKv) { this.nominalKv = nominalKv; }
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
