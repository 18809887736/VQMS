package com.ruoyi.vqms.source.reader;

import java.util.regex.Pattern;

/**
 * save_time / yc_time / warn_time 的轻量格式校验。
 *
 * <p>三步完整闸门属 D4；此处只做 D1 需要的「坏行跳过 + 记日志」。</p>
 */
final class SaveTimeFilter
{
    private static final Pattern PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");

    private SaveTimeFilter()
    {
    }

    static boolean isValid(String time)
    {
        return time != null && PATTERN.matcher(time.trim()).matches();
    }
}
