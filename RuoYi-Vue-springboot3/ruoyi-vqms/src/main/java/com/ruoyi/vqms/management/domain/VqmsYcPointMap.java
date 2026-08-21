package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS yc_history 遥测点语义映射。
 *
 * <p>对应 v5.0 §6.2.3，point_type 区分 busbar_id / voltage / yx 三类。</p>
 */
public class VqmsYcPointMap implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 遥测点编码，对齐 yc_history.yc_num */
    private Long ycNum;

    /** 语义名称 */
    private String pointName;

    /** busbar_id=主母线号 / voltage=电压模拟量 / yx=开关量 */
    private String pointType;

    /** 单位（yc 模拟量） */
    private String unit;

    /** yx 点值=1 的语义 */
    private String state1Label;

    /** yx 点值=0 的语义 */
    private String state0Label;

    /** 该 yx 点是否启用为考核门控：1=启用 / 0=不参与 */
    private Integer gateEnabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getYcNum() { return ycNum; }
    public void setYcNum(Long ycNum) { this.ycNum = ycNum; }
    public String getPointName() { return pointName; }
    public void setPointName(String pointName) { this.pointName = pointName; }
    public String getPointType() { return pointType; }
    public void setPointType(String pointType) { this.pointType = pointType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getState1Label() { return state1Label; }
    public void setState1Label(String state1Label) { this.state1Label = state1Label; }
    public String getState0Label() { return state0Label; }
    public void setState0Label(String state0Label) { this.state0Label = state0Label; }
    public Integer getGateEnabled() { return gateEnabled; }
    public void setGateEnabled(Integer gateEnabled) { this.gateEnabled = gateEnabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
