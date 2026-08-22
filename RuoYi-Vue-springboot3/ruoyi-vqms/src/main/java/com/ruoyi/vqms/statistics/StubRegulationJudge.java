package com.ruoyi.vqms.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 确定性占位判定实现（搁置轨 stub，v5.0 §8.2 / 正式 v1_0 §2.5）。
 *
 * <p><b>契约级部分为真</b>（输出契约的稳定面，D9 完成标准「Undecodable 按原因分类、
 * completeness 如实上报」要求）：</p>
 * <ul>
 *   <li>解码<b>分类</b>：目标值/增量形态识别 + 三类失败归因——只判"能不能解"，
 *       <b>不算 V_target 数值</b>（数值解码属算法核心，随 S1 定稿）；</li>
 *   <li>窗口结构统计：completeness（[1..t_econ] 有数据分钟占比，如实上报不处置）、
 *       invalidTiers（该档整档全缺或 L>H → 该档不可判，正式版 §2.5 两种成因）；</li>
 *   <li>分类优先级：结构脏写（尾码缺失/长度不对/数字槽位坏）→ 循环码非法（第 2 位 ∉
 *       {0..5}，Leo 2026-08-19 值域拍板；轮转规律待实证不影响本分类）→ 缺 t₀ 电压。</li>
 * </ul>
 *
 * <p><b>算法核心部分为占位</b>：有效档的 VERDICT 一律 {@link Verdict#QUALIFIED}
 * （确定性、禁随机、<b>不保证正确</b>）。真实包络判定（V_target 数值解码 + 区间比较）
 * 由 S1 解封后的实现替换——调用方零改动。{@link #isStub()} 恒 true。</p>
 */
public class StubRegulationJudge implements RegulationJudge
{
    /** 尾码提取（宽松）：抓 warn_info 文本末尾 ",<字母数字>." ——先留全量做逐槽位校验 */
    private static final Pattern TRAILING_CODE = Pattern.compile(",([A-Za-z0-9]+)\\.\\s*$");

    private static final String TARGET_FORM_KEYWORD = "目标值";

    @Override
    public boolean isStub()
    {
        return true;
    }

    @Override
    public RegulationOutcome judge(AvcCommand cmd, List<MinuteCurve> curves, JudgeParams params)
    {
        DecodeFailureReason reason = classify(cmd);
        if (reason != null)
        {
            return new RegulationOutcome.Undecodable(reason);
        }

        // 窗口结构统计（契约级，为真）：completeness + 按档无效
        Set<Integer> presentOffsets = new HashSet<>();
        for (MinuteCurve c : curves)
        {
            if (c.minuteOffset() >= 1 && c.minuteOffset() <= params.tEcon())
            {
                presentOffsets.add(c.minuteOffset());
            }
        }
        double completeness = (double) presentOffsets.size() / params.tEcon();

        Set<Tier> invalidTiers = new HashSet<>();
        for (Tier tier : Tier.values())
        {
            int lo = tier == Tier.FAST ? 1 : params.tFast() + 1;
            int hi = tier == Tier.FAST ? params.tFast() : params.tEcon();
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            boolean any = false;
            for (MinuteCurve c : curves)
            {
                if (c.minuteOffset() >= lo && c.minuteOffset() <= hi)
                {
                    any = true;
                    low = Math.min(low, c.lowSv());
                    high = Math.max(high, c.highSv());
                }
            }
            if (!any || low > high)
            {
                // 整档全缺（S14）/ L>H 数据异常（S16）：该档不可判，VERDICT 必空（构造期不变式）
                invalidTiers.add(tier);
            }
        }

        // 判定结论占位：有效档一律 QUALIFIED（stub=true，禁随机、不保证正确；S1 替换点）
        return new RegulationOutcome.Judged(
                verdictFor(Tier.FAST, invalidTiers),
                verdictFor(Tier.ECON, invalidTiers),
                completeness, invalidTiers);
    }

    private Optional<Verdict> verdictFor(Tier tier, Set<Tier> invalidTiers)
    {
        return invalidTiers.contains(tier) ? Optional.empty() : Optional.of(Verdict.QUALIFIED);
    }

    /**
     * 契约级解码分类（只分类、不算值）。返回 null = 结构上可解。
     *
     * <p>形态识别按文本关键词「目标值」分派（对齐 avc-data-gen decode.py 口径）：
     * 目标值形态只看尾码是否纯数字；增量形态逐槽位校验——长度 4、第 1/3/4 位数字、
     * 第 2 位循环码 ∈ {0..5}、方向 ∈ {1,2}、t₀ 实时电压在场。
     * 校验顺序 = 脏写 → 循环码 → 方向 → t₀（'9X02' 类双向坏按循环码非法归因，
     * 对齐策略文档 §4.1 例 #4）。</p>
     */
    private DecodeFailureReason classify(AvcCommand cmd)
    {
        String text = cmd.warnContent();
        if (text == null || text.isBlank())
        {
            return DecodeFailureReason.CORRUPTED_ENCODING;
        }
        Matcher m = TRAILING_CODE.matcher(text.trim());
        if (!m.find())
        {
            return DecodeFailureReason.CORRUPTED_ENCODING;
        }
        String code = m.group(1);

        if (text.contains(TARGET_FORM_KEYWORD))
        {
            // 目标值形态：数值 ÷100 即 kV——只要尾码是纯数字就结构可解（数值计算随 S1）
            return code.matches("\\d+") ? null : DecodeFailureReason.CORRUPTED_ENCODING;
        }

        // 增量形态：方向(1位) · 循环码(1位) · 幅值(2位)
        if (code.length() != 4)
        {
            return DecodeFailureReason.CORRUPTED_ENCODING;
        }
        if (!Character.isDigit(code.charAt(0)) || !Character.isDigit(code.charAt(2))
                || !Character.isDigit(code.charAt(3)))
        {
            // 第 1/3/4 位非数字（如 ABCD）= 编码脏写
            return DecodeFailureReason.CORRUPTED_ENCODING;
        }
        char cycle = code.charAt(1);
        // UNVERIFIED-ASSUMPTION 基础：{0..5} 值域为 Leo 2026-08-19 拍板，轮转规律待生产数据；
        // 分类规则本身按拍板实现，自造越界用例在测试处显式标注
        if (!Character.isDigit(cycle) || cycle > '5')
        {
            return DecodeFailureReason.CYCLE_CODE_INVALID;
        }
        char direction = code.charAt(0);
        if (direction != '1' && direction != '2')
        {
            // 方向非法 = 编码脏写（合法仅 1=减 / 2=加）
            return DecodeFailureReason.CORRUPTED_ENCODING;
        }
        if (cmd.t0RealtimeVoltageKv() == null)
        {
            return DecodeFailureReason.MISSING_T0_VOLTAGE;
        }
        return null;
    }
}
