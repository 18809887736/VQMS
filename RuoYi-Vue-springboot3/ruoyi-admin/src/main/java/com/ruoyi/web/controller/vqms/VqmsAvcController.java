package com.ruoyi.web.controller.vqms;

import java.util.ArrayList;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;

/**
 * AVC 投运率 / 调节合格率查询（v5.0 §10.1）——厂级口径，无母线维度。
 *
 * <p>空壳（D5）：投运率落库属搁置轨 S3、调节合格率属 S2，判定实现为接口+stub——
 * 端点与 perms 先行占位，解封后接真实现；导出按钮占位显式报「搁置轨」。</p>
 */
@RestController
@RequestMapping("/vqms/avc")
public class VqmsAvcController extends BaseController
{
    @PreAuthorize("@ss.hasPermi('vqms:avc:runtime:list')")
    @GetMapping("/runtime/list")
    public TableDataInfo runtimeList()
    {
        return empty("AVC 投运率统计属搁置轨（S3 同批解封），当前无数据");
    }

    @PreAuthorize("@ss.hasPermi('vqms:avc:regulation:list')")
    @GetMapping("/regulation/list")
    public TableDataInfo regulationList()
    {
        return empty("AVC 调节合格率统计属搁置轨（S2 同批解封），当前无数据");
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
        throw new ServiceException(what + "属搁置轨，暂不可导出");
    }
}
