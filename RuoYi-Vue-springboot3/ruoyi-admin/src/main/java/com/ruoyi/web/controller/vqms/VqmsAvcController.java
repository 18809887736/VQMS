package com.ruoyi.web.controller.vqms;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.management.service.VqmsAvcStatsService;

/**
 * AVC 投运率 / 调节合格率查询（v5.0 §10.1）——厂级口径，无母线维度。
 *
 * <p>S4 读侧（2026-08-24）：两列表端点接 rollup 表真数据（月度粒度，前端页契约字段），
 * 率/罚款经 {@code RegulationStatistics.summarizeCounts} 纯函数换算；导出仍占位
 * （POI 导出随报表交付批）。daily/monthly/yearly 三端点维持占位——旧分钟级口径已废，
 * 页面语义待真实数据回放后拍板。</p>
 */
@RestController
@RequestMapping("/vqms/avc")
public class VqmsAvcController extends BaseController
{
    @Autowired
    private VqmsAvcStatsService avcStatsService;

    /** 投运率月度行（statMonth 可选，yyyy-MM） */
    @PreAuthorize("@ss.hasPermi('vqms:avc:runtime:list')")
    @GetMapping("/runtime/list")
    public TableDataInfo runtimeList(@RequestParam(required = false) String statMonth)
    {
        return rows(avcStatsService.runtimeMonthly(statMonth));
    }

    /** 调节合格率月度行（两档平行；statMonth 可选，yyyy-MM） */
    @PreAuthorize("@ss.hasPermi('vqms:avc:regulation:list')")
    @GetMapping("/regulation/list")
    public TableDataInfo regulationList(@RequestParam(required = false) String statMonth)
    {
        return rows(avcStatsService.regulationMonthly(statMonth));
    }

    @PreAuthorize("@ss.hasPermi('vqms:avc:runtime:export')")
    @PostMapping("/runtime/export")
    public void runtimeExport()
    {
        shelved("AVC 投运率导出");
    }

    @PreAuthorize("@ss.hasPermi('vqms:avc:regulation:export')")
    @PostMapping("/regulation/export")
    public void regulationExport()
    {
        shelved("AVC 调节合格率导出");
    }

    private static TableDataInfo rows(java.util.List<java.util.Map<String, Object>> list)
    {
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(list);
        data.setTotal(list.size());
        return data;
    }

    private static void shelved(String what)
    {
        throw new ServiceException(what + "属搁置轨，暂不可导出");
    }
}
