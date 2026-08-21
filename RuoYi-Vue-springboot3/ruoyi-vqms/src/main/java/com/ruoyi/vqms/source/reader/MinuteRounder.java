package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 * D3 时间对齐工具：就近取整到分钟（秒≥30 进位，&lt;30 舍去）。
 *
 * <p>v5.0 §13 时间原则 + §5 时间契约：`his_curve_sv`/`yc_history` 的毫秒精度原始
 * 时间戳在任何逐分钟聚合前对齐用；reader 返回原始时间戳，窗口对齐由调用方经本工具完成。
 * 取整幂等（已整到分钟的值不再变化），双重取整无害。禁止 floor/截断/按原始秒分组。</p>
 *
 * <p>时间无时区，按北京时间墙钟理解（LocalDateTime，不做 UTC 中转）。</p>
 */
public final class MinuteRounder
{
    /** save_time/yc_time/warn_time 原文格式：yyyy-MM-dd[空格或T]HH:mm:ss[.1~9位毫秒/纳秒] */
    private static final DateTimeFormatter PARSER = buildParser();

    private MinuteRounder()
    {
    }

    /**
     * 解析时间原文为 LocalDateTime；格式非法或入参为空返回 null（坏行由调用方跳过+记日志）。
     * 与 SaveTimeFilter 正则同口径：小数 1~9 位、尾随裸小数点拒绝、不可能日期/24:00 拒绝（STRICT，不钳位改值）。
     */
    public static LocalDateTime parse(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.endsWith("."))
        {
            return null;
        }
        try
        {
            return LocalDateTime.parse(trimmed, PARSER);
        }
        catch (DateTimeParseException e)
        {
            return null;
        }
    }

    /**
     * 就近取整到分钟：秒≥30 进位（跨分/时/日/月/年自然进位），&lt;30 舍去；毫秒不参与判定。
     * 入参须非空——null/非法原文由 {@link #parse(String)} 拦截，组合路径用 {@link #parseAndRound(String)}。
     */
    public static LocalDateTime round(LocalDateTime time)
    {
        LocalDateTime truncated = time.truncatedTo(ChronoUnit.MINUTES);
        return time.getSecond() >= 30 ? truncated.plusMinutes(1) : truncated;
    }

    /**
     * 解析 + 取整一步完成；格式非法返回 null。
     */
    public static LocalDateTime parseAndRound(String raw)
    {
        LocalDateTime time = parse(raw);
        return time == null ? null : round(time);
    }

    private static DateTimeFormatter buildParser()
    {
        DateTimeFormatter timePart = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .toFormatter();
        return new DateTimeFormatterBuilder()
                // uuuu 而非 yyyy：STRICT 下 year-of-era 缺 era 会全拒，pro-year 直接可用
                .appendPattern("uuuu-MM-dd")
                .appendOptional(new DateTimeFormatterBuilder().appendLiteral(' ').append(timePart).toFormatter())
                .appendOptional(new DateTimeFormatterBuilder().appendLiteral('T').append(timePart).toFormatter())
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter()
                // STRICT：SMART 会把 2027-02-29 钳位成 02-28、24:00:00 滚到次日——脏值静默改写，必须拒
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
