package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D4 格式校验三步闸门（v5.0 §12.1 D4 内联）——反制 {@code save_time} 是 varchar(255)
 * 带毫秒、无索引的缺陷：
 *
 * <ol>
 * <li>① 放宽边界——SQL 层用目标区间前后各扩 1 分钟的字符串范围粗筛（防边界分钟被字符串比较漏掉）；</li>
 * <li>② 正则校验——逐行校验格式，不匹配 = 坏行，跳过 + 记日志，不拖垮整批；</li>
 * <li>③ 解析后精确过滤——合法行解析取整到分钟（D3 §13），Java 侧按目标区间精确过滤，
 *     粗筛多读的边界行在此剔除。</li>
 * </ol>
 *
 * <p>取整仅用于边界裁剪；本闸门返回的行保留原始时间戳原文（judge 侧窗口对齐由调用方经 D3 做）。
 * ②/③ 对 {@code his_curve_sv}/{@code yc_history}/{@code warn_info} 三表共用。</p>
 */
public final class SaveTimeGate
{
    private static final Logger log = LoggerFactory.getLogger(SaveTimeGate.class);

    private static final DateTimeFormatter MINUTE_TEXT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    public record ExpandedBounds(String start, String end)
    {
    }

    private SaveTimeGate()
    {
    }

    /**
     * ① 放宽边界：目标区间前后各扩 1 分钟，格式化为秒精度字符串供 SQL 粗筛。
     * 边界非法或区间倒置（caller bug）抛 {@link IllegalArgumentException} 快速失败。
     */
    public static ExpandedBounds expandBounds(String start, String end)
    {
        LocalDateTime[] window = requireWindow(start, end);
        return new ExpandedBounds(window[0].minusMinutes(1).format(MINUTE_TEXT),
                window[1].plusMinutes(1).format(MINUTE_TEXT));
    }

    /**
     * ②+③ 逐行：正则校验（坏行跳过+记日志）→ 解析取整 → 按目标区间（闭区间）精确过滤。
     * ② canonical 格式 = 空格分隔无空白衬垫（与 ① SQL 字典序假设一致）；正则通过但解析失败
     * （如不可能日期 2027-02-29）同计坏行。返回行为原实例，原始时间戳不动。
     */
    public static <T> List<T> filter(List<T> rows, Function<T, String> timeOf, String start, String end)
    {
        LocalDateTime[] window = requireWindow(start, end);
        LocalDateTime boundStart = window[0];
        LocalDateTime boundEnd = window[1];
        List<T> kept = new ArrayList<>(rows.size());
        int bad = 0;
        int outOfWindow = 0;
        for (T row : rows)
        {
            String raw = timeOf.apply(row);
            if (!SaveTimeFilter.isValid(raw))
            {
                bad++;
                logBad(raw);
                continue;
            }
            LocalDateTime parsed = MinuteRounder.parse(raw);
            if (parsed == null)
            {
                bad++;
                logBad(raw);
                continue;
            }
            LocalDateTime rounded = MinuteRounder.round(parsed);
            if (!rounded.isBefore(boundStart) && !rounded.isAfter(boundEnd))
            {
                kept.add(row);
            }
            else
            {
                outOfWindow++;
            }
        }
        if (outOfWindow > 0)
        {
            log.debug("目标区间外剔除 {} 行（粗筛多读的边界行，属预期）", outOfWindow);
        }
        if (bad > 0)
        {
            log.warn("坏行共 {} 行已跳过", bad);
        }
        return kept;
    }

    private static void logBad(String raw)
    {
        log.warn("坏 save_time 跳过: {}", raw);
    }

    private static LocalDateTime requireBound(String raw)
    {
        LocalDateTime parsed = MinuteRounder.parse(raw);
        if (parsed == null)
        {
            throw new IllegalArgumentException("非法查询边界: " + raw);
        }
        return parsed;
    }

    /** 边界各自可解析 + 区间不倒置——倒置会让 SQL/精滤双双静默空结果，必须快速失败 */
    private static LocalDateTime[] requireWindow(String start, String end)
    {
        LocalDateTime boundStart = requireBound(start);
        LocalDateTime boundEnd = requireBound(end);
        if (boundEnd.isBefore(boundStart))
        {
            throw new IllegalArgumentException("查询区间倒置: start=" + start + ", end=" + end);
        }
        return new LocalDateTime[] { boundStart, boundEnd };
    }
}
