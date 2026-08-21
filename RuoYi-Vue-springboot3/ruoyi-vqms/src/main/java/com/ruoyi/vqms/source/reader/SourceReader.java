package com.ruoyi.vqms.source.reader;

import java.util.List;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.model.YcHistory;

/**
 * 外部数据源只读读取（D1）。
 *
 * <p>三张表各自有独立方法：his_curve_sv（按母线+时间）、yc_history（按点号+时间）、
 * warn_info（按类型+时间，电压指令 warn_type=5）。每个方法对空时间不做。</p>
 */
public interface SourceReader
{
    List<HisCurveSv> readCurve(String startTime, String endTime, Long busbarNum);

    List<YcHistory> readYc(String startTime, String endTime, Long ycNum);

    List<WarnInfo> readWarn(String startTime, String endTime, Long warnType);
}
