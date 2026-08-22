package com.ruoyi.vqms.management.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * VQMS 数据不可用策略参数（v5.0 §8.7，D9 骨架）。
 *
 * <p>param_value 为 varchar（字符串枚举如 EXCLUDE_REPORTED / 整数文本如 50）。
 * <b>选套值留空待政策拍板</b>——整表零种子行、不预设处置值；拍板后写几行即换策略，
 * 代码不动。约定键：undecodable_mode / invalid_tier_mode / partial_missing_mode /
 * partial_missing_threshold_pct。无 CRUD UI（D9 范围拍板），随选套定稿按 D7 同款补。</p>
 */
public class VqmsPolicyParam implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long paramId;

    /** 参数键 */
    private String paramKey;

    /** 参数值（字符串枚举/整数文本；选套前为 null） */
    private String paramValue;

    /** 参数名 */
    private String name;

    /** 说明 */
    private String description;

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

    public Long getParamId() { return paramId; }
    public void setParamId(Long paramId) { this.paramId = paramId; }
    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
