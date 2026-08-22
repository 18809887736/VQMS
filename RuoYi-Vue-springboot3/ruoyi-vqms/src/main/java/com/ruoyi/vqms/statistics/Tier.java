package com.ruoyi.vqms.statistics;

/**
 * 两档判定档位（草稿 v5_0 §2.4，两档平行考察、互不隶属、不 fall-through）。
 */
public enum Tier
{
    /** 快速性档：窗口 [1, t_fast]，罚调得慢（动态性能） */
    FAST,

    /** 经济性档：窗口 [t_fast+1, t_econ]，罚持续越限（经济代价） */
    ECON
}
