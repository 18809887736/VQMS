package com.ruoyi.vqms.source.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.vqms.source.model.YcHistory;

/**
 * {@link YxSignalReader} 的 yc_history 实现（v5.0 §8.8.2，方言切换同 D1 模式）。
 *
 * <p>经 {@link SourceReader#readYc}（D4 三步闸门 + 多数据源 SLAVE 路由）取回原始行，
 * 阶跃保持选取为包内纯函数 {@link #selectHeld}（可独测）。</p>
 */
@Component
public class YxSignalReaderImpl implements YxSignalReader
{
    /** ponytail: 回看窗 1 天——阶跃保持量变化稀疏；若现场信号长静默跨日漏读，改常量或改 SQL LIMIT 1 倒查 */
    private static final long LOOKBACK_DAYS = 1;

    private static final DateTimeFormatter MINUTE_TEXT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SourceReader sourceReader;

    @Override
    public Optional<Integer> heldValue(Long pointNum, LocalDateTime atMinute)
    {
        return pickLatest(read(pointNum, atMinute), atMinute)
                .map(r -> r.getYcData() == null ? null : r.getYcData().intValue());
    }

    @Override
    public Optional<Double> heldDecimalValue(Long pointNum, LocalDateTime atMinute)
    {
        return pickLatest(read(pointNum, atMinute), atMinute)
                .map(YcHistory::getYcData);
    }

    private List<YcHistory> read(Long pointNum, LocalDateTime atMinute)
    {
        return sourceReader.readYc(
                MINUTE_TEXT.format(atMinute.minusDays(LOOKBACK_DAYS)),
                MINUTE_TEXT.format(atMinute), pointNum);
    }

    /** 阶跃保持行选取：≤ atMinute 的最近一条（同分钟多行取后见行）；无合格行 → empty。 */
    static Optional<YcHistory> pickLatest(List<YcHistory> rows, LocalDateTime atMinute)
    {
        YcHistory held = null;
        LocalDateTime heldAt = null;
        for (YcHistory row : rows)
        {
            LocalDateTime t = MinuteRounder.parseAndRound(row.getYcTime());
            if (t == null || t.isAfter(atMinute))
            {
                continue; // 坏时间原文跳过（闸门已滤大半）；未来行不取
            }
            if (heldAt == null || !t.isBefore(heldAt))
            {
                heldAt = t;
                held = row;
            }
        }
        return Optional.ofNullable(held).filter(r -> r.getYcData() != null);
    }

    /** 兼容既有测试口径的整值选取。 */
    static Optional<Integer> selectHeld(List<YcHistory> rows, LocalDateTime atMinute)
    {
        return pickLatest(rows, atMinute).map(r -> Integer.valueOf(r.getYcData().intValue()));
    }
}
