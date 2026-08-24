package com.ruoyi.vqms.statistics;

import java.util.List;
import java.util.Objects;

/**
 * 调节合格率统计（正式 v1_0 §2.7：两档平行 · 附件6 原始分母）——纯函数。
 *
 * <p><b>口径（2026-08-19 Leo 拍板）</b>：分母 = 发令总次数（进入统计流的指令数，
 * 由调用方保证口径），两档同分母、分子各异。免考点不进分子、<b>不剔分母</b>；
 * 不可判档（INVALID）计入分母、拖低披露合格率。</p>
 *
 * <p><b>两条独立链路（⚠️ 推荐读法，待 Leo 确认——正式版 §2.7）</b>：
 * 「披露合格率」= 合格点数 ÷ 发令总次数（全量分母）；「罚款缺额」= 非免考不合格点数 ÷
 * 发令总次数（剔免考落在罚金层）。两链互不可推导；若拍板否掉推荐读法，罚款缺额
 * 改全量缺额单链（100 − 披露合格率），相关断言随之改（测试方案 §5.0 已标）。</p>
 *
 * <p>数据不可用处置（剔除/计不合格/挂起）选套落地前不在本函数分支——现行固定分母
 * 拍板即全部语义；策略细化随 S5 落地时扩展输入契约。</p>
 */
public final class RegulationStatistics
{
    /** 单位换算：容量 kW → 万千瓦（考核单价按万千瓦计，CLAUDE.md 单位规约容量存 kW） */
    private static final double KW_PER_WAN_KW = 10_000.0;

    /** 考核单价：每缺 1 个百分点 = 额定容量 × 0.02 分/万千瓦（附件6，线性非分档） */
    private static final double SCORE_PER_POINT_PER_WAN_KW = 0.02;

    private RegulationStatistics()
    {
    }

    /**
     * 单档统计结果。计数口径：qualified + penalized + exempted + invalid = totalCommands。
     */
    public record TierRateResult(int totalCommands, int qualifiedCount, int penalizedCount,
            int exemptCount, int invalidCount,
            double disclosedRatePct, double penaltyShortfallPct, double penaltyScore)
    {
    }

    /**
     * 两档平行汇总（不合并合格率）；{@code totalPenaltyScore} = 快速性罚款 + 经济性罚款
     * （正式版 §2.7「总览」）。单位：罚款为考核分。
     */
    public record Summary(TierRateResult fast, TierRateResult econ, double totalPenaltyScore)
    {
    }

    /**
     * @param records          进入统计流的逐指令最终记账（分母 = 其数量）
     * @param ratedCapacityKw  额定容量（kW，项目单位规约），≥ 0
     */
    public static Summary summarize(List<TierFinalDisposition> records, double ratedCapacityKw)
    {
        Objects.requireNonNull(records, "records 不可为 null");
        if (Double.isNaN(ratedCapacityKw) || ratedCapacityKw < 0)
        {
            throw new IllegalArgumentException("额定容量须 ≥ 0 kW: " + ratedCapacityKw);
        }

        TierRateResult fast = tier(records, TierFinalDisposition::fast, ratedCapacityKw);
        TierRateResult econ = tier(records, TierFinalDisposition::econ, ratedCapacityKw);
        return new Summary(fast, econ, fast.penaltyScore() + econ.penaltyScore());
    }

    private interface StateGetter
    {
        FinalTierState get(TierFinalDisposition d);
    }

    private static TierRateResult tier(List<TierFinalDisposition> records, StateGetter state,
            double ratedCapacityKw)
    {
        int total = records.size();
        int qualified = 0;
        int penalized = 0;
        int exempt = 0;
        int invalid = 0;
        for (TierFinalDisposition d : records)
        {
            switch (state.get(d))
            {
                case QUALIFIED -> qualified++;
                case PENALIZED -> penalized++;
                case EXEMPTED -> exempt++;
                case INVALID -> invalid++;
            }
        }
        double disclosedRatePct = total == 0 ? 0.0 : qualified * 100.0 / total;
        double shortfallPct = total == 0 ? 0.0 : penalized * 100.0 / total;
        double penalty = shortfallPct * (ratedCapacityKw / KW_PER_WAN_KW) * SCORE_PER_POINT_PER_WAN_KW;
        return new TierRateResult(total, qualified, penalized, exempt, invalid,
                disclosedRatePct, shortfallPct, penalty);
    }
}
