package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS 母线组（主母线判定单元）。
 *
 * <p>对应 v5.0 §6.2.2，逻辑上与外部库 QHeatAvcRtdb.BUSBAR_GROUP（废表）无关。</p>
 */
public class VqmsBusbarGroup implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 母线组编号 */
    private Long groupNum;

    /** 组名 */
    private String groupName;

    /** 电压等级编码，同 vqms_busbar.v_grade */
    private Integer vGrade;

    /** 该组"当前主母线号"指示点，对齐 yc_history.yc_num；未接入前为 null */
    private Long mainIndicatorYcNum;

    /** 指示点不可用时的兜底主母线号；null=不兜底 */
    private Long defaultMainBusbarNum;

    /** 指示点陈旧窗口（分钟） */
    private Integer maxStalenessMinutes;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getGroupNum() { return groupNum; }
    public void setGroupNum(Long groupNum) { this.groupNum = groupNum; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    /** Jackson 默认会把 getVGrade 序列化为 "vgrade"，前端契约是 vGrade（v5.0 §9.3） */
    @com.fasterxml.jackson.annotation.JsonProperty("vGrade")
    public Integer getVGrade() { return vGrade; }
    public void setVGrade(Integer vGrade) { this.vGrade = vGrade; }
    public Long getMainIndicatorYcNum() { return mainIndicatorYcNum; }
    public void setMainIndicatorYcNum(Long mainIndicatorYcNum) { this.mainIndicatorYcNum = mainIndicatorYcNum; }
    public Long getDefaultMainBusbarNum() { return defaultMainBusbarNum; }
    public void setDefaultMainBusbarNum(Long defaultMainBusbarNum) { this.defaultMainBusbarNum = defaultMainBusbarNum; }
    public Integer getMaxStalenessMinutes() { return maxStalenessMinutes; }
    public void setMaxStalenessMinutes(Integer maxStalenessMinutes) { this.maxStalenessMinutes = maxStalenessMinutes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
