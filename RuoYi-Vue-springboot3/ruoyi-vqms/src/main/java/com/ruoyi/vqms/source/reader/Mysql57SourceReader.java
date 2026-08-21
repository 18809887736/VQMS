package com.ruoyi.vqms.source.reader;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.DataSource;
import com.ruoyi.common.enums.DataSourceType;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.model.YcHistory;
import com.ruoyi.vqms.source.mapper.HisCurveSvMapper;
import com.ruoyi.vqms.source.mapper.WarnInfoMapper;
import com.ruoyi.vqms.source.mapper.YcHistoryMapper;

/**
 * MySQL 5.7 外部数据源读取实现（SourceReader）。
 *
 * <p>经 RuoYi 多数据源 {@code SLAVE} 访问外部源（只读，v5.0 §5）。</p>
 */
@Component
public class Mysql57SourceReader implements SourceReader
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Mysql57SourceReader.class);

    @Autowired
    private HisCurveSvMapper hisCurveSvMapper;

    @Autowired
    private YcHistoryMapper ycHistoryMapper;

    @Autowired
    private WarnInfoMapper warnInfoMapper;

    @Override
    @DataSource(DataSourceType.SLAVE)
    public List<HisCurveSv> readCurve(String startTime, String endTime, Long busbarNum)
    {
        List<HisCurveSv> rows = hisCurveSvMapper.selectByRangeAndBusbar(startTime, endTime, busbarNum);
        int before = rows.size();
        rows.removeIf(r -> !SaveTimeFilter.isValid(r.getSaveTime()));
        logBeforeAfter("his_curve_sv", before, rows.size());
        return rows;
    }

    @Override
    @DataSource(DataSourceType.SLAVE)
    public List<YcHistory> readYc(String startTime, String endTime, Long ycNum)
    {
        List<YcHistory> rows = ycHistoryMapper.selectByRangeAndYc(startTime, endTime, ycNum);
        int before = rows.size();
        rows.removeIf(r -> !SaveTimeFilter.isValid(r.getYcTime()));
        logBeforeAfter("yc_history", before, rows.size());
        return rows;
    }

    @Override
    @DataSource(DataSourceType.SLAVE)
    public List<WarnInfo> readWarn(String startTime, String endTime, Long warnType)
    {
        List<WarnInfo> rows = warnInfoMapper.selectByRangeAndType(startTime, endTime, warnType);
        int before = rows.size();
        rows.removeIf(r -> !SaveTimeFilter.isValid(r.getWarnTime()));
        logBeforeAfter("warn_info", before, rows.size());
        return rows;
    }

    private void logBeforeAfter(String table, int before, int after)
    {
        if (after < before)
        {
            log.warn("{} 坏行跳过: {}/{} 行", table, before - after, before);
        }
    }
}
