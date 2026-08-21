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
 * <p>经 RuoYi 多数据源 {@code SLAVE} 访问外部源（只读，v5.0 §5）。
 * D4 三步闸门：① {@link SaveTimeGate#expandBounds} 放宽边界给 SQL 粗筛，
 * ②+③ {@link SaveTimeGate#filter} 正则校验 + 取整后按目标区间精确过滤。</p>
 */
@Component
public class Mysql57SourceReader implements SourceReader
{
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
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(startTime, endTime);
        List<HisCurveSv> rows = hisCurveSvMapper.selectByRangeAndBusbar(bounds.start(), bounds.end(), busbarNum);
        return SaveTimeGate.filter(rows, HisCurveSv::getSaveTime, startTime, endTime);
    }

    @Override
    @DataSource(DataSourceType.SLAVE)
    public List<YcHistory> readYc(String startTime, String endTime, Long ycNum)
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(startTime, endTime);
        List<YcHistory> rows = ycHistoryMapper.selectByRangeAndYc(bounds.start(), bounds.end(), ycNum);
        return SaveTimeGate.filter(rows, YcHistory::getYcTime, startTime, endTime);
    }

    @Override
    @DataSource(DataSourceType.SLAVE)
    public List<WarnInfo> readWarn(String startTime, String endTime, Long warnType)
    {
        SaveTimeGate.ExpandedBounds bounds = SaveTimeGate.expandBounds(startTime, endTime);
        List<WarnInfo> rows = warnInfoMapper.selectByRangeAndType(bounds.start(), bounds.end(), warnType);
        return SaveTimeGate.filter(rows, WarnInfo::getWarnTime, startTime, endTime);
    }
}
