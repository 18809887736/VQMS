package com.ruoyi.web.controller.vqms;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.vqms.source.model.HisCurveSv;
import com.ruoyi.vqms.source.reader.MinuteRounder;
import com.ruoyi.vqms.source.reader.SourceReader;

/**
 * 电压曲线查询（v5.0 §10.1）：母线 + 时间范围，逐分钟 high/low，数据经 D1 reader + D4 闸门取自外部源。
 *
 * <p>限量（§10.2，反制 his_curve_sv 无索引全表扫）：时间范围上限 31 天、pageSize 上限 500。</p>
 */
@RestController
@RequestMapping("/vqms/curve")
public class VqmsCurveController extends BaseController
{
    private static final int MAX_RANGE_DAYS = 31;

    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private SourceReader sourceReader;

    @PreAuthorize("@ss.hasPermi('vqms:curve:list')")
    @GetMapping("/list")
    public TableDataInfo list(String startTime, String endTime, Long busbarNum,
            Integer pageNum, Integer pageSize)
    {
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime))
        {
            throw new ServiceException("startTime / endTime 必填");
        }
        LocalDateTime start = MinuteRounder.parse(startTime);
        LocalDateTime end = MinuteRounder.parse(endTime);
        if (start == null || end == null)
        {
            throw new ServiceException("startTime / endTime 格式非法（yyyy-MM-dd HH:mm:ss）");
        }
        if (end.isBefore(start))
        {
            throw new ServiceException("查询区间倒置");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end))
        {
            throw new ServiceException("曲线查询时间范围上限 " + MAX_RANGE_DAYS + " 天");
        }
        List<HisCurveSv> rows = sourceReader.readCurve(startTime, endTime, busbarNum);
        return pageOf(rows, pageNum, pageSize);
    }

    private TableDataInfo pageOf(List<HisCurveSv> rows, Integer pageNum, Integer pageSize)
    {
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int from = (pageNum == null || pageNum < 1 ? 1 : pageNum) - 1;
        int total = rows.size();
        int to = Math.min(from + size, total);
        List<HisCurveSv> page = from >= total ? List.of() : rows.subList(from, to);
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(page);
        data.setTotal(total);
        return data;
    }
}
