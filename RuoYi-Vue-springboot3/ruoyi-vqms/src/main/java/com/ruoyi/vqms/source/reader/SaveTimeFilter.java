package com.ruoyi.vqms.source.reader;

import java.util.regex.Pattern;

/**
 * save_time / yc_time / warn_time 的闸门格式校验（D4 ②步）。
 *
 * <p>canonical 契约：空格分隔、无首尾空白、小数 1~9 位——与 ① 步 SQL 字符串粗筛的
 * 字典序假设严格一致（T 分隔/空白衬垫的行在 SQL 层已被结构性排除，此处见到同样按
 * 坏行跳过+记日志，保证「② 认合法 = ① 已送达 = ③ 可见」三步一致）。
 * MinuteRounder 作为独立解析工具仍宽容 T 分隔与空白（D3 契约），与本闸门口径不同属有意为之。</p>
 */
final class SaveTimeFilter
{
    private static final Pattern PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?");

    private SaveTimeFilter()
    {
    }

    static boolean isValid(String time)
    {
        return time != null && PATTERN.matcher(time).matches();
    }
}
