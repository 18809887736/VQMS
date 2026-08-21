package com.ruoyi.web.controller.vqms;

import java.util.ArrayList;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;

/**
 * 日/月/年合格率查询（v5.0 §10.1）。
 *
 * <p>空壳（D5）：统计落库属搁置轨 S2（调节合格率）/S3（投运率）+ 电压合格率同批，
 * 表未建、算法定稿前无数据可查——端点与 perms 先行占位，解封后接真实现。
 * 导出按钮同理占位：显式报「搁置轨」而非 404。</p>
 */
@RestController
@RequestMapping("/vqms/stats")
public class VqmsStatsController extends BaseController
{
    @PreAuthorize("@ss.hasPermi('vqms:daily:list')")
    @GetMapping("/daily/list")
    public TableDataInfo dailyList()
    {
        return empty("日合格率统计属搁置轨（S2/S3 同批解封），当前无数据");
    }

    @PreAuthorize("@ss.hasPermi('vqms:monthly:list')")
    @GetMapping("/monthly/list")
    public TableDataInfo monthlyList()
    {
        return empty("月合格率统计属搁置轨（S2/S3 同批解封），当前无数据");
    }

    @PreAuthorize("@ss.hasPermi('vqms:yearly:list')")
    @GetMapping("/yearly/list")
    public TableDataInfo yearlyList()
    {
        return empty("年合格率统计属搁置轨（S2/S3 同批解封），当前无数据");
    }

    @PreAuthorize("@ss.hasPermi('vqms:daily:export')")
    @PostMapping("/daily/export")
    public void dailyExport()
    {
        shelved("日合格率导出");
    }

    @PreAuthorize("@ss.hasPermi('vqms:monthly:export')")
    @PostMapping("/monthly/export")
    public void monthlyExport()
    {
        shelved("月合格率导出");
    }

    @PreAuthorize("@ss.hasPermi('vqms:yearly:export')")
    @PostMapping("/yearly/export")
    public void yearlyExport()
    {
        shelved("年合格率导出");
    }

    private static TableDataInfo empty(String msg)
    {
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setMsg(msg);
        data.setRows(new ArrayList<>());
        data.setTotal(0);
        return data;
    }

    private static void shelved(String what)
    {
        throw new ServiceException(what + "属搁置轨（S2/S3 同批解封），暂不可导出");
    }
}
