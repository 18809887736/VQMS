package com.ruoyi.vqms.statistics;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V_target 解码（正式 v1_0 §2.1；v5.0 §8.8.2 S1 组件）。
 *
 * <p>{@code DefaultRegulationJudge} 内部组件（同包、非对调用方接口）——拍板约束
 * 「调用方不预算 V_target」不变：调用方仍只递 {@link AvcCommand} 原文。</p>
 *
 * <p>两种形态（按文本关键词「目标值」分派，对齐 avc-data-gen decode.py 口径）：</p>
 * <ul>
 *   <li>目标值形态：尾码数值 ÷ 100 → kV（22315 → 223.15）；</li>
 *   <li>增量值形态：4 位编码 = 方向(1) · 循环码(1) · 幅值(2)，幅值 × 100V × 方向
 *       ± t₀ 实时电压（2202 = +200V；234.25 + 0.2 → 234.45）。第 2 位循环码
 *       ∈ {0..5} 仅校验不参与计算（Leo 2026-08-19 拍板）。</li>
 * </ul>
 *
 * <p>分类优先级 = 脏写 → 循环码 → 方向 → t₀（'9X02' 类双向坏按循环码非法归因，
 * 对齐策略文档 §4.1 例 #4）。{@link #classify} 与 decode.py 差分基线一致；
 * {@link #decode} 结果保留 2 位小数（增量幅值粒度 0.1kV × t₀ ≤ 2 位小数，
 * 数学上不存在 .xx5 中点，double 半升取整与 Python round 无分歧路径）。</p>
 */
public final class VTargetDecoder
{
    /** 尾码提取（宽松）：抓 warn_info 文本末尾 ",<字母数字>." ——先留全量做逐槽位校验 */
    private static final Pattern TRAILING_CODE = Pattern.compile(",([A-Za-z0-9]+)\\.\\s*$");

    private static final String TARGET_FORM_KEYWORD = "目标值";

    /**
     * 非电压指令关键词（对端实时库 BUSBAR_PLANTYPE id=2 的字典名，2026-08-26 考据）。
     * ⚠ UNVERIFIED-ASSUMPTION：真实无功指令 warn_info 文本从未见过（合成库也无），
     * 关键词按字典名推断，待真实数据回放核对覆盖度。
     */
    private static final String NON_VOLTAGE_KEYWORD = "无功增量";

    private VTargetDecoder()
    {
    }

    /**
     * 非电压指令识别——文本含「无功增量」即排除出调节合格率分母（Leo 2026-08-26 拍板：
     * 形态真值以指令文本为准，BUSBAR_PLANTYPE 仅考据存档不做表路由；无功指令无电压包络
     * 口径，误入管线会被当增量形态解出无意义 V_target）。管线在脏时间过滤后、judge 前调用。
     */
    public static boolean isNonVoltage(String warnContent)
    {
        return warnContent != null && warnContent.contains(NON_VOLTAGE_KEYWORD);
    }

    /**
     * 解码失败分类（只分类、不算值）。返回 empty = 结构上可解。
     * 三类归因见 {@link DecodeFailureReason}。
     */
    public static Optional<DecodeFailureReason> classify(AvcCommand cmd)
    {
        String text = cmd.warnContent();
        if (text == null || text.isBlank())
        {
            return Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }
        Matcher m = TRAILING_CODE.matcher(text.trim());
        if (!m.find())
        {
            return Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }
        String code = m.group(1);

        if (text.contains(TARGET_FORM_KEYWORD))
        {
            // 目标值形态：只要尾码是纯数字就结构可解（数值 ÷100 在 decode）
            return code.matches("\\d+") ? Optional.empty() : Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }

        // 增量形态：方向(1位) · 循环码(1位) · 幅值(2位)
        if (code.length() != 4)
        {
            return Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }
        if (!Character.isDigit(code.charAt(0)) || !Character.isDigit(code.charAt(2))
                || !Character.isDigit(code.charAt(3)))
        {
            return Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }
        char cycle = code.charAt(1);
        // UNVERIFIED-ASSUMPTION 基础：{0..5} 值域为 Leo 2026-08-19 拍板，轮转规律待生产数据；
        // 分类规则本身按拍板实现，自造越界用例在测试处显式标注
        if (!Character.isDigit(cycle) || cycle > '5')
        {
            return Optional.of(DecodeFailureReason.CYCLE_CODE_INVALID);
        }
        char direction = code.charAt(0);
        if (direction != '1' && direction != '2')
        {
            return Optional.of(DecodeFailureReason.CORRUPTED_ENCODING);
        }
        if (cmd.t0RealtimeVoltageKv() == null)
        {
            return Optional.of(DecodeFailureReason.MISSING_T0_VOLTAGE);
        }
        return Optional.empty();
    }

    /**
     * 计算 V_target（kV）。前置条件：{@link #classify} 为 empty，否则行为未定义。
     */
    public static double decode(AvcCommand cmd)
    {
        String text = cmd.warnContent().trim();
        Matcher m = TRAILING_CODE.matcher(text);
        String code = m.find() ? m.group(1) : null;
        if (code == null)
        {
            throw new IllegalStateException("classify 未先行调用或文本无尾码: " + cmd.warnContent());
        }
        if (text.contains(TARGET_FORM_KEYWORD))
        {
            return Long.parseLong(code) / 100.0;
        }
        double deltaKv = Integer.parseInt(code.substring(2)) * 0.1;
        double raw = code.charAt(0) == '2'
                ? cmd.t0RealtimeVoltageKv() + deltaKv
                : cmd.t0RealtimeVoltageKv() - deltaKv;
        // 2 位小数收口：消除 double 加法尾差（234.8 + 0.2 → 235.00000000000003 会误判整数边界档）
        return Math.round(raw * 100.0) / 100.0;
    }
}
